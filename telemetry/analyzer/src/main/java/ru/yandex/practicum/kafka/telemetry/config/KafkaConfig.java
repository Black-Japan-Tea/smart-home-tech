package ru.yandex.practicum.kafka.telemetry.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AnalyzerKafkaProperties.class)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final AnalyzerKafkaProperties analyzerKafkaProperties;

    public KafkaConfig(AnalyzerKafkaProperties analyzerKafkaProperties) {
        this.analyzerKafkaProperties = analyzerKafkaProperties;
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotKafkaConsumer() {
        Map<String, Object> props = baseConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, analyzerKafkaProperties.getConsumer().getSnapshotsGroupId());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorsSnapshotDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(props);
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumer<String, HubEventAvro> hubEventKafkaConsumer() {
        Map<String, Object> props = baseConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, analyzerKafkaProperties.getConsumer().getHubsGroupId());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, HubEventDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return new KafkaConsumer<>(props);
    }

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }
}

