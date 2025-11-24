package ru.yandex.practicum.kafka.telemetry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.domain.ActionType;
import ru.yandex.practicum.kafka.telemetry.domain.ConditionOperation;
import ru.yandex.practicum.kafka.telemetry.domain.ConditionType;
import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Condition;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioActionBinding;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioConditionBinding;
import ru.yandex.practicum.kafka.telemetry.entity.Sensor;
import ru.yandex.practicum.kafka.telemetry.entity.id.ScenarioActionKey;
import ru.yandex.practicum.kafka.telemetry.entity.id.ScenarioConditionKey;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.repository.ActionRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ConditionRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioActionBindingRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioConditionBindingRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.repository.SensorRepository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionBindingRepository conditionBindingRepository;
    private final ScenarioActionBindingRepository actionBindingRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            log.info("Registering device {} for hub {}", deviceAdded.getId(), event.getHubId());
            registerSensor(event.getHubId(), deviceAdded);
            return;
        }
        if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            log.info("Removing device {} for hub {}", deviceRemoved.getId(), event.getHubId());
            removeSensor(event.getHubId(), deviceRemoved);
            return;
        }
        if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            log.info("Upserting scenario {} for hub {}", scenarioAdded.getName(), event.getHubId());
            upsertScenario(event.getHubId(), scenarioAdded);
            return;
        }
        if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            log.info("Removing scenario {} for hub {}", scenarioRemoved.getName(), event.getHubId());
            removeScenario(event.getHubId(), scenarioRemoved);
            return;
        }
        log.warn("Unsupported hub event payload: {}", payload);
    }

    private void registerSensor(String hubId, DeviceAddedEventAvro payload) {
        String sensorId = payload.getId().toString();
        sensorRepository.findById(sensorId)
                .ifPresentOrElse(sensor -> {
                    if (!Objects.equals(sensor.getHubId(), hubId)) {
                        sensor.setHubId(hubId);
                        sensorRepository.save(sensor);
                    }
                }, () -> sensorRepository.save(new Sensor(sensorId, hubId)));
        log.debug("Registered sensor {} for hub {}", sensorId, hubId);
    }

    private void removeSensor(String hubId, DeviceRemovedEventAvro payload) {
        String sensorId = payload.getId().toString();
        sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresentOrElse(sensor -> {
            removeBindingsForSensor(sensorId);
            sensorRepository.delete(sensor);
            log.debug("Removed sensor {} for hub {}", sensorId, hubId);
        }, () -> log.info("Skip removing unknown sensor {} for hub {}", sensorId, hubId));
    }

    private void upsertScenario(String hubId, ScenarioAddedEventAvro payload) {
        String scenarioName = payload.getName().toString();
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .orElseGet(() -> {
                    Scenario created = new Scenario();
                    created.setHubId(hubId);
                    created.setName(scenarioName);
                    return scenarioRepository.save(created);
                });

        cleanupScenarioBindings(scenario.getId());

        List<ScenarioConditionAvro> conditions = payload.getConditions();
        List<DeviceActionAvro> actions = payload.getActions();

        ensureSensorsExist(hubId, conditions.stream()
                .map(c -> c.getSensorId().toString())
                .collect(Collectors.toSet()));
        ensureSensorsExist(hubId, actions.stream()
                .map(a -> a.getSensorId().toString())
                .collect(Collectors.toSet()));

        for (ScenarioConditionAvro conditionAvro : conditions) {
            Sensor sensor = getSensorOrThrow(hubId, conditionAvro.getSensorId().toString());
            Condition condition = conditionRepository.save(buildCondition(conditionAvro));
            ScenarioConditionBinding binding = new ScenarioConditionBinding();
            binding.setScenario(scenario);
            binding.setSensor(sensor);
            binding.setCondition(condition);
            binding.setId(new ScenarioConditionKey(
                    scenario.getId(),
                    sensor.getId(),
                    condition.getId()
            ));
            conditionBindingRepository.save(binding);
        }

        for (DeviceActionAvro actionAvro : actions) {
            Sensor sensor = getSensorOrThrow(hubId, actionAvro.getSensorId().toString());
            Action action = actionRepository.save(buildAction(actionAvro));
            ScenarioActionBinding binding = new ScenarioActionBinding();
            binding.setScenario(scenario);
            binding.setSensor(sensor);
            binding.setAction(action);
            binding.setId(new ScenarioActionKey(
                    scenario.getId(),
                    sensor.getId(),
                    action.getId()
            ));
            actionBindingRepository.save(binding);
        }

        log.info("Scenario {} for hub {} saved ({} conditions, {} actions)",
                scenarioName, hubId, conditions.size(), actions.size());
    }

    private void removeScenario(String hubId, ScenarioRemovedEventAvro payload) {
        String scenarioName = payload.getName().toString();
        scenarioRepository.findByHubIdAndName(hubId, scenarioName).ifPresentOrElse(scenario -> {
            cleanupScenarioBindings(scenario.getId());
            scenarioRepository.delete(scenario);
            log.info("Scenario {} for hub {} removed", scenarioName, hubId);
        }, () -> log.info("Skip removing unknown scenario {} for hub {}", scenarioName, hubId));
    }

    private void cleanupScenarioBindings(Long scenarioId) {
        List<ScenarioConditionBinding> conditionBindings = conditionBindingRepository.findByScenario_Id(scenarioId);
        List<Long> conditionIds = conditionBindings.stream()
                .map(binding -> binding.getCondition().getId())
                .toList();
        conditionBindingRepository.deleteAll(conditionBindings);
        if (!conditionIds.isEmpty()) {
            conditionRepository.deleteAllById(conditionIds);
        }

        List<ScenarioActionBinding> actionBindings = actionBindingRepository.findByScenario_Id(scenarioId);
        List<Long> actionIds = actionBindings.stream()
                .map(binding -> binding.getAction().getId())
                .toList();
        actionBindingRepository.deleteAll(actionBindings);
        if (!actionIds.isEmpty()) {
            actionRepository.deleteAllById(actionIds);
        }
        log.debug("Cleaned up {} condition bindings and {} action bindings for scenario {}", conditionIds.size(), actionIds.size(), scenarioId);
    }

    private void removeBindingsForSensor(String sensorId) {
        List<ScenarioConditionBinding> conditionBindings = conditionBindingRepository.findBySensor_Id(sensorId);
        List<Long> conditionIds = conditionBindings.stream()
                .map(binding -> binding.getCondition().getId())
                .toList();
        conditionBindingRepository.deleteAll(conditionBindings);
        if (!conditionIds.isEmpty()) {
            conditionRepository.deleteAllById(conditionIds);
        }

        List<ScenarioActionBinding> actionBindings = actionBindingRepository.findBySensor_Id(sensorId);
        List<Long> actionIds = actionBindings.stream()
                .map(binding -> binding.getAction().getId())
                .toList();
        actionBindingRepository.deleteAll(actionBindings);
        if (!actionIds.isEmpty()) {
            actionRepository.deleteAllById(actionIds);
        }
        log.debug("Removed sensor {} bindings: {} conditions, {} actions", sensorId, conditionIds.size(), actionIds.size());
    }

    private void ensureSensorsExist(String hubId, Set<String> sensorIds) {
        for (String sensorId : sensorIds) {
            if (sensorId == null) {
                continue;
            }
            sensorRepository.findByIdAndHubId(sensorId, hubId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Sensor %s is not registered in hub %s".formatted(sensorId, hubId)
                    ));
        }
    }

    private Sensor getSensorOrThrow(String hubId, String sensorId) {
        return sensorRepository.findByIdAndHubId(sensorId, hubId)
                .orElseThrow(() -> new IllegalStateException(
                        "Sensor %s is not registered in hub %s".formatted(sensorId, hubId)
                ));
    }

    private Condition buildCondition(ScenarioConditionAvro avro) {
        Condition condition = new Condition();
        condition.setType(ConditionType.valueOf(avro.getType().name()));
        condition.setOperation(ConditionOperation.valueOf(avro.getOperation().name()));
        condition.setValue(extractValue(avro.getValue()));
        return condition;
    }

    private Action buildAction(DeviceActionAvro avro) {
        Action action = new Action();
        action.setType(ActionType.valueOf(avro.getType().name()));
        action.setValue(extractNumericValue(avro.getValue()));
        return action;
    }

    private Integer extractValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        throw new IllegalArgumentException("Unsupported condition value type: " + value.getClass());
    }

    private Integer extractNumericValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        throw new IllegalArgumentException("Unsupported action value type: " + value.getClass());
    }
}

