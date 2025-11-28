package ru.yandex.practicum.kafka.telemetry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("aggregator.kafka")
public class AggregatorKafkaProperties {

    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String sensors;
        private String snapshots;
    }

    @Data
    public static class Consumer {
        private String groupId;
    }
}

