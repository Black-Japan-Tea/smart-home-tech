package ru.yandex.practicum.kafka.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.config.AggregatorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
public class AggregationStarter {

    private final String sensorsTopic;
    private final String snapshotsTopic;
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    public AggregationStarter(KafkaConsumer<String, SensorEventAvro> consumer,
                              KafkaProducer<String, SensorsSnapshotAvro> producer,
                              AggregatorKafkaProperties aggregatorKafkaProperties) {
        this.consumer = consumer;
        this.producer = producer;
        this.sensorsTopic = aggregatorKafkaProperties.getTopics().getSensors();
        this.snapshotsTopic = aggregatorKafkaProperties.getTopics().getSnapshots();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
    }

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public void start() {
        try {
            log.info("Подписываемся на топик: {}", sensorsTopic);
            consumer.subscribe(Collections.singletonList(sensorsTopic));

            log.info("Начинаем обработку событий от датчиков");

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    log.debug("Получено событие: key={}, value={}", record.key(), record.value());
                    
                    Optional<SensorsSnapshotAvro> updatedSnapshot = updateState(record.value());
                    
                    if (updatedSnapshot.isPresent()) {
                        SensorsSnapshotAvro snapshot = updatedSnapshot.get();
                        log.debug("Отправляем обновленный снапшот для хаба: {}", snapshot.getHubId());
                        
                        ProducerRecord<String, SensorsSnapshotAvro> producerRecord = 
                            new ProducerRecord<>(snapshotsTopic, snapshot.getHubId(), snapshot);
                        producer.send(producerRecord);
                        log.debug("Снапшот отправлен в топик: {}", snapshotsTopic);
                    }
                }
                
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            log.info("Получен сигнал WakeupException, завершаем обработку");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Сбрасываем буфер продюсера");
                producer.flush();
                
                log.info("Фиксируем смещения консьюмера");
                consumer.commitSync();
                
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        
        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        
        if (snapshot == null) {
            log.debug("Создаем новый снапшот для хаба: {}", hubId);
            Map<String, SensorStateAvro> sensorsState = new HashMap<>();
            SensorStateAvro sensorState = createSensorState(event);
            sensorsState.put(sensorId, sensorState);
            
            snapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(event.getTimestamp())
                .setSensorsState(sensorsState)
                .build();
            
            snapshots.put(hubId, snapshot);
            return Optional.of(snapshot);
        }
        
        Map<String, SensorStateAvro> sensorsState = new HashMap<>(snapshot.getSensorsState());
        SensorStateAvro oldState = sensorsState.get(sensorId);
        
        if (oldState != null) {
            long oldTimestamp = oldState.getTimestamp();
            long eventTimestamp = event.getTimestamp();
            
            if (oldTimestamp > eventTimestamp) {
                log.debug("Игнорируем событие с более ранним timestamp для датчика: {}", sensorId);
                return Optional.empty();
            }
            
            Object oldData = oldState.getData();
            Object newData = event.getPayload();
            if (oldData != null && newData != null && oldData.equals(newData)) {
                log.debug("Данные не изменились для датчика: {}", sensorId);
                return Optional.empty();
            }
        }
        
        SensorStateAvro sensorState = createSensorState(event);
        sensorsState.put(sensorId, sensorState);
        
        snapshot = SensorsSnapshotAvro.newBuilder()
            .setHubId(hubId)
            .setTimestamp(event.getTimestamp())
            .setSensorsState(sensorsState)
            .build();
        
        snapshots.put(hubId, snapshot);
        return Optional.of(snapshot);
    }

    private SensorStateAvro createSensorState(SensorEventAvro event) {
        return SensorStateAvro.newBuilder()
            .setTimestamp(event.getTimestamp())
            .setData(event.getPayload())
            .build();
    }
}

