package ru.yandex.practicum.kafka.telemetry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.service.KafkaProducerService;

import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    @Value("${kafka.topics.sensors:telemetry.sensors.v1}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs:telemetry.hubs.v1}")
    private String hubsTopic;

    private final KafkaProducer<String, SensorEventAvro> sensorEventKafkaProducer;
    private final KafkaProducer<String, HubEventAvro> hubEventKafkaProducer;

    @Override
    public void sendSensorEvent(SensorEventAvro event) {
        try {
            log.debug("Sending sensor event to topic {}: {}", sensorsTopic, event);
            ProducerRecord<String, SensorEventAvro> record = 
                new ProducerRecord<>(sensorsTopic, event.getId(), event);
            Future<RecordMetadata> future = sensorEventKafkaProducer.send(record);
            RecordMetadata metadata = future.get();
            log.debug("Successfully sent sensor event to topic {} at offset {}", 
                sensorsTopic, metadata.offset());
        } catch (Exception e) {
            log.error("Error sending sensor event to topic {}: {}", sensorsTopic, event, e);
            throw new RuntimeException("Failed to send sensor event to Kafka", e);
        }
    }

    @Override
    public void sendHubEvent(HubEventAvro event) {
        try {
            log.debug("Sending hub event to topic {}: {}", hubsTopic, event);
            ProducerRecord<String, HubEventAvro> record = 
                new ProducerRecord<>(hubsTopic, event.getHubId(), event);
            Future<RecordMetadata> future = hubEventKafkaProducer.send(record);
            RecordMetadata metadata = future.get();
            log.debug("Successfully sent hub event to topic {} at offset {}", 
                hubsTopic, metadata.offset());
        } catch (Exception e) {
            log.error("Error sending hub event to topic {}: {}", hubsTopic, event, e);
            throw new RuntimeException("Failed to send hub event to Kafka", e);
        }
    }
}

