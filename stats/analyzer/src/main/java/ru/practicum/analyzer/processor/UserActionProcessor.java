package ru.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor implements Runnable {

    private final KafkaConsumer<String, UserActionAvro> userActionConsumer;
    private final UserActionRepository userActionRepository;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            userActionConsumer.subscribe(Collections.singletonList("stats.user-actions.v1"));
            log.info("UserActionProcessor subscribed to stats.user-actions.v1");

            while (running) {
                ConsumerRecords<String, UserActionAvro> records = userActionConsumer.poll(Duration.ofMillis(5000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    processUserAction(record.value());
                }

                userActionConsumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in UserActionProcessor", e);
        } finally {
            userActionConsumer.close();
            log.info("UserActionProcessor consumer closed");
        }
    }

    private void processUserAction(UserActionAvro event) {
        try {
            long userId = event.getUserId();
            long eventId = event.getEventId();
            double weight = getActionWeight(event);

            UserActionEntity existing = userActionRepository
                    .findByUserIdAndEventId(userId, eventId)
                    .orElse(null);

            if (existing != null && existing.getWeight() >= weight) {
                return;
            }

            UserActionEntity entity = UserActionEntity.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .weight(weight)
                    .lastActionTime(Instant.now())
                    .build();

            if (existing != null) {
                entity.setId(existing.getId());
            }

            userActionRepository.save(entity);
            log.debug("Saved user action: userId={}, eventId={}, weight={}", userId, eventId, weight);

        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }

    private double getActionWeight(UserActionAvro event) {
        return switch (event.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    public void shutdown() {
        running = false;
    }
}