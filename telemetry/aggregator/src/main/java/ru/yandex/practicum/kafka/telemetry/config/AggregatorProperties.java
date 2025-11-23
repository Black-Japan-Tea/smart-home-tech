package ru.yandex.practicum.kafka.telemetry.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aggregator")
public class AggregatorProperties {

    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();

    @Getter
    @Setter
    public static class Topics {
        private String sensorsTopic = "telemetry.sensors.v1";
        private String snapshotsTopic = "telemetry.snapshots.v1";
    }

    @Getter
    @Setter
    public static class Consumer {
        private String groupId = "telemetry-aggregator";
        private String clientId = "telemetry-aggregator";
        private Duration pollTimeout = Duration.ofSeconds(1);
    }
}

