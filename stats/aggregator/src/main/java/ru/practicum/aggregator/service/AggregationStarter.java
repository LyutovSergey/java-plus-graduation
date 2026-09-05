package ru.practicum.aggregator.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.config.KafkaConfig;
import ru.practicum.aggregator.producer.EventSimilarityProducer;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final EventSimilarityProducer similarityProducer;
    private final SimilarityManager similarityManager;
    private final KafkaConfig kafkaConfig;

    private volatile boolean running = true;

    public void start() {
        log.info("Запуск AggregationStarter...");

        try {
            String topic = kafkaConfig.getUserActionsTopic();
            consumer.subscribe(List.of(topic));
            log.info("Подписка на топик {} выполнена", topic);

            while (running) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("Получено {} записей", records.count());

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    UserActionAvro event = record.value();

                    if (event == null) {
                        log.warn("Получено пустое событие, пропускаем");
                        continue;
                    }

                    log.debug("Обработка действия пользователя {} к событию {}", event.getUserId(), event.getEventId());

                    Optional<List<EventSimilarityAvro>> updatedSimilarities = similarityManager.updateState(event);
                    if (updatedSimilarities.isPresent()) {
                        List<EventSimilarityAvro> similarities = updatedSimilarities.get();
                        for (EventSimilarityAvro similarity : similarities) {
                            similarityProducer.send(similarity);
                        }
                        similarityProducer.flush();
                        log.info("Обновлены similarity для события {}", event.getEventId());
                    } else {
                        log.debug("Similarity не изменился, пропускаем");
                    }
                }

                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка при фиксации смещений: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в цикле обработки событий", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            consumer.commitSync();
        } catch (Exception e) {
            log.warn("Ошибка при фиксации смещений: {}", e.getMessage());
        }

        try {
            similarityProducer.flush();
            similarityProducer.close();
        } catch (Exception e) {
            log.warn("Ошибка при закрытии продюсера: {}", e.getMessage());
        }

        try {
            consumer.close();
        } catch (Exception e) {
            log.warn("Ошибка при закрытии консьюмера: {}", e.getMessage());
        }

        log.info("AggregationStarter завершен");
    }

    @PreDestroy
    public void stop() {
        log.info("Получен сигнал завершения, останавливаем AggregationStarter...");
        running = false;
    }
}