package ru.yandex.practicum.kafka.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@ExtendWith(MockitoExtension.class)
class AggregationStarterTest {

    @Mock
    private KafkaConsumer<String, SensorEventAvro> consumer;

    @Mock
    private KafkaProducer<String, SensorsSnapshotAvro> producer;

    private AggregationStarter aggregationStarter;

    @BeforeEach
    void setUp() {
        aggregationStarter = new AggregationStarter(consumer, producer);
    }

    @Test
    void shouldCreateSnapshotWhenNoneExists() {
        SensorEventAvro event = temperatureEvent("hub-1", "sensor-1", 10L, 25);

        SensorsSnapshotAvro snapshot = aggregationStarter.updateState(event).orElseThrow();

        assertThat(snapshot.getHubId()).isEqualTo("hub-1");
        assertThat(snapshot.getSensorsState()).containsOnlyKeys("sensor-1");
        assertThat(snapshot.getSensorsState().get("sensor-1").getData())
            .isEqualTo(event.getPayload());
    }

    @Test
    void shouldIgnoreOlderEvent() {
        SensorEventAvro latest = temperatureEvent("hub-1", "sensor-1", 20L, 26);
        SensorEventAvro older = temperatureEvent("hub-1", "sensor-1", 10L, 24);
        aggregationStarter.updateState(latest);

        Optional<SensorsSnapshotAvro> result = aggregationStarter.updateState(older);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIgnoreUnchangedPayload() {
        SensorEventAvro event = temperatureEvent("hub-1", "sensor-1", 20L, 26);
        aggregationStarter.updateState(event);

        Optional<SensorsSnapshotAvro> result = aggregationStarter.updateState(event);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMergeMultipleSensorsIntoSnapshot() {
        SensorEventAvro sensorOne = temperatureEvent("hub-1", "sensor-1", 20L, 26);
        SensorEventAvro sensorTwo = temperatureEvent("hub-1", "sensor-2", 25L, 18);
        aggregationStarter.updateState(sensorOne);

        SensorsSnapshotAvro updatedSnapshot = aggregationStarter.updateState(sensorTwo).orElseThrow();

        assertThat(updatedSnapshot.getSensorsState())
            .hasSize(2)
            .containsKeys("sensor-1", "sensor-2");
        assertThat(updatedSnapshot.getSensorsState().get("sensor-2").getData())
            .isEqualTo(sensorTwo.getPayload());
    }

    private SensorEventAvro temperatureEvent(String hubId, String sensorId, long timestamp, int temperatureC) {
        TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
            .setTemperatureC(temperatureC)
            .setTemperatureF(temperatureC * 2)
            .build();

        return SensorEventAvro.newBuilder()
            .setHubId(hubId)
            .setId(sensorId)
            .setTimestamp(timestamp)
            .setPayload(payload)
            .build();
    }
}

