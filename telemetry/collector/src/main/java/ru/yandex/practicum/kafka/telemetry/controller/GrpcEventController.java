package ru.yandex.practicum.kafka.telemetry.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.service.collector.CollectorControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.mapper.ProtobufMapper;
import ru.yandex.practicum.kafka.telemetry.service.CollectorService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcEventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final ProtobufMapper protobufMapper;
    private final CollectorService collectorService;

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received sensor event via gRPC: {}", request.getId());
            var dto = protobufMapper.toDto(request);
            collectorService.collectSensorEvent(dto);
            
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("Successfully processed sensor event: {}", request.getId());
        } catch (Exception e) {
            log.error("Error processing sensor event: {}", request.getId(), e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received hub event via gRPC: hubId={}, payloadCase={}", 
                request.getHubId(), request.getPayloadCase());
            
            // Логируем детали события для диагностики
            if (request.hasDeviceAdded()) {
                var deviceAdded = request.getDeviceAdded();
                log.info("DeviceAdded: hubId={}, id={}, type={} (number={})", 
                    request.getHubId(), deviceAdded.getId(), deviceAdded.getType(), deviceAdded.getType().getNumber());
                
                // UNSPECIFIED теперь обрабатывается в ProtobufMapper с использованием значений по умолчанию
                // Просто логируем предупреждение
                if (deviceAdded.getType() == ru.yandex.practicum.grpc.telemetry.DeviceTypeProto.DEVICE_TYPE_UNSPECIFIED) {
                    log.warn("Получено событие DEVICE_ADDED с UNSPECIFIED типом: hubId={}, deviceId={}. " +
                        "Будет использован тип по умолчанию (TEMPERATURE_SENSOR). " +
                        "Hub Router должен устанавливать enum значения явно.", 
                        request.getHubId(), deviceAdded.getId());
                }
            } else if (request.hasScenarioAdded()) {
                var scenarioAdded = request.getScenarioAdded();
                log.info("ScenarioAdded: hubId={}, name={}, conditions={}, actions={}", 
                    request.getHubId(), scenarioAdded.getName(), scenarioAdded.getConditionCount(), scenarioAdded.getActionCount());
                
                // Логируем предупреждения о UNSPECIFIED, но не пропускаем событие
                // UNSPECIFIED теперь обрабатывается в ProtobufMapper с использованием значений по умолчанию
                int conditionIndex = 0;
                for (var condition : scenarioAdded.getConditionList()) {
                    if (condition.getType() == ru.yandex.practicum.grpc.telemetry.ConditionTypeProto.CONDITION_TYPE_UNSPECIFIED ||
                        condition.getOperation() == ru.yandex.practicum.grpc.telemetry.ConditionOperationProto.CONDITION_OPERATION_UNSPECIFIED) {
                        log.warn("Найдено UNSPECIFIED в условии #{} сценария: hubId={}, name={}, sensorId={}, type={}, operation={}. " +
                            "Будут использованы значения по умолчанию.", 
                            conditionIndex, request.getHubId(), scenarioAdded.getName(), 
                            condition.getSensorId(), condition.getType(), condition.getOperation());
                    }
                    conditionIndex++;
                }
                int actionIndex = 0;
                for (var action : scenarioAdded.getActionList()) {
                    if (action.getType() == ru.yandex.practicum.grpc.telemetry.ActionTypeProto.ACTION_TYPE_UNSPECIFIED) {
                        log.warn("Найдено UNSPECIFIED в действии #{} сценария: hubId={}, name={}, sensorId={}, type={}. " +
                            "Будет использовано значение по умолчанию (ACTIVATE).", 
                            actionIndex, request.getHubId(), scenarioAdded.getName(), 
                            action.getSensorId(), action.getType());
                    }
                    actionIndex++;
                }
            }
            
            var dto = protobufMapper.toDto(request);
            collectorService.collectHubEvent(dto);
            
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("Successfully processed hub event: {}", request.getHubId());
        } catch (IllegalArgumentException e) {
            // Обрабатываем ошибки валидации (UNSPECIFIED) более мягко
            if (e.getMessage() != null && e.getMessage().contains("unspecified")) {
                log.warn("Пропускаем событие с UNSPECIFIED значениями: hubId={}, payloadCase={}, error={}", 
                    request.getHubId(), request.getPayloadCase(), e.getMessage());
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            } else {
                log.error("Error processing hub event: hubId={}, payloadCase={}, error={}", 
                    request.getHubId(), request.getPayloadCase(), e.getMessage(), e);
                responseObserver.onError(new StatusRuntimeException(
                        Status.INTERNAL
                                .withDescription(e.getLocalizedMessage())
                                .withCause(e)
                ));
            }
        } catch (Exception e) {
            log.error("Error processing hub event: hubId={}, payloadCase={}, error={}", 
                request.getHubId(), request.getPayloadCase(), e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
