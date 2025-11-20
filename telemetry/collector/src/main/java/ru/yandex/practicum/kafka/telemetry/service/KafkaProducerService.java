package ru.yandex.practicum.kafka.telemetry.service;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public interface KafkaProducerService {
    void sendSensorEvent(SensorEventAvro event);
    void sendHubEvent(HubEventAvro event);
}