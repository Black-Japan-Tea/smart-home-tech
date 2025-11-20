package ru.yandex.practicum.kafka.telemetry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.kafka.telemetry.dto.HubEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.service.CollectorService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final CollectorService collectorService;

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.OK)
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        try {
            collectorService.collectSensorEvent(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process sensor event", e);
        }
    }

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.OK)
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        try {
            collectorService.collectHubEvent(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process hub event", e);
        }
    }
}

