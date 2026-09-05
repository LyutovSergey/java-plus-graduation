package ru.practicum.aggregator.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.topic.user-actions:stats.user-actions.v1}")
    private String userActionsTopic;

    @Value("${kafka.topic.events-similarity:stats.events-similarity.v1}")
    private String eventsSimilarityTopic;

    @Bean
    public KafkaConsumer<String, UserActionAvro> kafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "ru.practicum.aggregator.deserializer.UserActionDeserializer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        return new KafkaConsumer<>(props);
    }

    @Bean
    public KafkaProducer<String, byte[]> kafkaProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        return new KafkaProducer<>(props);
    }
}