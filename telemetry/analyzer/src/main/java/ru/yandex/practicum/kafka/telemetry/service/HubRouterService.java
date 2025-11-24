package ru.yandex.practicum.kafka.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction;

/**
 * Сервис для логирования действий, которые должны быть отправлены устройствам.
 * Hub Router модуль удален, поэтому действия только логируются.
 */
@Slf4j
@Service
public class HubRouterService {

    /**
     * Логирует действие, которое должно быть выполнено устройством.
     */
    public void executeAction(Scenario scenario, ScenarioAction scenarioAction) {
        try {
            Action action = scenarioAction.getAction();
            String sensorId = scenarioAction.getSensor().getId();
            
            log.info("Действие для выполнения (Hub Router удален, только логирование): " +
                    "hubId={}, scenario={}, sensorId={}, type={}, value={}",
                scenario.getHubId(), scenario.getName(), sensorId, action.getType(), action.getValue());
        } catch (Exception e) {
            log.error("Ошибка при логировании действия для сценария {}: {}", 
                scenario.getName(), e.getMessage(), e);
        }
    }
}

