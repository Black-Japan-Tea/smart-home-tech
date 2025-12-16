package ru.yandex.practicum.kafka.telemetry.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.service.HubEventService;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final String hubsTopic;
    private final long pollTimeoutMs;
    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;

    public HubEventProcessor(KafkaConsumer<String, HubEventAvro> consumer,
                              HubEventService hubEventService,
                              AnalyzerKafkaProperties analyzerKafkaProperties) {
        this.consumer = consumer;
        this.hubEventService = hubEventService;
        this.hubsTopic = analyzerKafkaProperties.getTopics().getHubs();
        this.pollTimeoutMs = analyzerKafkaProperties.getConsumer().getPollTimeoutMs();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            log.info("Subscribing hub event processor to {}", hubsTopic);
            consumer.subscribe(Collections.singletonList(hubsTopic));

            while (running) {
                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeoutMs));
                if (!records.isEmpty()) {
                    log.debug("Hub events poll returned {} records", records.count());
                }

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    if (record.value() == null) {
                        continue;
                    }
                    try {
                        hubEventService.handle(record.value());
                    } catch (Exception processingError) {
                        log.error("Failed to process hub event {}", record.value(), processingError);
                    }
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Hub event processor loop failed", e);
        } finally {
            consumer.close();
        }
    }

    public void stop() {
        running = false;
        consumer.wakeup();
    }
}

