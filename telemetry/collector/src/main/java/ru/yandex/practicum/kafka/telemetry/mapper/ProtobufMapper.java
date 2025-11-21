package ru.yandex.practicum.kafka.telemetry.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.ConditionOperationProto;
import ru.yandex.practicum.grpc.telemetry.ConditionTypeProto;
import ru.yandex.practicum.grpc.telemetry.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.DeviceTypeProto;
import ru.yandex.practicum.grpc.telemetry.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.ScenarioConditionProto;
import ru.yandex.practicum.grpc.telemetry.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtobufMapper {

    private final AvroMapper avroMapper;

    public SensorEvent toDto(SensorEventProto proto) {
        Instant timestamp = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );

        SensorEvent event = switch (proto.getPayloadCase()) {
            case MOTION_SENSOR -> {
                var motionSensor = proto.getMotionSensor();
                var dto = new MotionSensorEvent();
                dto.setLinkQuality(motionSensor.getLinkQuality());
                dto.setMotion(motionSensor.getMotion());
                dto.setVoltage(motionSensor.getVoltage());
                yield dto;
            }
            case TEMPERATURE_SENSOR -> {
                var tempSensor = proto.getTemperatureSensor();
                var dto = new TemperatureSensorEvent();
                dto.setTemperatureC(tempSensor.getTemperatureC());
                dto.setTemperatureF(tempSensor.getTemperatureF());
                yield dto;
            }
            case LIGHT_SENSOR -> {
                var lightSensor = proto.getLightSensor();
                var dto = new LightSensorEvent();
                dto.setLinkQuality(lightSensor.getLinkQuality());
                dto.setLuminosity(lightSensor.getLuminosity());
                yield dto;
            }
            case CLIMATE_SENSOR -> {
                var climateSensor = proto.getClimateSensor();
                var dto = new ClimateSensorEvent();
                dto.setTemperatureC(climateSensor.getTemperatureC());
                dto.setHumidity(climateSensor.getHumidity());
                dto.setCo2Level(climateSensor.getCo2Level());
                yield dto;
            }
            case SWITCH_SENSOR -> {
                var switchSensor = proto.getSwitchSensor();
                var dto = new SwitchSensorEvent();
                dto.setState(switchSensor.getState());
                yield dto;
            }
            default -> throw new IllegalArgumentException("Unknown sensor event payload type: " + proto.getPayloadCase());
        };

        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(timestamp);

        return event;
    }

    public HubEvent toDto(HubEventProto proto) {
        Instant timestamp = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );

        HubEvent event = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                var deviceAdded = proto.getDeviceAdded();
                var dto = new DeviceAddedEvent();
                dto.setId(deviceAdded.getId());
                dto.setDeviceType(mapDeviceType(deviceAdded.getType()));
                yield dto;
            }
            case DEVICE_REMOVED -> {
                var deviceRemoved = proto.getDeviceRemoved();
                var dto = new DeviceRemovedEvent();
                dto.setId(deviceRemoved.getId());
                yield dto;
            }
            case SCENARIO_ADDED -> {
                var scenarioAdded = proto.getScenarioAdded();
                var dto = new ScenarioAddedEvent();
                dto.setName(scenarioAdded.getName());
                dto.setConditions(scenarioAdded.getConditionList().stream()
                        .map(this::mapScenarioCondition)
                        .collect(Collectors.toList()));
                dto.setActions(scenarioAdded.getActionList().stream()
                        .map(this::mapDeviceAction)
                        .collect(Collectors.toList()));
                yield dto;
            }
            case SCENARIO_REMOVED -> {
                var scenarioRemoved = proto.getScenarioRemoved();
                var dto = new ScenarioRemovedEvent();
                dto.setName(scenarioRemoved.getName());
                yield dto;
            }
            default -> throw new IllegalArgumentException("Unknown hub event payload type: " + proto.getPayloadCase());
        };

        event.setHubId(proto.getHubId());
        event.setTimestamp(timestamp);

        return event;
    }

    public SensorEventAvro toAvro(SensorEvent dto) {
        return avroMapper.toAvro(dto);
    }

    public HubEventAvro toAvro(HubEvent dto) {
        return avroMapper.toAvro(dto);
    }

    private DeviceType mapDeviceType(DeviceTypeProto proto) {
        return switch (proto) {
            case MOTION_SENSOR -> DeviceType.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceType.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceType.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceType.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceType.SWITCH_SENSOR;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown device type: " + proto);
        };
    }

    private ConditionType mapConditionType(ConditionTypeProto proto) {
        return switch (proto) {
            case MOTION -> ConditionType.MOTION;
            case LUMINOSITY -> ConditionType.LUMINOSITY;
            case SWITCH -> ConditionType.SWITCH;
            case TEMPERATURE -> ConditionType.TEMPERATURE;
            case CO2LEVEL -> ConditionType.CO2LEVEL;
            case HUMIDITY -> ConditionType.HUMIDITY;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown condition type: " + proto);
        };
    }

    private ConditionOperation mapConditionOperation(ConditionOperationProto proto) {
        return switch (proto) {
            case EQUALS -> ConditionOperation.EQUALS;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown condition operation: " + proto);
        };
    }

    private ActionType mapActionType(ActionTypeProto proto) {
        return switch (proto) {
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case INVERSE -> ActionType.INVERSE;
            case SET_VALUE -> ActionType.SET_VALUE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }

    private ScenarioCondition mapScenarioCondition(ScenarioConditionProto proto) {
        var condition = new ScenarioCondition();
        condition.setSensorId(proto.getSensorId());
        condition.setType(mapConditionType(proto.getType()));
        condition.setOperation(mapConditionOperation(proto.getOperation()));
        
        if (proto.getValueCase() == ScenarioConditionProto.ValueCase.INT_VALUE) {
            condition.setValue(proto.getIntValue());
        } else if (proto.getValueCase() == ScenarioConditionProto.ValueCase.BOOL_VALUE) {
            // Преобразуем boolean в Integer: true -> 1, false -> 0
            condition.setValue(proto.getBoolValue() ? 1 : 0);
        }
        
        return condition;
    }

    private DeviceAction mapDeviceAction(DeviceActionProto proto) {
        var action = new DeviceAction();
        action.setSensorId(proto.getSensorId());
        action.setType(mapActionType(proto.getType()));
        
        if (proto.hasValue()) {
            action.setValue(proto.getValue());
        }
        
        return action;
    }
}
