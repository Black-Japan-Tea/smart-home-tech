package ru.yandex.practicum.kafka.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Condition;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.entity.Sensor;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

class ScenarioEvaluationServiceTest {

    private ScenarioEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new ScenarioEvaluationService();
    }

    @Test
    void shouldReturnActionsWhenAllConditionsSatisfied() {
        Scenario scenario = baseScenario();
        scenario.getConditions().add(condition(scenario, "temp-1", "TEMPERATURE", "GREATER_THAN", 20));
        scenario.getConditions().add(condition(scenario, "switch-1", "SWITCH", "EQUALS", 1));
        ScenarioAction action = action(scenario, "relay-1", "ACTIVATE", 1);
        scenario.getActions().add(action);

        SensorsSnapshotAvro snapshot = snapshot(Map.of(
            "temp-1", temperatureState(25),
            "switch-1", switchState(true)
        ));

        List<ScenarioAction> result = service.evaluateScenario(scenario, snapshot);

        assertThat(result).containsExactly(action);
    }

    @Test
    void shouldReturnEmptyListWhenSensorMissing() {
        Scenario scenario = baseScenario();
        scenario.getConditions().add(condition(scenario, "temp-1", "TEMPERATURE", "GREATER_THAN", 20));
        scenario.getActions().add(action(scenario, "relay-1", "ACTIVATE", 1));

        SensorsSnapshotAvro snapshot = snapshot(Map.of()); // нет показаний датчиков

        List<ScenarioAction> result = service.evaluateScenario(scenario, snapshot);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenConditionNotMet() {
        Scenario scenario = baseScenario();
        scenario.getConditions().add(condition(scenario, "switch-1", "SWITCH", "EQUALS", 1));
        scenario.getActions().add(action(scenario, "relay-1", "ACTIVATE", 1));

        SensorsSnapshotAvro snapshot = snapshot(Map.of(
            "switch-1", switchState(false)
        ));

        List<ScenarioAction> result = service.evaluateScenario(scenario, snapshot);

        assertThat(result).isEmpty();
    }

    private Scenario baseScenario() {
        Scenario scenario = new Scenario();
        scenario.setHubId("hub-1");
        scenario.setName("test");
        scenario.setConditions(new ArrayList<>());
        scenario.setActions(new ArrayList<>());
        return scenario;
    }

    private ScenarioCondition condition(
        Scenario scenario,
        String sensorId,
        String type,
        String operation,
        Integer value
    ) {
        Sensor sensor = new Sensor();
        sensor.setId(sensorId);

        Condition condition = new Condition();
        condition.setType(type);
        condition.setOperation(operation);
        condition.setValue(value);

        ScenarioCondition scenarioCondition = new ScenarioCondition();
        scenarioCondition.setScenario(scenario);
        scenarioCondition.setSensor(sensor);
        scenarioCondition.setCondition(condition);
        return scenarioCondition;
    }

    private ScenarioAction action(Scenario scenario, String sensorId, String type, Integer value) {
        Sensor sensor = new Sensor();
        sensor.setId(sensorId);

        Action action = new Action();
        action.setType(type);
        action.setValue(value);

        ScenarioAction scenarioAction = new ScenarioAction();
        scenarioAction.setScenario(scenario);
        scenarioAction.setSensor(sensor);
        scenarioAction.setAction(action);
        return scenarioAction;
    }

    private SensorsSnapshotAvro snapshot(Map<String, SensorStateAvro> sensors) {
        return SensorsSnapshotAvro.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(100L)
            .setSensorsState(new HashMap<>(sensors))
            .build();
    }

    private SensorStateAvro temperatureState(int value) {
        TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
            .setTemperatureC(value)
            .setTemperatureF(value * 2)
            .build();
        return SensorStateAvro.newBuilder()
            .setTimestamp(100L)
            .setData(payload)
            .build();
    }

    private SensorStateAvro switchState(boolean state) {
        SwitchSensorAvro payload = SwitchSensorAvro.newBuilder()
            .setState(state)
            .build();
        return SensorStateAvro.newBuilder()
            .setTimestamp(100L)
            .setData(payload)
            .build();
    }
}

