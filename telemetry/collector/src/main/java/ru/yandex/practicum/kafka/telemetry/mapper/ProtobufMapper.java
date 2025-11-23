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
import ru.yandex.practicum.grpc.telemetry.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.TemperatureSensorProto;
import ru.yandex.practicum.grpc.telemetry.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.SwitchSensorProto;
import ru.yandex.practicum.grpc.telemetry.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.ScenarioRemovedEventProto;
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
        Instant timestamp;
        if (proto.hasTimestamp()) {
            timestamp = Instant.ofEpochSecond(
                    proto.getTimestamp().getSeconds(),
                    proto.getTimestamp().getNanos()
            );
        } else {
            timestamp = Instant.now();
        }

        SensorEvent event;
        SensorEventProto.PayloadCase payloadCase = proto.getPayloadCase();
        
        if (payloadCase == SensorEventProto.PayloadCase.MOTION_SENSOR) {
            MotionSensorProto motionSensor = proto.getMotionSensor();
            MotionSensorEvent dto = new MotionSensorEvent();
            dto.setLinkQuality(motionSensor.getLinkQuality());
            dto.setMotion(motionSensor.getMotion());
            dto.setVoltage(motionSensor.getVoltage());
            event = dto;
        } else if (payloadCase == SensorEventProto.PayloadCase.TEMPERATURE_SENSOR) {
            TemperatureSensorProto tempSensor = proto.getTemperatureSensor();
            TemperatureSensorEvent dto = new TemperatureSensorEvent();
            dto.setTemperatureC(tempSensor.getTemperatureC());
            dto.setTemperatureF(tempSensor.getTemperatureF());
            event = dto;
        } else if (payloadCase == SensorEventProto.PayloadCase.LIGHT_SENSOR) {
            LightSensorProto lightSensor = proto.getLightSensor();
            LightSensorEvent dto = new LightSensorEvent();
            dto.setLinkQuality(lightSensor.getLinkQuality());
            dto.setLuminosity(lightSensor.getLuminosity());
            event = dto;
        } else if (payloadCase == SensorEventProto.PayloadCase.CLIMATE_SENSOR) {
            ClimateSensorProto climateSensor = proto.getClimateSensor();
            ClimateSensorEvent dto = new ClimateSensorEvent();
            dto.setTemperatureC(climateSensor.getTemperatureC());
            dto.setHumidity(climateSensor.getHumidity());
            dto.setCo2Level(climateSensor.getCo2Level());
            event = dto;
        } else if (payloadCase == SensorEventProto.PayloadCase.SWITCH_SENSOR) {
            SwitchSensorProto switchSensor = proto.getSwitchSensor();
            SwitchSensorEvent dto = new SwitchSensorEvent();
            dto.setState(switchSensor.getState());
            event = dto;
        } else {
            throw new IllegalArgumentException("Unknown sensor event payload type: " + payloadCase);
        }

        String id = proto.getId();
        String hubId = proto.getHubId();
        
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Sensor event id cannot be null or empty");
        }
        if (hubId.isEmpty()) {
            throw new IllegalArgumentException("Sensor event hubId cannot be null or empty");
        }
        
        event.setId(id);
        event.setHubId(hubId);
        event.setTimestamp(timestamp);

        return event;
    }

    public HubEvent toDto(HubEventProto proto) {
        Instant timestamp;
        if (proto.hasTimestamp()) {
            timestamp = Instant.ofEpochSecond(
                    proto.getTimestamp().getSeconds(),
                    proto.getTimestamp().getNanos()
            );
        } else {
            timestamp = Instant.now();
        }

        HubEvent event;
        HubEventProto.PayloadCase payloadCase = proto.getPayloadCase();
        
        if (payloadCase == HubEventProto.PayloadCase.DEVICE_ADDED) {
            DeviceAddedEventProto deviceAdded = proto.getDeviceAdded();
            DeviceAddedEvent dto = new DeviceAddedEvent();
            dto.setId(deviceAdded.getId());
            dto.setDeviceType(mapDeviceType(deviceAdded.getType()));
            event = dto;
        } else if (payloadCase == HubEventProto.PayloadCase.DEVICE_REMOVED) {
            DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();
            DeviceRemovedEvent dto = new DeviceRemovedEvent();
            dto.setId(deviceRemoved.getId());
            event = dto;
        } else if (payloadCase == HubEventProto.PayloadCase.SCENARIO_ADDED) {
            ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();
            ScenarioAddedEvent dto = new ScenarioAddedEvent();
            dto.setName(scenarioAdded.getName());
            dto.setConditions(scenarioAdded.getConditionList().stream()
                    .map(this::mapScenarioCondition)
                    .collect(Collectors.toList()));
            dto.setActions(scenarioAdded.getActionList().stream()
                    .map(this::mapDeviceAction)
                    .collect(Collectors.toList()));
            event = dto;
        } else if (payloadCase == HubEventProto.PayloadCase.SCENARIO_REMOVED) {
            ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();
            ScenarioRemovedEvent dto = new ScenarioRemovedEvent();
            dto.setName(scenarioRemoved.getName());
            event = dto;
        } else {
            throw new IllegalArgumentException("Unknown hub event payload type: " + payloadCase);
        }

        String hubId = proto.getHubId();
        
        if (hubId.isEmpty()) {
            throw new IllegalArgumentException("Hub event hubId cannot be null or empty");
        }
        
        event.setHubId(hubId);
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
            default -> throw new IllegalArgumentException("Unknown device type: " + proto);
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
            default -> throw new IllegalArgumentException("Unknown condition type: " + proto);
        };
    }

    private ConditionOperation mapConditionOperation(ConditionOperationProto proto) {
        return switch (proto) {
            case EQUALS -> ConditionOperation.EQUALS;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
            default -> throw new IllegalArgumentException("Unknown condition operation: " + proto);
        };
    }

    private ActionType mapActionType(ActionTypeProto proto) {
        return switch (proto) {
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case INVERSE -> ActionType.INVERSE;
            case SET_VALUE -> ActionType.SET_VALUE;
            default -> throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }

    private ScenarioCondition mapScenarioCondition(ScenarioConditionProto proto) {
        ScenarioCondition condition = new ScenarioCondition();
        condition.setSensorId(proto.getSensorId());
        condition.setType(mapConditionType(proto.getType()));
        condition.setOperation(mapConditionOperation(proto.getOperation()));

        if (proto.getValueCase() == ScenarioConditionProto.ValueCase.INT_VALUE) {
            condition.setValue(proto.getIntValue());
        } else if (proto.getValueCase() == ScenarioConditionProto.ValueCase.BOOL_VALUE) {
            condition.setValue(proto.getBoolValue() ? 1 : 0);
        }

        return condition;
    }

    private DeviceAction mapDeviceAction(DeviceActionProto proto) {
        DeviceAction action = new DeviceAction();
        action.setSensorId(proto.getSensorId());
        action.setType(mapActionType(proto.getType()));

        if (proto.hasValue()) {
            action.setValue(proto.getValue());
        }

        return action;
    }
}