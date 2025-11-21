package ru.yandex.practicum.kafka.telemetry.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AvroMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        try {
            var builder = SensorEventAvro.newBuilder()
                    .setId(event.getId())
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp().toEpochMilli());

        Object payload;
        switch (event) {
            case ClimateSensorEvent climateEvent -> payload = ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climateEvent.getTemperatureC())
                    .setHumidity(climateEvent.getHumidity())
                    .setCo2Level(climateEvent.getCo2Level())
                    .build();
            case LightSensorEvent lightEvent -> {
                var lightBuilder = LightSensorAvro.newBuilder();
                lightBuilder.setLinkQuality(lightEvent.getLinkQuality() != null ? lightEvent.getLinkQuality() : 0);
                lightBuilder.setLuminosity(lightEvent.getLuminosity() != null ? lightEvent.getLuminosity() : 0);
                payload = lightBuilder.build();
            }
            case MotionSensorEvent motionEvent -> payload = MotionSensorAvro.newBuilder()
                    .setLinkQuality(motionEvent.getLinkQuality())
                    .setMotion(motionEvent.getMotion())
                    .setVoltage(motionEvent.getVoltage())
                    .build();
            case SwitchSensorEvent switchEvent -> payload = SwitchSensorAvro.newBuilder()
                    .setState(switchEvent.getState())
                    .build();
            case TemperatureSensorEvent tempEvent -> payload = TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(tempEvent.getTemperatureC())
                    .setTemperatureF(tempEvent.getTemperatureF())
                    .build();
            default -> throw new IllegalArgumentException("Unknown sensor event type: " + event.getClass());
        }

            return builder.setPayload(payload).build();
        } catch (Exception e) {
            log.error("Error converting sensor event to Avro: {}", event, e);
            throw new RuntimeException("Failed to convert sensor event to Avro", e);
        }
    }

    public HubEventAvro toAvro(HubEvent event) {
        try {
            var builder = HubEventAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp().toEpochMilli());

        Object payload = switch (event) {
            case DeviceAddedEvent deviceAdded -> DeviceAddedEventAvro.newBuilder()
                    .setId(deviceAdded.getId())
                    .setType(mapDeviceType(deviceAdded.getDeviceType()))
                    .build();
            case DeviceRemovedEvent deviceRemoved -> DeviceRemovedEventAvro.newBuilder()
                    .setId(deviceRemoved.getId())
                    .build();
            case ScenarioAddedEvent scenarioAdded -> ScenarioAddedEventAvro.newBuilder()
                    .setName(scenarioAdded.getName())
                    .setConditions(scenarioAdded.getConditions().stream()
                            .map(this::mapCondition)
                            .collect(Collectors.toList()))
                    .setActions(scenarioAdded.getActions().stream()
                            .map(this::mapAction)
                            .collect(Collectors.toList()))
                    .build();
            case ScenarioRemovedEvent scenarioRemoved -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(scenarioRemoved.getName())
                    .build();
            default -> throw new IllegalArgumentException("Unknown hub event type: " + event.getClass());
        };

            return builder.setPayload(payload).build();
        } catch (Exception e) {
            log.error("Error converting hub event to Avro: {}", event, e);
            throw new RuntimeException("Failed to convert hub event to Avro", e);
        }
    }

    private DeviceTypeAvro mapDeviceType(DeviceType type) {
        return switch (type) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
        };
    }

    private ConditionTypeAvro mapConditionType(ConditionType type) {
        return switch (type) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
        };
    }

    private ConditionOperationAvro mapConditionOperation(ConditionOperation operation) {
        return switch (operation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
        };
    }

    private ActionTypeAvro mapActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioCondition condition) {
        var builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapConditionOperation(condition.getOperation()));

        if (condition.getValue() != null) {
            builder.setValue(condition.getValue());
        }

        return builder.build();
    }

    private DeviceActionAvro mapAction(DeviceAction action) {
        var builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }

        return builder.build();
    }
}