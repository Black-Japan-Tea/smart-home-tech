package ru.yandex.practicum.kafka.telemetry.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Slf4j
@Service
public class SnapshotAggregator {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        if (event == null || event.getHubId() == null || event.getId() == null || event.getPayload() == null) {
            log.debug("Skip event because of missing fields: {}", event);
            return Optional.empty();
        }

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(event.getHubId().toString(), this::createSnapshot);
        Map<String, SensorStateAvro> states = ensureStateMap(snapshot);

        long eventTimestamp = event.getTimestamp();
        String sensorId = event.getId().toString();
        SensorStateAvro previousState = states.get(sensorId);

        if (previousState != null) {
            long previousTimestamp = previousState.getTimestamp();
            if (eventTimestamp <= previousTimestamp) {
                return Optional.empty();
            }
            if (Objects.equals(previousState.getData(), event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
            .setTimestamp(eventTimestamp)
            .setData(event.getPayload())
            .build();

        states.put(sensorId, newState);
        snapshot.setTimestamp(eventTimestamp);

        log.debug("Snapshot for hub {} updated: {} sensors tracked", snapshot.getHubId(), states.size());
        return Optional.of(SensorsSnapshotAvro.newBuilder(snapshot).build());
    }

    private SensorsSnapshotAvro createSnapshot(String hubId) {
        return SensorsSnapshotAvro.newBuilder()
            .setHubId(hubId)
            .setTimestamp(0L)
            .setSensorsState(new HashMap<>())
            .build();
    }

    private Map<String, SensorStateAvro> ensureStateMap(SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> states = snapshot.getSensorsState();
        if (states == null) {
            states = new HashMap<>();
            snapshot.setSensorsState(states);
        }
        return states;
    }
}

