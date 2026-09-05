package ru.practicum.aggregator.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.config.KafkaConfig;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProducer {

    private final KafkaProducer<String, byte[]> producer;
    private final KafkaConfig kafkaConfig;

    public void send(EventSimilarityAvro similarity) {
        if (similarity == null) {
            log.warn("Попытка отправить null");
            return;
        }

        String key = similarity.getEventA() + "-" + similarity.getEventB();
        String topic = kafkaConfig.getEventsSimilarityTopic();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(similarity, encoder);
            encoder.flush();

            byte[] serializedValue = outputStream.toByteArray();

            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, serializedValue);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки similarity для пары {}: {}", key, exception.getMessage());
                } else {
                    log.debug("Similarity отправлен: eventA={}, eventB={}, score={}",
                            similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                }
            });

        } catch (IOException e) {
            log.error("Ошибка сериализации similarity: {}", e.getMessage());
        }
    }

    public void flush() {
        try {
            producer.flush();
        } catch (Exception e) {
            log.error("Ошибка при сбросе продюсера: {}", e.getMessage());
        }
    }

    public void close() {
        try {
            producer.close();
        } catch (Exception e) {
            log.error("Ошибка при закрытии продюсера: {}", e.getMessage());
        }
    }
}