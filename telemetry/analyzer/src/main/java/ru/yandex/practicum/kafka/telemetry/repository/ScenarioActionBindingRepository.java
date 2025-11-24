package ru.yandex.practicum.kafka.telemetry.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioActionBinding;
import ru.yandex.practicum.kafka.telemetry.entity.id.ScenarioActionKey;

import java.util.List;

public interface ScenarioActionBindingRepository extends JpaRepository<ScenarioActionBinding, ScenarioActionKey> {

    @EntityGraph(attributePaths = {"action", "sensor"})
    List<ScenarioActionBinding> findByScenario_Id(Long scenarioId);

    @EntityGraph(attributePaths = {"action"})
    List<ScenarioActionBinding> findBySensor_Id(String sensorId);
}

