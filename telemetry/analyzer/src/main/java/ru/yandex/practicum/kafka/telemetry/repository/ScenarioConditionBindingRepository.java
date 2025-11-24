package ru.yandex.practicum.kafka.telemetry.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioConditionBinding;
import ru.yandex.practicum.kafka.telemetry.entity.id.ScenarioConditionKey;

import java.util.List;

public interface ScenarioConditionBindingRepository extends JpaRepository<ScenarioConditionBinding, ScenarioConditionKey> {

    @EntityGraph(attributePaths = {"condition", "sensor"})
    List<ScenarioConditionBinding> findByScenario_Id(Long scenarioId);

    @EntityGraph(attributePaths = {"condition"})
    List<ScenarioConditionBinding> findBySensor_Id(String sensorId);
}

