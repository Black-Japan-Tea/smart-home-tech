package ru.yandex.practicum.kafka.telemetry.service;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.service.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.entity.Action;
import ru.yandex.practicum.kafka.telemetry.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.entity.ScenarioAction;

import java.time.Instant;

/**
 * Сервис для отправки команд устройствам через Hub Router.
 */
@Slf4j
@Service
public class HubRouterService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    /**
     * Отправляет действие на выполнение устройству через Hub Router.
     */
    public void executeAction(Scenario scenario, ScenarioAction scenarioAction) {
        try {
            Action action = scenarioAction.getAction();
            String sensorId = scenarioAction.getSensor().getId();
            
            DeviceActionProto.Builder deviceActionProto =
                DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(mapActionType(action.getType()));
            
            if (action.getValue() != null) {
                deviceActionProto = deviceActionProto.setValue(action.getValue());
            }

            ru.yandex.practicum.grpc.telemetry.DeviceActionRequest request = 
                ru.yandex.practicum.grpc.telemetry.DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(deviceActionProto)
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                    .build();
            
            log.info("Отправляем действие в Hub Router: hubId={}, scenario={}, sensorId={}, type={}, value={}",
                scenario.getHubId(), scenario.getName(), sensorId, action.getType(), action.getValue());
            
            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Действие успешно отправлено в Hub Router: hubId={}, scenario={}, sensorId={}",
                    scenario.getHubId(), scenario.getName(), sensorId);
            } catch (io.grpc.StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
                    log.warn("Hub Router недоступен, но действие было подготовлено: hubId={}, scenario={}, sensorId={}",
                        scenario.getHubId(), scenario.getName(), sensorId);
                    // Не бросаем исключение, чтобы не прерывать обработку других сценариев
                } else {
                    log.error("Ошибка при отправке действия в Hub Router: hubId={}, scenario={}, error={}", 
                        scenario.getHubId(), scenario.getName(), e.getMessage(), e);
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке действия в Hub Router для сценария {}: {}", 
                scenario.getName(), e.getMessage(), e);
            // Не бросаем исключение, чтобы не прерывать обработку других сценариев
            // throw new RuntimeException("Failed to send action to hub router", e);
        }
    }

    private ru.yandex.practicum.grpc.telemetry.ActionTypeProto mapActionType(String type) {
        return switch (type) {
            case "ACTIVATE" -> ru.yandex.practicum.grpc.telemetry.ActionTypeProto.ACTIVATE;
            case "DEACTIVATE" -> ru.yandex.practicum.grpc.telemetry.ActionTypeProto.DEACTIVATE;
            case "INVERSE" -> ru.yandex.practicum.grpc.telemetry.ActionTypeProto.INVERSE;
            case "SET_VALUE" -> ru.yandex.practicum.grpc.telemetry.ActionTypeProto.SET_VALUE;
            default -> {
                log.warn("Unknown action type: {}", type);
                yield ru.yandex.practicum.grpc.telemetry.ActionTypeProto.ACTIVATE;
            }
        };
    }
}

