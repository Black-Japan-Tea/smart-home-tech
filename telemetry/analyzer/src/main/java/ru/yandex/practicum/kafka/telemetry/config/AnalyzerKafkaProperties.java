package ru.yandex.practicum.kafka.telemetry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("analyzer.kafka")
public class AnalyzerKafkaProperties {

    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String snapshots;
        private String hubs;
    }

    @Data
    public static class Consumer {
        private String snapshotsGroupId;
        private String hubsGroupId;
        private Long pollTimeoutMs;
    }
}

