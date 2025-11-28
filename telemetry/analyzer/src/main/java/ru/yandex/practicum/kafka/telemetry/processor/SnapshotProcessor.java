package ru.yandex.practicum.kafka.telemetry.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.service.DeviceActionDispatcher;
import ru.yandex.practicum.kafka.telemetry.service.ScenarioEvaluationService;
import ru.yandex.practicum.kafka.telemetry.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SnapshotProcessor {

    private final String snapshotsTopic;
    private final long pollTimeoutMs;
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioService scenarioService;
    private final ScenarioEvaluationService evaluationService;
    private final DeviceActionDispatcher dispatcher;

    public SnapshotProcessor(KafkaConsumer<String, SensorsSnapshotAvro> consumer,
                             ScenarioService scenarioService,
                             ScenarioEvaluationService evaluationService,
                             DeviceActionDispatcher dispatcher,
                             AnalyzerKafkaProperties analyzerKafkaProperties) {
        this.consumer = consumer;
        this.scenarioService = scenarioService;
        this.evaluationService = evaluationService;
        this.dispatcher = dispatcher;
        this.snapshotsTopic = analyzerKafkaProperties.getTopics().getSnapshots();
        this.pollTimeoutMs = analyzerKafkaProperties.getConsumer().getPollTimeoutMs();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    private volatile boolean running = true;

    public void start() {
        try {
            log.info("Subscribing to topic {}", snapshotsTopic);
            consumer.subscribe(Collections.singletonList(snapshotsTopic));

            while (running) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeoutMs));
                if (!records.isEmpty()) {
                    log.debug("Snapshots poll returned {} records", records.count());
                }
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();
                    if (snapshot == null) {
                        continue;
                    }
                    handleSnapshot(snapshot);
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Snapshot processing loop failed", e);
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception ignored) {
            }
            consumer.close();
        }
    }

    public void stop() {
        running = false;
        consumer.wakeup();
    }

    private void handleSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        int sensorStates = snapshot.getSensorsState() != null ? snapshot.getSensorsState().size() : 0;
        log.info("Processing snapshot for hub {} with {} sensor states (ts={})", hubId, sensorStates, snapshot.getTimestamp());
        List<Scenario> scenarios = scenarioService.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.debug("No scenarios configured for hub {}", hubId);
            return;
        }

        List<DeviceActionRequest> requests = evaluationService.evaluate(snapshot, scenarios);
        if (requests.isEmpty()) {
            log.debug("Snapshot for hub {} did not trigger any scenario ({} scenarios loaded)", hubId, scenarios.size());
            return;
        }

        log.info("Snapshot for hub {} triggered {} actions across {} scenarios", hubId, requests.size(), scenarios.size());
        requests.forEach(dispatcher::dispatch);
    }
}

