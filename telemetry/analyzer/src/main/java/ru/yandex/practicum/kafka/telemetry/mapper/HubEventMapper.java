package ru.yandex.practicum.kafka.telemetry.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.entity.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.repository.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Маппер для преобразования Avro событий в JPA сущности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventMapper {

    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    /**
     * Создает или обновляет Sensor из DeviceAddedEventAvro.
     */
    public Sensor toSensor(String hubId, DeviceAddedEventAvro event) {
        Sensor sensor = sensorRepository.findById(event.getId())
            .orElse(new Sensor());
        sensor.setId(event.getId());
        sensor.setHubId(hubId);
        return sensor;
    }

    /**
     * Создает Scenario из ScenarioAddedEventAvro.
     */
    public Scenario toScenario(String hubId, ScenarioAddedEventAvro event) {
        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(event.getName());
        scenario.setConditions(new ArrayList<>());
        scenario.setActions(new ArrayList<>());

        log.debug("Создаем сценарий: hubId={}, name={}, условий={}, действий={}", 
            hubId, event.getName(), event.getConditions().size(), event.getActions().size());

        // Создаем условия
        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            Condition condition = createOrGetCondition(conditionAvro);
            Sensor sensor = sensorRepository.findById(conditionAvro.getSensorId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Sensor not found: " + conditionAvro.getSensorId()));
            
            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);
            scenario.getConditions().add(scenarioCondition);
            
            log.debug("Добавлено условие: sensorId={}, type={}, operation={}, value={}", 
                conditionAvro.getSensorId(), conditionAvro.getType(), 
                conditionAvro.getOperation(), conditionAvro.getValue());
        }

        // Создаем действия
        for (DeviceActionAvro actionAvro : event.getActions()) {
            Action action = createOrGetAction(actionAvro);
            Sensor sensor = sensorRepository.findById(actionAvro.getSensorId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Sensor not found: " + actionAvro.getSensorId()));
            
            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);
            scenario.getActions().add(scenarioAction);
            
            log.debug("Добавлено действие: sensorId={}, type={}, value={}", 
                actionAvro.getSensorId(), actionAvro.getType(), actionAvro.getValue());
        }

        log.debug("Сценарий создан: hubId={}, name={}, условий={}, действий={}", 
            hubId, event.getName(), scenario.getConditions().size(), scenario.getActions().size());

        return scenario;
    }

    private Condition createOrGetCondition(ScenarioConditionAvro conditionAvro) {
        String type = conditionAvro.getType().toString();
        String operation = conditionAvro.getOperation().toString();
        Integer value = extractConditionValue(conditionAvro);

        // Ищем существующее условие
        return conditionRepository.findAll().stream()
            .filter(c -> c.getType().equals(type) 
                && c.getOperation().equals(operation)
                && java.util.Objects.equals(c.getValue(), value))
            .findFirst()
            .orElseGet(() -> {
                Condition condition = new Condition();
                condition.setType(type);
                condition.setOperation(operation);
                condition.setValue(value);
                return conditionRepository.save(condition);
            });
    }

    private Action createOrGetAction(DeviceActionAvro actionAvro) {
        String type = actionAvro.getType().toString();
        Integer value = actionAvro.getValue() != null ? actionAvro.getValue() : null;

        // Ищем существующее действие
        return actionRepository.findAll().stream()
            .filter(a -> a.getType().equals(type)
                && java.util.Objects.equals(a.getValue(), value))
            .findFirst()
            .orElseGet(() -> {
                Action action = new Action();
                action.setType(type);
                action.setValue(value);
                return actionRepository.save(action);
            });
    }

    private Integer extractConditionValue(ScenarioConditionAvro conditionAvro) {
        Object value = conditionAvro.getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return null;
    }
}

