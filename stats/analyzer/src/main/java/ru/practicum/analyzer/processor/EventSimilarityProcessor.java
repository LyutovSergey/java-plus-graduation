package ru.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    private final KafkaConsumer<String, EventSimilarityAvro> similarityConsumer;
    private final EventSimilarityRepository similarityRepository;

    private volatile boolean running = true;

    public void start() {
        try {
            similarityConsumer.subscribe(Collections.singletonList("stats.events-similarity.v1"));
            log.info("EventSimilarityProcessor subscribed to stats.events-similarity.v1");

            while (running) {
                ConsumerRecords<String, EventSimilarityAvro> records = similarityConsumer.poll(Duration.ofMillis(5000));

                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    processSimilarity(record.value());
                }

                similarityConsumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in EventSimilarityProcessor", e);
        } finally {
            similarityConsumer.close();
            log.info("EventSimilarityProcessor consumer closed");
        }
    }

    private void processSimilarity(EventSimilarityAvro event) {
        try {
            long eventA = event.getEventA();
            long eventB = event.getEventB();
            double score = event.getScore();

            EventSimilarityEntity existing = similarityRepository
                    .findByEventAAndEventB(eventA, eventB)
                    .orElse(null);

            EventSimilarityEntity entity = EventSimilarityEntity.builder()
                    .eventA(eventA)
                    .eventB(eventB)
                    .score(score)
                    .updatedAt(Instant.now())
                    .build();

            if (existing != null) {
                entity.setId(existing.getId());
            }

            similarityRepository.save(entity);
            log.debug("Saved similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);

        } catch (Exception e) {
            log.error("Error processing similarity", e);
        }
    }

    public void shutdown() {
        running = false;
    }
}