package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;
import java.util.*;


@Slf4j
@Component
public class SimilarityManager {

    // Веса действий
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;



    private double getActionWeight(UserActionAvro event) {
        return switch (event.getActionType()) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    // Заготовка
    public Optional<List<EventSimilarityAvro>> updateState(UserActionAvro event) {

        return Optional.empty();
    }


}