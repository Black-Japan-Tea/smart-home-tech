package ru.yandex.practicum.kafka.telemetry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("collector.kafka")
public class CollectorKafkaProperties {

    private Topics topics = new Topics();

    @Data
    public static class Topics {
        private String sensors;
        private String hubs;
    }
}

