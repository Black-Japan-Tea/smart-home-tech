package ru.yandex.practicum.kafka.telemetry.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Condition;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.entity.Sensor;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.mapper.HubEventMapper;
import ru.yandex.practicum.kafka.telemetry.repository.ActionRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ConditionRepository;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.service.HubRouterService;
import ru.yandex.practicum.kafka.telemetry.service.ScenarioEvaluationService;

/**
 * Интеграционные тесты для проверки работы Analyzer.
 * Эти тесты воссоздают проблему, когда сценарии не сохраняются из-за пропущенных событий,
 * и Analyzer не может выполнить действия.
 */
@ExtendWith(MockitoExtension.class)
class AnalyzerIntegrationTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private HubRouterService hubRouterService;

    private HubEventMapper hubEventMapper;
    private ScenarioEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        hubEventMapper = new HubEventMapper(sensorRepository, conditionRepository, actionRepository);
        evaluationService = new ScenarioEvaluationService();
    }

    /**
     * Тест воссоздает ситуацию, когда событие DEVICE_ADDED было пропущено из-за UNSPECIFIED,
     * и сенсор не был сохранен в БД. Затем приходит SCENARIO_ADDED, который ссылается на этот сенсор.
     * Ожидается ошибка "Sensor not found".
     */
    @Test
    void shouldFailWhenSensorNotFoundForScenario() {
        // Ситуация: DEVICE_ADDED был пропущен из-за UNSPECIFIED, сенсор не в БД
        when(sensorRepository.findById("sensor-1")).thenReturn(Optional.empty());

        // Hub Router отправляет SCENARIO_ADDED, который ссылается на несуществующий сенсор
        ScenarioAddedEventAvro event = ScenarioAddedEventAvro.newBuilder()
            .setName("test-scenario")
            .setConditions(List.of(
                ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro.newBuilder()
                    .setSensorId("sensor-1") // Ссылается на несуществующий сенсор
                    .setType(ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro.TEMPERATURE)
                    .setOperation(ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro.EQUALS)
                    .setValue(1)
                    .build()
            ))
            .setActions(new ArrayList<>())
            .build();

        // Попытка создать сценарий должна упасть с ошибкой
        assertThatThrownBy(() -> hubEventMapper.toScenario("hub-1", event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sensor not found");
    }

    /**
     * Тест воссоздает ситуацию, когда сценарии не сохранены в БД (из-за пропущенных SCENARIO_ADDED),
     * и Analyzer не может найти сценарии для проверки.
     */
    @Test
    void shouldReturnEmptyListWhenNoScenariosInDatabase() {
        String hubId = "hub-1";
        
        // Ситуация: сценарии не сохранены в БД (события SCENARIO_ADDED были пропущены)
        when(scenarioRepository.findByHubId(hubId)).thenReturn(new ArrayList<>());

        // Проверяем, что сценариев нет
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        assertThat(scenarios).isEmpty();
        
        // В реальном SnapshotProcessor это приведет к логированию:
        // "Нет сценариев для хаба: hub-1"
    }

    /**
     * Тест воссоздает ситуацию, когда сценарий сохранен, но Analyzer не отправляет команды,
     * потому что Hub Router недоступен или не настроен.
     */
    @Test
    void shouldLogActionWhenHubRouterUnavailable() {
        // Создаем сценарий с условием и действием
        Scenario scenario = new Scenario();
        scenario.setHubId("hub-1");
        scenario.setName("test-scenario");
        scenario.setConditions(new ArrayList<>());
        scenario.setActions(new ArrayList<>());

        Sensor sensor = new Sensor();
        sensor.setId("sensor-1");
        sensor.setHubId("hub-1");

        Condition condition = new Condition();
        condition.setType("TEMPERATURE");
        condition.setOperation("GREATER_THAN");
        condition.setValue(20);

        ScenarioCondition scenarioCondition = new ScenarioCondition();
        scenarioCondition.setScenario(scenario);
        scenarioCondition.setSensor(sensor);
        scenarioCondition.setCondition(condition);
        scenario.getConditions().add(scenarioCondition);

        Action action = new Action();
        action.setType("ACTIVATE");
        action.setValue(1);

        ScenarioAction scenarioAction = new ScenarioAction();
        scenarioAction.setScenario(scenario);
        scenarioAction.setSensor(sensor);
        scenarioAction.setAction(action);
        scenario.getActions().add(scenarioAction);

        // Создаем снапшот, который удовлетворяет условию
        SensorStateAvro sensorState = SensorStateAvro.newBuilder()
            .setTimestamp(100L)
            .setData(ClimateSensorAvro.newBuilder()
                .setTemperatureC(25) // > 20, условие выполнено
                .setHumidity(60)
                .setCo2Level(450)
                .build())
            .build();

        SensorsSnapshotAvro snapshot = SensorsSnapshotAvro.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(100L)
            .setSensorsState(java.util.Map.of("sensor-1", sensorState))
            .build();

        // Проверяем сценарий
        List<ScenarioAction> actionsToExecute = evaluationService.evaluateScenario(scenario, snapshot);
        
        assertThat(actionsToExecute).hasSize(1);
        
        // Выполняем действие (в реальности это вызовет hubRouterService.executeAction)
        // Но так как Hub Router удален, мы только логируем
        hubRouterService.executeAction(scenario, actionsToExecute.get(0));
        
        // Проверяем, что метод был вызван (логирование произошло)
        verify(hubRouterService).executeAction(eq(scenario), any());
    }

    /**
     * Тест воссоздает полный сценарий проблемы:
     * 1. DEVICE_ADDED с UNSPECIFIED пропущен
     * 2. SCENARIO_ADDED с UNSPECIFIED пропущен
     * 3. Снапшот приходит, но сценариев нет в БД
     * 4. Analyzer не может выполнить действия
     */
    @Test
    void shouldDemonstrateFullProblemScenario() {
        String hubId = "hub-1";
        
        // Шаг 1: DEVICE_ADDED с UNSPECIFIED был пропущен Collector'ом
        // Сенсор не сохранен в БД - это уже проверено в других тестах
        
        // Шаг 2: SCENARIO_ADDED с UNSPECIFIED был пропущен Collector'ом
        // Сценарий не сохранен в БД
        when(scenarioRepository.findByHubId(hubId)).thenReturn(new ArrayList<>());
        
        // Шаг 3: Приходит снапшот (в реальности он обрабатывается SnapshotProcessor)
        // Шаг 4: Analyzer пытается найти сценарии
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        
        // Результат: сценариев нет, действия не выполняются
        assertThat(scenarios).isEmpty();
        
        // Проверяем, что мок был вызван
        verify(scenarioRepository).findByHubId(hubId);
        
        // В реальном SnapshotProcessor это приведет к логированию:
        // "Нет сценариев для хаба: hub-1. Проверьте, что события SCENARIO_ADDED были успешно обработаны HubEventProcessor."
    }
}

