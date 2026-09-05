package ru.practicum.collector.service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.stats.avro.UserActionAvro;

@Slf4j
@Service
public class KafkaEventProducer {

    @Value("${kafka.topic.user-actions}")
    private String userActionsTopic;

    private final Producer<String, SpecificRecordBase> kafkaProducer;

    public KafkaEventProducer(Producer<String, SpecificRecordBase> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public void send(UserActionAvro event) {
        String key = String.valueOf(event.getUserId());
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(userActionsTopic, key, event);

        kafkaProducer.send(record, (metadata, ex) -> {
            if (ex == null) {
                log.debug("Successfully sent user action: offset={}", metadata.offset());
            } else {
                log.error("Failed to send user action", ex);
            }
        });
    }
}