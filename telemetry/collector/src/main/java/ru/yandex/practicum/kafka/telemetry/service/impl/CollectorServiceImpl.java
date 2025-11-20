package ru.yandex.practicum.kafka.telemetry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.dto.HubEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SensorEvent;
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
            log.debug("Received sensor event: {}", event);
            var avroEvent = avroMapper.toAvro(event);
            kafkaProducerService.sendSensorEvent(avroEvent);
            log.debug("Successfully processed sensor event: {}", event.getId());
        } catch (Exception e) {
            log.error("Error processing sensor event: {}", event, e);
            throw e;
        }
    }

    @Override
    public void collectHubEvent(HubEvent event) {
        try {
            log.debug("Received hub event: {}", event);
            var avroEvent = avroMapper.toAvro(event);
            kafkaProducerService.sendHubEvent(avroEvent);
            log.debug("Successfully processed hub event: {}", event.getHubId());
        } catch (Exception e) {
            log.error("Error processing hub event: {}", event, e);
            throw e;
        }
    }
}

