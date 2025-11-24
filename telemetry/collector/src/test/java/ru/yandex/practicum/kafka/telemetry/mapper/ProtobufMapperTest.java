package ru.yandex.practicum.kafka.telemetry.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Timestamp;

import ru.yandex.practicum.grpc.telemetry.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.ConditionOperationProto;
import ru.yandex.practicum.grpc.telemetry.ConditionTypeProto;
import ru.yandex.practicum.grpc.telemetry.DeviceTypeProto;
import ru.yandex.practicum.grpc.telemetry.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.dto.ClimateSensorEvent;
import ru.yandex.practicum.kafka.telemetry.dto.DeviceAddedEvent;
import ru.yandex.practicum.kafka.telemetry.dto.DeviceType;
import ru.yandex.practicum.kafka.telemetry.dto.HubEvent;
import ru.yandex.practicum.kafka.telemetry.dto.MotionSensorEvent;
import ru.yandex.practicum.kafka.telemetry.dto.ScenarioAddedEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.dto.SwitchSensorEvent;
import ru.yandex.practicum.kafka.telemetry.dto.TemperatureSensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@ExtendWith(MockitoExtension.class)
class ProtobufMapperTest {

    @Mock
    private AvroMapper avroMapper;

    private ProtobufMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProtobufMapper(avroMapper);
    }

    @Test
    void shouldMapMotionSensorEvent() {
        Instant now = Instant.now();
        SensorEventProto proto = SensorEventProto.newBuilder()
            .setId("sensor-1")
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setMotionSensor(ru.yandex.practicum.grpc.telemetry.MotionSensorProto.newBuilder()
                .setMotion(true)
                .setVoltage(3)
                .setLinkQuality(85)
                .build())
            .build();

        SensorEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(MotionSensorEvent.class);
        assertThat(dto.getId()).isEqualTo("sensor-1");
        assertThat(dto.getHubId()).isEqualTo("hub-1");
        assertThat(dto.getTimestamp()).isEqualTo(now);
        
        MotionSensorEvent motionEvent = (MotionSensorEvent) dto;
        assertThat(motionEvent.getMotion()).isTrue();
        assertThat(motionEvent.getVoltage()).isEqualTo(3);
        assertThat(motionEvent.getLinkQuality()).isEqualTo(85);
    }

    @Test
    void shouldMapTemperatureSensorEvent() {
        Instant now = Instant.now();
        SensorEventProto proto = SensorEventProto.newBuilder()
            .setId("temp-1")
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setTemperatureSensor(ru.yandex.practicum.grpc.telemetry.TemperatureSensorProto.newBuilder()
                .setTemperatureC(25)
                .setTemperatureF(77)
                .build())
            .build();

        SensorEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(TemperatureSensorEvent.class);
        TemperatureSensorEvent tempEvent = (TemperatureSensorEvent) dto;
        assertThat(tempEvent.getTemperatureC()).isEqualTo(25);
        assertThat(tempEvent.getTemperatureF()).isEqualTo(77);
    }

    @Test
    void shouldMapClimateSensorEvent() {
        Instant now = Instant.now();
        SensorEventProto proto = SensorEventProto.newBuilder()
            .setId("climate-1")
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setClimateSensor(ru.yandex.practicum.grpc.telemetry.ClimateSensorProto.newBuilder()
                .setTemperatureC(22)
                .setHumidity(60)
                .setCo2Level(450)
                .build())
            .build();

        SensorEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(ClimateSensorEvent.class);
        ClimateSensorEvent climateEvent = (ClimateSensorEvent) dto;
        assertThat(climateEvent.getTemperatureC()).isEqualTo(22);
        assertThat(climateEvent.getHumidity()).isEqualTo(60);
        assertThat(climateEvent.getCo2Level()).isEqualTo(450);
    }

    @Test
    void shouldMapSwitchSensorEvent() {
        Instant now = Instant.now();
        SensorEventProto proto = SensorEventProto.newBuilder()
            .setId("switch-1")
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setSwitchSensor(ru.yandex.practicum.grpc.telemetry.SwitchSensorProto.newBuilder()
                .setState(true)
                .build())
            .build();

        SensorEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(SwitchSensorEvent.class);
        SwitchSensorEvent switchEvent = (SwitchSensorEvent) dto;
        assertThat(switchEvent.getState()).isTrue();
    }

    @Test
    void shouldMapDeviceAddedEvent() {
        Instant now = Instant.now();
        HubEventProto proto = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setDeviceAdded(ru.yandex.practicum.grpc.telemetry.DeviceAddedEventProto.newBuilder()
                .setId("device-1")
                .setType(DeviceTypeProto.TEMPERATURE_SENSOR)
                .build())
            .build();

        HubEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(DeviceAddedEvent.class);
        assertThat(dto.getHubId()).isEqualTo("hub-1");
        assertThat(dto.getTimestamp()).isEqualTo(now);
        
        DeviceAddedEvent deviceEvent = (DeviceAddedEvent) dto;
        assertThat(deviceEvent.getId()).isEqualTo("device-1");
        assertThat(deviceEvent.getDeviceType()).isEqualTo(DeviceType.TEMPERATURE_SENSOR);
    }

    @Test
    void shouldMapScenarioAddedEvent() {
        Instant now = Instant.now();
        HubEventProto proto = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setScenarioAdded(ru.yandex.practicum.grpc.telemetry.ScenarioAddedEventProto.newBuilder()
                .setName("test-scenario")
                .addCondition(ru.yandex.practicum.grpc.telemetry.ScenarioConditionProto.newBuilder()
                    .setSensorId("sensor-1")
                    .setType(ConditionTypeProto.TEMPERATURE)
                    .setOperation(ConditionOperationProto.GREATER_THAN)
                    .setIntValue(20)
                    .build())
                .addAction(ru.yandex.practicum.grpc.telemetry.DeviceActionProto.newBuilder()
                    .setSensorId("sensor-2")
                    .setType(ActionTypeProto.ACTIVATE)
                    .setValue(1)
                    .build())
                .build())
            .build();

        HubEvent dto = mapper.toDto(proto);

        assertThat(dto).isInstanceOf(ScenarioAddedEvent.class);
        ScenarioAddedEvent scenarioEvent = (ScenarioAddedEvent) dto;
        assertThat(scenarioEvent.getName()).isEqualTo("test-scenario");
        assertThat(scenarioEvent.getConditions()).hasSize(1);
        assertThat(scenarioEvent.getConditions().get(0).getSensorId()).isEqualTo("sensor-1");
        assertThat(scenarioEvent.getConditions().get(0).getType().toString()).isEqualTo("TEMPERATURE");
        assertThat(scenarioEvent.getConditions().get(0).getOperation().toString()).isEqualTo("GREATER_THAN");
        assertThat(scenarioEvent.getConditions().get(0).getValue()).isEqualTo(20);
        assertThat(scenarioEvent.getActions()).hasSize(1);
        assertThat(scenarioEvent.getActions().get(0).getSensorId()).isEqualTo("sensor-2");
        assertThat(scenarioEvent.getActions().get(0).getType().toString()).isEqualTo("ACTIVATE");
        assertThat(scenarioEvent.getActions().get(0).getValue()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionForUnspecifiedDeviceType() {
        Instant now = Instant.now();
        HubEventProto proto = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setDeviceAdded(ru.yandex.practicum.grpc.telemetry.DeviceAddedEventProto.newBuilder()
                .setId("device-1")
                .setType(DeviceTypeProto.DEVICE_TYPE_UNSPECIFIED)
                .build())
            .build();

        assertThatThrownBy(() -> mapper.toDto(proto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unspecified");
    }

    @Test
    void shouldThrowExceptionForUnspecifiedConditionType() {
        Instant now = Instant.now();
        HubEventProto proto = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(now))
            .setScenarioAdded(ru.yandex.practicum.grpc.telemetry.ScenarioAddedEventProto.newBuilder()
                .setName("test")
                .addCondition(ru.yandex.practicum.grpc.telemetry.ScenarioConditionProto.newBuilder()
                    .setSensorId("sensor-1")
                    .setType(ConditionTypeProto.CONDITION_TYPE_UNSPECIFIED)
                    .setOperation(ConditionOperationProto.EQUALS)
                    .setIntValue(1)
                    .build())
                .build())
            .build();

        assertThatThrownBy(() -> mapper.toDto(proto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unspecified");
    }

    @Test
    void shouldMapToAvro() {
        MotionSensorEvent dto = new MotionSensorEvent();
        dto.setId("sensor-1");
        dto.setHubId("hub-1");
        dto.setTimestamp(Instant.now());
        dto.setMotion(true);
        dto.setVoltage(3);
        dto.setLinkQuality(85);
        
        SensorEventAvro avro = SensorEventAvro.newBuilder()
            .setId("sensor-1")
            .setHubId("hub-1")
            .setTimestamp(100L)
            .setPayload(MotionSensorAvro.newBuilder()
                .setMotion(true)
                .setVoltage(3)
                .setLinkQuality(85)
                .build())
            .build();
        
        when(avroMapper.toAvro(any(SensorEvent.class))).thenReturn(avro);
        
        SensorEventAvro result = mapper.toAvro(dto);
        
        assertThat(result).isEqualTo(avro);
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}

