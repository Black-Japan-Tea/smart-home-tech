package ru.yandex.practicum.kafka.telemetry.service;

import ru.yandex.practicum.kafka.telemetry.dto.HubEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SensorEvent;

public interface CollectorService {
    void collectSensorEvent(SensorEvent event);
    void collectHubEvent(HubEvent event);
}

