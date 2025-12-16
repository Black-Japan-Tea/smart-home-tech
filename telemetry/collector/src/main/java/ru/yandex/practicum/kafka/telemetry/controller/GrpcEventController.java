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
import ru.yandex.practicum.kafka.telemetry.mapper.ProtobufMapper;
import ru.yandex.practicum.kafka.telemetry.service.CollectorService;
import telemetry.service.collector.CollectorControllerGrpc;

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
            log.info("Received hub event via gRPC: {}", request.getHubId());
            var dto = protobufMapper.toDto(request);
            collectorService.collectHubEvent(dto);
            
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("Successfully processed hub event: {}", request.getHubId());
        } catch (Exception e) {
            log.error("Error processing hub event: {}", request.getHubId(), e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
