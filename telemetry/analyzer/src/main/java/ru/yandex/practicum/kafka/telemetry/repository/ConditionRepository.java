package ru.yandex.practicum.kafka.telemetry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.entity.Condition;

public interface ConditionRepository extends JpaRepository<Condition, Long> {
}

