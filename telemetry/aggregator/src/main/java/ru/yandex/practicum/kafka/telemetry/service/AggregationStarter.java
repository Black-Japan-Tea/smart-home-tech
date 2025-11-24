package ru.yandex.practicum.kafka.telemetry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
@RequiredArgsConstructor
public class AggregationStarter {

    @Value("${kafka.topics.sensors:telemetry.sensors.v1}")
    private String sensorsTopic;

    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    private String snapshotsTopic;

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    // Хранилище снапшотов по hubId
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топики для получения событий от датчиков,
     * формирует снимок их состояния и записывает в кафку.
     */
    public void start() {
        try {
            log.info("Подписываемся на топик: {}", sensorsTopic);
            consumer.subscribe(Collections.singletonList(sensorsTopic));

            log.info("Начинаем обработку событий от датчиков");

            // Цикл обработки событий
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
                
                // Фиксируем смещения после обработки батча
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
            log.info("Получен сигнал WakeupException, завершаем обработку");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедиться,
                // что все сообщения, лежащие в буффере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы
                
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

    /**
     * Обновляет состояние снапшота на основе полученного события.
     * 
     * @param event событие от датчика
     * @return Optional с обновленным снапшотом, если состояние изменилось, иначе empty
     */
    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        
        // Проверяем, есть ли снапшот для event.getHubId()
        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        
        if (snapshot == null) {
            // Если снапшота нет, создаем новый
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
        
        // Если снапшот есть, проверяем данные для event.getId()
        Map<String, SensorStateAvro> sensorsState = new HashMap<>(snapshot.getSensorsState());
        SensorStateAvro oldState = sensorsState.get(sensorId);
        
        if (oldState != null) {
            // Если данные есть, проверяем, нужно ли обновлять
            long oldTimestamp = oldState.getTimestamp();
            long eventTimestamp = event.getTimestamp();
            
            // Если старое событие произошло позже, игнорируем новое
            if (oldTimestamp > eventTimestamp) {
                log.debug("Игнорируем событие с более ранним timestamp для датчика: {}", sensorId);
                return Optional.empty();
            }
            
            // Проверяем, изменились ли данные
            // Если timestamp одинаковый и данные одинаковые, не обновляем
            Object oldData = oldState.getData();
            Object newData = event.getPayload();
            if (oldData != null && newData != null && oldData.equals(newData)) {
                log.debug("Данные не изменились для датчика: {}", sensorId);
                return Optional.empty();
            }
        }
        
        // Если дошли до сюда, значит пришли новые данные и снапшот нужно обновить
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

    /**
     * Создает SensorStateAvro на основе события от датчика.
     */
    private SensorStateAvro createSensorState(SensorEventAvro event) {
        return SensorStateAvro.newBuilder()
            .setTimestamp(event.getTimestamp())
            .setData(event.getPayload())
            .build();
    }
}

