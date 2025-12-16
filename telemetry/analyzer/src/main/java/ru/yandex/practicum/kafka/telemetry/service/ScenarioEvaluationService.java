package ru.yandex.practicum.kafka.telemetry.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.domain.ConditionType;
import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioActionBinding;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioConditionBinding;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioEvaluationService {

    public List<DeviceActionRequest> evaluate(
            SensorsSnapshotAvro snapshot,
            List<Scenario> scenarios
    ) {
        if (scenarios.isEmpty()) {
            return List.of();
        }

        Map<String, SensorStateAvro> states = snapshot.getSensorsState();
        Timestamp timestamp = toTimestamp(snapshot.getTimestamp());
        String hubId = snapshot.getHubId();

        List<DeviceActionRequest> requests = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            if (matchesAllConditions(scenario, states)) {
                log.info("Scenario '{}' for hub {} satisfied conditions", scenario.getName(), hubId);
                for (ScenarioActionBinding binding : scenario.getActions()) {
                    DeviceActionProto actionProto = buildActionProto(binding);
                    DeviceActionRequest request =
                            DeviceActionRequest.newBuilder()
                                    .setHubId(hubId)
                                    .setScenarioName(scenario.getName())
                                    .setAction(actionProto)
                                    .setTimestamp(timestamp)
                                    .build();
                    requests.add(request);
                }
            }
        }
        if (requests.isEmpty()) {
            log.debug("No scenarios satisfied for hub {}", hubId);
        }
        return requests;
    }

    private boolean matchesAllConditions(Scenario scenario, Map<String, SensorStateAvro> states) {
        for (ScenarioConditionBinding binding : scenario.getConditions()) {
            SensorStateAvro state = states.get(binding.getSensor().getId());
            if (state == null) {
                return false;
            }
            Integer actualValue = extractValue(binding.getCondition().getType(), state);
            Integer expectedValue = binding.getCondition().getValue();
            if (actualValue == null || expectedValue == null) {
                return false;
            }
            if (!binding.getCondition().getOperation().matches(actualValue, expectedValue)) {
                log.debug("Scenario '{}' condition failed for sensor {}: actual={}, expected={}",
                        scenario.getName(),
                        binding.getSensor().getId(),
                        actualValue,
                        expectedValue);
                return false;
            }
        }
        return true;
    }

    private Integer extractValue(ConditionType type, SensorStateAvro state) {
        Object data = state.getData();
        if (data == null) {
            return null;
        }
        return switch (type) {
            case MOTION -> (data instanceof MotionSensorAvro motion)
                    ? booleanToInt(motion.getMotion()) : null;
            case SWITCH -> (data instanceof SwitchSensorAvro sw)
                    ? booleanToInt(sw.getState()) : null;
            case LUMINOSITY -> (data instanceof LightSensorAvro light)
                    ? light.getLuminosity() : null;
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temperature) {
                    yield temperature.getTemperatureC();
                }
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case CO2LEVEL -> (data instanceof ClimateSensorAvro climate)
                    ? climate.getCo2Level() : null;
            case HUMIDITY -> (data instanceof ClimateSensorAvro climate)
                    ? climate.getHumidity() : null;
        };
    }

    private int booleanToInt(boolean value) {
        return value ? 1 : 0;
    }

    private DeviceActionProto buildActionProto(ScenarioActionBinding binding) {
        Action action = binding.getAction();
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(binding.getSensor().getId())
                .setType(ActionTypeProto.valueOf(action.getType().name()));
        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }
        return builder.build();
    }

    private Timestamp toTimestamp(long epochMillis) {
        long seconds = epochMillis / 1000;
        int nanos = (int) ((epochMillis % 1000) * 1_000_000);
        return Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }
}

