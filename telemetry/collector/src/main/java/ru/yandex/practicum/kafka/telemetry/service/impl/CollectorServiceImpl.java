package ru.yandex.practicum.kafka.telemetry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.dto.HubEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.mapper.AvroMapper;
import ru.yandex.practicum.kafka.telemetry.service.CollectorService;
import ru.yandex.practicum.kafka.telemetry.service.KafkaProducerService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final AvroMapper avroMapper;
    private final KafkaProducerService kafkaProducerService;

    @Override
    public void collectSensorEvent(SensorEvent event) {
        try {
            log.debug("Received sensor event: type={}, id={}, hubId={}", 
                    event != null ? event.getClass().getSimpleName() : "null",
                    event != null ? event.getId() : "null",
                    event != null ? event.getHubId() : "null");
            SensorEventAvro avroEvent = avroMapper.toAvro(event);
            log.debug("Converted to Avro: id={}, hubId={}", 
                    avroEvent != null ? avroEvent.getId() : "null",
                    avroEvent != null ? avroEvent.getHubId() : "null");
            kafkaProducerService.sendSensorEvent(avroEvent);
            log.debug("Successfully processed sensor event: {}", event != null ? event.getId() : "null");
        } catch (Exception e) {
            log.error("Error processing sensor event: type={}, id={}, hubId={}, error={}", 
                    event != null ? event.getClass().getSimpleName() : "null",
                    event != null ? event.getId() : "null",
                    event != null ? event.getHubId() : "null",
                    e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : ""), e);
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getMessage(), e.getCause());
            }
            throw e;
        }
    }

    @Override
    public void collectHubEvent(HubEvent event) {
        try {
            log.debug("Received hub event: {}", event);
            HubEventAvro avroEvent = avroMapper.toAvro(event);
            kafkaProducerService.sendHubEvent(avroEvent);
            log.debug("Successfully processed hub event: {}", event.getHubId());
        } catch (Exception e) {
            log.error("Error processing hub event: {}", event, e);
            throw e;
        }
    }
}

