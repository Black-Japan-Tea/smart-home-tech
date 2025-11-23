package ru.yandex.practicum.kafka.telemetry.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.config.AggregatorProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final SnapshotAggregator snapshotAggregator;
    private final AggregatorProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public void start() {
        Duration pollTimeout = resolvePollTimeout();

        try {
            consumer.subscribe(Collections.singletonList(properties.getTopics().getSensorsTopic()));
            log.info("Subscribed to topic {}", properties.getTopics().getSensorsTopic());

            while (running.get()) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(pollTimeout);
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    Optional<SensorsSnapshotAvro> snapshotOpt = snapshotAggregator.updateState(event);
                    snapshotOpt.ifPresent(this::publishSnapshot);
                }

                producer.flush();
                consumer.commitAsync();
            }
        } catch (WakeupException wakeupException) {
            if (running.get()) {
                log.error("WakeupException while running poll loop", wakeupException);
            } else {
                log.info("Poll loop interrupted because shutdown was requested");
            }
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
            } catch (Exception e) {
                log.warn("Unable to flush producer buffer before shutdown", e);
            }
            try {
                consumer.commitSync();
            } catch (Exception e) {
                log.warn("Unable to commit offsets before shutdown", e);
            }

            log.info("Закрываем консьюмер");
            consumer.close();
            log.info("Закрываем продюсер");
            producer.close();
        }
    }

    private void publishSnapshot(SensorsSnapshotAvro snapshot) {
        ProducerRecord<String, SensorsSnapshotAvro> record = new ProducerRecord<>(
            properties.getTopics().getSnapshotsTopic(),
            snapshot.getHubId().toString(),
            snapshot
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to write snapshot for hub {}", snapshot.getHubId(), exception);
            } else {
                log.debug("Snapshot for hub {} sent to partition {} offset {}",
                    snapshot.getHubId(), metadata.partition(), metadata.offset());
            }
        });
    }

    private Duration resolvePollTimeout() {
        Duration pollTimeout = properties.getConsumer().getPollTimeout();
        if (pollTimeout == null || pollTimeout.isNegative() || pollTimeout.isZero()) {
            pollTimeout = Duration.ofSeconds(1);
        }
        return pollTimeout;
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        consumer.wakeup();
    }
}

