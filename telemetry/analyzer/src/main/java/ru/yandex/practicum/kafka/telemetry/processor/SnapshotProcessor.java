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

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.debug("Получен снапшот: key={}, hubId={}", record.key(), record.value().getHubId());
                    processSnapshot(record.value());
                }

                // Фиксируем смещения после обработки батча
                consumer.commitSync();
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

        // Загружаем все сценарии для данного хаба
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба: {}", hubId);
            return;
        }

        log.debug("Проверяем {} сценариев для хаба: {}", scenarios.size(), hubId);

        // Проверяем каждый сценарий
        for (Scenario scenario : scenarios) {
            try {
                List<ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction> actionsToExecute = 
                    evaluationService.evaluateScenario(scenario, snapshot);

                // Выполняем действия, если условия выполнены
                for (ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction action : actionsToExecute) {
                    hubRouterService.executeAction(scenario, action);
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке сценария {} для хаба {}: {}", 
                    scenario.getName(), hubId, e.getMessage(), e);
            }
        }
    }
}

