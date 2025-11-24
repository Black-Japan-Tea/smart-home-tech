package ru.yandex.practicum.kafka.telemetry.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.repository.ScenarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    @Transactional(readOnly = true)
    public List<Scenario> findByHubId(String hubId) {
        return scenarioRepository.findByHubId(hubId);
    }
}

