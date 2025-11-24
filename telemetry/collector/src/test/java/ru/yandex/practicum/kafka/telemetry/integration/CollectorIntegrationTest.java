package ru.yandex.practicum.kafka.telemetry.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import ru.yandex.practicum.grpc.telemetry.DeviceTypeProto;
import ru.yandex.practicum.grpc.telemetry.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.controller.GrpcEventController;
import ru.yandex.practicum.kafka.telemetry.mapper.ProtobufMapper;
import ru.yandex.practicum.kafka.telemetry.service.CollectorService;

/**
 * Интеграционные тесты для проверки обработки событий с UNSPECIFIED значениями.
 * Эти тесты воссоздают проблему, когда Hub Router отправляет события с UNSPECIFIED,
 * и мы можем увидеть логи ошибок для анализа.
 */
@ExtendWith(MockitoExtension.class)
class CollectorIntegrationTest {

    @Mock
    private ProtobufMapper protobufMapper;

    @Mock
    private CollectorService collectorService;

    @Mock
    private StreamObserver<Empty> responseObserver;

    private GrpcEventController controller;

    @BeforeEach
    void setUp() {
        controller = new GrpcEventController(protobufMapper, collectorService);
    }

    /**
     * Тест воссоздает ситуацию, когда Hub Router отправляет DEVICE_ADDED с UNSPECIFIED типом.
     * Ожидается, что событие будет обработано с использованием значения по умолчанию (TEMPERATURE_SENSOR).
     */
    @Test
    void shouldProcessDeviceAddedWithUnspecifiedTypeUsingDefault() {
        // Создаем событие с UNSPECIFIED типом (как отправляет Hub Router)
        HubEventProto request = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(Instant.now()))
            .setDeviceAdded(ru.yandex.practicum.grpc.telemetry.DeviceAddedEventProto.newBuilder()
                .setId("device-1")
                .setType(DeviceTypeProto.DEVICE_TYPE_UNSPECIFIED) // Проблемное значение
                .build())
            .build();

        // Вызываем gRPC метод
        controller.collectHubEvent(request, responseObserver);

        // Проверяем, что событие БЫЛО обработано (вызван collectorService)
        verify(collectorService).collectHubEvent(any());
        
        // Проверяем, что ответ был успешным (не было ошибки)
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    /**
     * Тест воссоздает ситуацию, когда Hub Router отправляет SCENARIO_ADDED с UNSPECIFIED значениями.
     * Ожидается, что событие будет обработано с использованием значений по умолчанию.
     */
    @Test
    void shouldProcessScenarioAddedWithUnspecifiedConditionTypeUsingDefault() {
        // Создаем событие с UNSPECIFIED в условии (как отправляет Hub Router)
        HubEventProto request = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(Instant.now()))
            .setScenarioAdded(ru.yandex.practicum.grpc.telemetry.ScenarioAddedEventProto.newBuilder()
                .setName("test-scenario")
                .addCondition(ru.yandex.practicum.grpc.telemetry.ScenarioConditionProto.newBuilder()
                    .setSensorId("sensor-1")
                    .setType(ru.yandex.practicum.grpc.telemetry.ConditionTypeProto.CONDITION_TYPE_UNSPECIFIED) // Проблемное значение
                    .setOperation(ru.yandex.practicum.grpc.telemetry.ConditionOperationProto.EQUALS)
                    .setIntValue(1)
                    .build())
                .build())
            .build();

        // Вызываем gRPC метод
        controller.collectHubEvent(request, responseObserver);

        // Проверяем, что событие БЫЛО обработано
        verify(collectorService).collectHubEvent(any());
        
        // Проверяем, что ответ был успешным
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    /**
     * Тест воссоздает ситуацию, когда Hub Router отправляет SCENARIO_ADDED с UNSPECIFIED в действии.
     * Ожидается, что событие будет обработано с использованием значения по умолчанию (ACTIVATE).
     */
    @Test
    void shouldProcessScenarioAddedWithUnspecifiedActionTypeUsingDefault() {
        HubEventProto request = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(Instant.now()))
            .setScenarioAdded(ru.yandex.practicum.grpc.telemetry.ScenarioAddedEventProto.newBuilder()
                .setName("test-scenario")
                .addAction(ru.yandex.practicum.grpc.telemetry.DeviceActionProto.newBuilder()
                    .setSensorId("sensor-1")
                    .setType(ru.yandex.practicum.grpc.telemetry.ActionTypeProto.ACTION_TYPE_UNSPECIFIED) // Проблемное значение
                    .setValue(1)
                    .build())
                .build())
            .build();

        controller.collectHubEvent(request, responseObserver);

        // Проверяем, что событие БЫЛО обработано
        verify(collectorService).collectHubEvent(any());
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    /**
     * Тест проверяет, что валидные события обрабатываются нормально.
     */
    @Test
    void shouldProcessValidDeviceAddedEvent() {
        HubEventProto request = HubEventProto.newBuilder()
            .setHubId("hub-1")
            .setTimestamp(toTimestamp(Instant.now()))
            .setDeviceAdded(ru.yandex.practicum.grpc.telemetry.DeviceAddedEventProto.newBuilder()
                .setId("device-1")
                .setType(DeviceTypeProto.TEMPERATURE_SENSOR) // Валидное значение
                .build())
            .build();

        controller.collectHubEvent(request, responseObserver);

        // Проверяем, что событие было обработано
        verify(collectorService).collectHubEvent(any());
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}

