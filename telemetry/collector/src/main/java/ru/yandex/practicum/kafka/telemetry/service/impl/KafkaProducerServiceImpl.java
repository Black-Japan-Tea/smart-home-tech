package ru.yandex.practicum.kafka.telemetry.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.config.CollectorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.service.KafkaProducerService;

import java.util.concurrent.Future;

@Slf4j
@Service
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final String sensorsTopic;
    private final String hubsTopic;
    private final KafkaProducer<String, SensorEventAvro> sensorEventKafkaProducer;
    private final KafkaProducer<String, HubEventAvro> hubEventKafkaProducer;

    public KafkaProducerServiceImpl(KafkaProducer<String, SensorEventAvro> sensorEventKafkaProducer,
                                     KafkaProducer<String, HubEventAvro> hubEventKafkaProducer,
                                     CollectorKafkaProperties collectorKafkaProperties) {
        this.sensorEventKafkaProducer = sensorEventKafkaProducer;
        this.hubEventKafkaProducer = hubEventKafkaProducer;
        this.sensorsTopic = collectorKafkaProperties.getTopics().getSensors();
        this.hubsTopic = collectorKafkaProperties.getTopics().getHubs();
    }

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

