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
import ru.yandex.practicum.kafka.telemetry.entity.Sensor;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.mapper.HubEventMapper;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.repository.SensorRepository;

import java.time.Duration;
import java.util.Collections;

/**
 * Процессор для обработки событий добавления/удаления устройств и сценариев.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    @Value("${kafka.topics.hubs:telemetry.hubs.v1}")
    private String hubsTopic;

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventMapper hubEventMapper;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;

    @Override
    public void run() {
        try {
            log.info("Подписываемся на топик: {}", hubsTopic);
            consumer.subscribe(Collections.singletonList(hubsTopic));

            log.info("Начинаем обработку событий от хабов");

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    log.debug("Получено событие хаба: key={}, value={}", record.key(), record.value());
                    processHubEvent(record.value());
                }

                // Фиксируем смещения после обработки батча
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            log.info("Получен сигнал WakeupException, завершаем обработку событий хабов");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хабов", e);
        } finally {
            try {
                log.info("Фиксируем смещения консьюмера событий хабов");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер событий хабов");
                consumer.close();
            }
        }
    }

    private void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(hubId, deviceAdded);
        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(hubId, deviceRemoved);
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(hubId, scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(hubId, scenarioRemoved);
        } else {
            log.warn("Unknown hub event payload type: {}", payload.getClass());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        try {
            Sensor sensor = hubEventMapper.toSensor(hubId, event);
            sensorRepository.save(sensor);
            log.info("Добавлено устройство: hubId={}, deviceId={}, type={}", 
                hubId, event.getId(), event.getType());
        } catch (Exception e) {
            log.error("Ошибка при добавлении устройства: hubId={}, deviceId={}", 
                hubId, event.getId(), e);
        }
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        try {
            sensorRepository.deleteById(event.getId());
            log.info("Удалено устройство: hubId={}, deviceId={}", hubId, event.getId());
        } catch (Exception e) {
            log.error("Ошибка при удалении устройства: hubId={}, deviceId={}", 
                hubId, event.getId(), e);
        }
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        try {
            // Удаляем существующий сценарий с таким же именем, если есть
            scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(scenarioRepository::delete);

            Scenario scenario = hubEventMapper.toScenario(hubId, event);
            scenarioRepository.save(scenario);
            log.info("Добавлен сценарий: hubId={}, name={}", hubId, event.getName());
        } catch (Exception e) {
            log.error("Ошибка при добавлении сценария: hubId={}, name={}", 
                hubId, event.getName(), e);
        }
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        try {
            scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(scenarioRepository::delete);
            log.info("Удален сценарий: hubId={}, name={}", hubId, event.getName());
        } catch (Exception e) {
            log.error("Ошибка при удалении сценария: hubId={}, name={}", 
                hubId, event.getName(), e);
        }
    }
}

