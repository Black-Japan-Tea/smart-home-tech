package ru.yandex.practicum.kafka.telemetry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.entity.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для проверки условий сценариев и определения действий для выполнения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioEvaluationService {

    /**
     * Проверяет все условия сценария и возвращает список действий для выполнения,
     * если все условия выполнены.
     */
    public List<ScenarioAction> evaluateScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        
        // Проверяем, что все условия сценария выполнены
        boolean allConditionsMet = scenario.getConditions().stream()
            .allMatch(scenarioCondition -> {
                Sensor sensor = scenarioCondition.getSensor();
                Condition condition = scenarioCondition.getCondition();
                SensorStateAvro sensorState = sensorsState.get(sensor.getId());
                
                if (sensorState == null) {
                    log.debug("Sensor {} not found in snapshot for scenario {}", 
                        sensor.getId(), scenario.getName());
                    return false;
                }
                
                return evaluateCondition(condition, sensorState);
            });
        
        if (allConditionsMet) {
            log.info("All conditions met for scenario {} in hub {}", 
                scenario.getName(), scenario.getHubId());
            return scenario.getActions();
        }
        
        return List.of();
    }

    /**
     * Проверяет одно условие на основе данных датчика из снапшота.
     */
    private boolean evaluateCondition(Condition condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        String conditionType = condition.getType();
        String operation = condition.getOperation();
        Integer conditionValue = condition.getValue();
        
        return switch (conditionType) {
            case "MOTION" -> evaluateMotionCondition(operation, conditionValue, sensorData);
            case "LUMINOSITY" -> evaluateLuminosityCondition(operation, conditionValue, sensorData);
            case "SWITCH" -> evaluateSwitchCondition(operation, conditionValue, sensorData);
            case "TEMPERATURE" -> evaluateTemperatureCondition(operation, conditionValue, sensorData);
            case "CO2LEVEL" -> evaluateCo2LevelCondition(operation, conditionValue, sensorData);
            case "HUMIDITY" -> evaluateHumidityCondition(operation, conditionValue, sensorData);
            default -> {
                log.warn("Unknown condition type: {}", conditionType);
                yield false;
            }
        };
    }

    private boolean evaluateMotionCondition(String operation, Integer conditionValue, Object sensorData) {
        if (!(sensorData instanceof MotionSensorAvro motionSensor)) {
            return false;
        }
        
        boolean motion = motionSensor.getMotion();
        return switch (operation) {
            case "EQUALS" -> motion == (conditionValue != null && conditionValue != 0);
            default -> false;
        };
    }

    private boolean evaluateLuminosityCondition(String operation, Integer conditionValue, Object sensorData) {
        if (!(sensorData instanceof LightSensorAvro lightSensor)) {
            return false;
        }
        
        int luminosity = lightSensor.getLuminosity();
        if (conditionValue == null) {
            return false;
        }
        
        return switch (operation) {
            case "EQUALS" -> luminosity == conditionValue;
            case "GREATER_THAN" -> luminosity > conditionValue;
            case "LOWER_THAN" -> luminosity < conditionValue;
            default -> false;
        };
    }

    private boolean evaluateSwitchCondition(String operation, Integer conditionValue, Object sensorData) {
        if (!(sensorData instanceof SwitchSensorAvro switchSensor)) {
            return false;
        }
        
        boolean state = switchSensor.getState();
        return switch (operation) {
            case "EQUALS" -> state == (conditionValue != null && conditionValue != 0);
            default -> false;
        };
    }

    private boolean evaluateTemperatureCondition(String operation, Integer conditionValue, Object sensorData) {
        int temperature;
        
        if (sensorData instanceof TemperatureSensorAvro tempSensor) {
            temperature = tempSensor.getTemperatureC();
        } else if (sensorData instanceof ClimateSensorAvro climateSensor) {
            temperature = climateSensor.getTemperatureC();
        } else {
            return false;
        }
        
        if (conditionValue == null) {
            return false;
        }
        
        return switch (operation) {
            case "EQUALS" -> temperature == conditionValue;
            case "GREATER_THAN" -> temperature > conditionValue;
            case "LOWER_THAN" -> temperature < conditionValue;
            default -> false;
        };
    }

    private boolean evaluateCo2LevelCondition(String operation, Integer conditionValue, Object sensorData) {
        if (!(sensorData instanceof ClimateSensorAvro climateSensor)) {
            return false;
        }
        
        int co2Level = climateSensor.getCo2Level();
        if (conditionValue == null) {
            return false;
        }
        
        return switch (operation) {
            case "EQUALS" -> co2Level == conditionValue;
            case "GREATER_THAN" -> co2Level > conditionValue;
            case "LOWER_THAN" -> co2Level < conditionValue;
            default -> false;
        };
    }

    private boolean evaluateHumidityCondition(String operation, Integer conditionValue, Object sensorData) {
        if (!(sensorData instanceof ClimateSensorAvro climateSensor)) {
            return false;
        }
        
        int humidity = climateSensor.getHumidity();
        if (conditionValue == null) {
            return false;
        }
        
        return switch (operation) {
            case "EQUALS" -> humidity == conditionValue;
            case "GREATER_THAN" -> humidity > conditionValue;
            case "LOWER_THAN" -> humidity < conditionValue;
            default -> false;
        };
    }
}

