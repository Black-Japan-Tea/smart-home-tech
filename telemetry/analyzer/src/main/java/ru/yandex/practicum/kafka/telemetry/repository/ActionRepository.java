package ru.yandex.practicum.kafka.telemetry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.entity.Action;

public interface ActionRepository extends JpaRepository<Action, Long> {
}

