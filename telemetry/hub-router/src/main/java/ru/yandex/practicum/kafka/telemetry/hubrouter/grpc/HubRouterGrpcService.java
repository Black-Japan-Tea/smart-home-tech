package ru.yandex.practicum.kafka.telemetry.hubrouter.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.service.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.hubrouter.service.ActionLogService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class HubRouterGrpcService extends HubRouterControllerGrpc.HubRouterControllerImplBase {

    private final ActionLogService actionLogService;

    @Override
    public void handleDeviceAction(
        DeviceActionRequest request,
        StreamObserver<Empty> responseObserver
    ) {
        log.info(
            "gRPC action received: hubId={}, scenario={}, sensorId={}, type={}, value={}",
            request.getHubId(),
            request.getScenarioName(),
            request.getAction().getSensorId(),
            request.getAction().getType(),
            request.getAction().getValue()
        );

        actionLogService.record(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

