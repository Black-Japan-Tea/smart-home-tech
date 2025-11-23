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
            log.debug("Converting sensor event to Avro: type={}, id={}, hubId={}", 
                    event != null ? event.getClass().getSimpleName() : "null",
                    event != null ? event.getId() : "null",
                    event != null ? event.getHubId() : "null");
            
            if (event == null) {
                throw new IllegalArgumentException("Sensor event cannot be null");
            }
            if (event.getId() == null || event.getId().isEmpty()) {
                throw new IllegalArgumentException("Sensor event id cannot be null or empty");
            }
            if (event.getHubId() == null || event.getHubId().isEmpty()) {
                throw new IllegalArgumentException("Sensor event hubId cannot be null or empty");
            }
            if (event.getTimestamp() == null) {
                throw new IllegalArgumentException("Sensor event timestamp cannot be null");
            }
            
            SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                    .setId(event.getId())
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp().toEpochMilli());

        Object payload;
            switch (event) {
                case ClimateSensorEvent climateEvent -> {
                    if (climateEvent.getTemperatureC() == null || climateEvent.getHumidity() == null || climateEvent.getCo2Level() == null) {
                        throw new IllegalArgumentException("Climate sensor event fields cannot be null");
                    }
                    payload = ClimateSensorAvro.newBuilder()
                            .setTemperatureC(climateEvent.getTemperatureC())
                            .setHumidity(climateEvent.getHumidity())
                            .setCo2Level(climateEvent.getCo2Level())
                            .build();
                }
                case LightSensorEvent lightEvent -> {
                    LightSensorAvro.Builder lightBuilder = LightSensorAvro.newBuilder();
                    lightBuilder.setLinkQuality(lightEvent.getLinkQuality() != null ? lightEvent.getLinkQuality() : 0);
                    lightBuilder.setLuminosity(lightEvent.getLuminosity() != null ? lightEvent.getLuminosity() : 0);
                    payload = lightBuilder.build();
                }
                case MotionSensorEvent motionEvent -> {
                    if (motionEvent.getLinkQuality() == null || motionEvent.getMotion() == null || motionEvent.getVoltage() == null) {
                        throw new IllegalArgumentException("Motion sensor event fields cannot be null");
                    }
                    payload = MotionSensorAvro.newBuilder()
                            .setLinkQuality(motionEvent.getLinkQuality())
                            .setMotion(motionEvent.getMotion())
                            .setVoltage(motionEvent.getVoltage())
                            .build();
                }
                case SwitchSensorEvent switchEvent -> {
                    if (switchEvent.getState() == null) {
                        throw new IllegalArgumentException("Switch sensor event state cannot be null");
                    }
                    payload = SwitchSensorAvro.newBuilder()
                            .setState(switchEvent.getState())
                            .build();
                }
                case TemperatureSensorEvent tempEvent -> {
                    if (tempEvent.getTemperatureC() == null || tempEvent.getTemperatureF() == null) {
                        throw new IllegalArgumentException("Temperature sensor event fields cannot be null");
                    }
                    payload = TemperatureSensorAvro.newBuilder()
                            .setTemperatureC(tempEvent.getTemperatureC())
                            .setTemperatureF(tempEvent.getTemperatureF())
                            .build();
                }
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
            log.debug("Converting hub event to Avro: type={}, hubId={}", 
                    event != null ? event.getClass().getSimpleName() : "null",
                    event != null ? event.getHubId() : "null");
            
            if (event == null) {
                throw new IllegalArgumentException("Hub event cannot be null");
            }
            if (event.getHubId() == null || event.getHubId().isEmpty()) {
                throw new IllegalArgumentException("Hub event hubId cannot be null or empty");
            }
            if (event.getTimestamp() == null) {
                throw new IllegalArgumentException("Hub event timestamp cannot be null");
            }
            
            HubEventAvro.Builder builder = HubEventAvro.newBuilder()
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
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapConditionOperation(condition.getOperation()));

        if (condition.getValue() != null) {
            builder.setValue(condition.getValue());
        }

        return builder.build();
    }

    private DeviceActionAvro mapAction(DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }

        return builder.build();
    }
}