package ru.yandex.practicum.kafka.telemetry.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.service.HubRouterService;
import ru.yandex.practicum.kafka.telemetry.service.ScenarioEvaluationService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Процессор для обработки снапшотов и проверки сценариев.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    private String snapshotsTopic;

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioEvaluationService evaluationService;
    private final HubRouterService hubRouterService;

    /**
     * Запускает цикл обработки снапшотов.
     */
    public void start() {
        try {
            log.info("Подписываемся на топик: {}", snapshotsTopic);
            consumer.subscribe(Collections.singletonList(snapshotsTopic));

            log.info("Начинаем обработку снапшотов");
            
            long lastLogTime = System.currentTimeMillis();
            int pollCount = 0;

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(100));
                pollCount++;
                
                // Логируем каждые 10 секунд, что консьюмер работает
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 10000) {
                    log.debug("Консьюмер snapshots работает, выполнено {} poll операций", pollCount);
                    lastLogTime = currentTime;
                }
                
                if (!records.isEmpty()) {
                    log.info("Получено {} снапшотов", records.count());
                }

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.info("Получен снапшот: key={}, hubId={}, offset={}, partition={}", 
                        record.key(), 
                        record.value() != null ? record.value().getHubId() : "null",
                        record.offset(),
                        record.partition());
                    try {
                        processSnapshot(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка при обработке снапшота: key={}, offset={}", 
                            record.key(), record.offset(), e);
                    }
                }

                // Фиксируем смещения после обработки батча
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }

        } catch (WakeupException ignored) {
            log.info("Получен сигнал WakeupException, завершаем обработку снапшотов");
        } catch (Exception e) {
            log.error("Ошибка во время обработки снапшотов", e);
        } finally {
            try {
                log.info("Фиксируем смещения консьюмера снапшотов");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер снапшотов");
                consumer.close();
            }
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.info("Обрабатываем снапшот для хаба: {}, количество датчиков: {}", 
            hubId, snapshot.getSensorsState().size());
        
        // Логируем все датчики в снапшоте
        snapshot.getSensorsState().forEach((sensorId, state) -> {
            log.debug("Датчик в снапшоте: id={}, data type={}", 
                sensorId, state.getData() != null ? state.getData().getClass().getSimpleName() : "null");
        });

        // Загружаем все сценарии для данного хаба
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.warn("Нет сценариев для хаба: {}", hubId);
            return;
        }

        log.info("Проверяем {} сценариев для хаба: {}", scenarios.size(), hubId);
        for (Scenario scenario : scenarios) {
            log.info("Проверяем сценарий: {} (условий: {}, действий: {})", 
                scenario.getName(), 
                scenario.getConditions() != null ? scenario.getConditions().size() : 0, 
                scenario.getActions() != null ? scenario.getActions().size() : 0);
            
            // Логируем детали условий
            if (scenario.getConditions() != null) {
                scenario.getConditions().forEach(sc -> {
                    log.debug("Условие сценария {}: sensorId={}, type={}, operation={}, value={}", 
                        scenario.getName(),
                        sc.getSensor() != null ? sc.getSensor().getId() : "null",
                        sc.getCondition() != null ? sc.getCondition().getType() : "null",
                        sc.getCondition() != null ? sc.getCondition().getOperation() : "null",
                        sc.getCondition() != null ? sc.getCondition().getValue() : "null");
                });
            }
        }

        // Проверяем каждый сценарий
        for (Scenario scenario : scenarios) {
            try {
                List<ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction> actionsToExecute = 
                    evaluationService.evaluateScenario(scenario, snapshot);

                log.info("Сценарий {} для хаба {}: найдено {} действий для выполнения", 
                    scenario.getName(), hubId, actionsToExecute.size());

                // Выполняем действия, если условия выполнены
                for (ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction action : actionsToExecute) {
                    log.info("Выполняем действие для сценария {}: sensor={}, type={}, value={}", 
                        scenario.getName(), 
                        action.getSensor().getId(), 
                        action.getAction().getType(), 
                        action.getAction().getValue());
                    hubRouterService.executeAction(scenario, action);
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке сценария {} для хаба {}: {}", 
                    scenario.getName(), hubId, e.getMessage(), e);
            }
        }
    }
}

