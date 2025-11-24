package ru.yandex.practicum.kafka.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Slf4j
@Service
public class DeviceActionDispatcher {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void dispatch(DeviceActionRequest request) {
        try {
            hubRouterClient.handleDeviceAction(request);
            log.info("Dispatched action {} for hub {} scenario {}", request.getAction().getSensorId(), request.getHubId(), request.getScenarioName());
        } catch (Exception e) {
            log.error("Failed to send device action for hub {} scenario {}", request.getHubId(), request.getScenarioName(), e);
        }
    }
}

