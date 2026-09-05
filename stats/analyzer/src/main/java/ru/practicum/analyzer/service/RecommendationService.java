package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.stats.proto.RecommendedEventProto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository similarityRepository;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        // 1. Получаем все события, с которыми взаимодействовал пользователь
        List<Long> interactedEvents = userActionRepository.findAllEventIdsByUserId(userId);

        if (interactedEvents.isEmpty()) {
            return List.of();
        }

        // 2. Для каждого взаимодействия ищем похожие события
        List<EventSimilarityEntity> allSimilar = new ArrayList<>();
        for (Long eventId : interactedEvents) {
            List<EventSimilarityEntity> similar = similarityRepository.findSimilarEvents(eventId);
            allSimilar.addAll(similar);
        }

        // 3. Фильтруем: исключаем уже просмотренные
        List<RecommendedEventProto> result = allSimilar.stream()
                .filter(sim -> !interactedEvents.contains(sim.getEventA()) ||
                        !interactedEvents.contains(sim.getEventB()))
                .map(sim -> {
                    long recommendedEventId = interactedEvents.contains(sim.getEventA())
                            ? sim.getEventB()
                            : sim.getEventA();

                    // Дополнительная проверка: не должно быть в списке просмотренных
                    if (interactedEvents.contains(recommendedEventId)) {
                        return null;
                    }

                    return RecommendedEventProto.newBuilder()
                            .setEventId(recommendedEventId)
                            .setScore(sim.getScore())
                            .build();
                })
                .filter(rec -> rec != null)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults > 0 ? maxResults : 10)
                .collect(Collectors.toList());

        return result;
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        // 1. Получаем события, с которыми взаимодействовал пользователь
        List<Long> interactedEvents = userActionRepository.findAllEventIdsByUserId(userId);

        // 2. Ищем похожие на eventId
        List<EventSimilarityEntity> similar = similarityRepository.findSimilarEvents(eventId);

        // 3. Исключаем уже просмотренные
        List<RecommendedEventProto> result = similar.stream()
                .filter(sim -> {
                    long candidateA = sim.getEventA();
                    long candidateB = sim.getEventB();
                    // Если оба события в списке просмотренных — исключаем
                    if (interactedEvents.contains(candidateA) && interactedEvents.contains(candidateB)) {
                        return false;
                    }
                    return true;
                })
                .map(sim -> {
                    long recommendedEventId = eventId == sim.getEventA()
                            ? sim.getEventB()
                            : sim.getEventA();

                    // Если рекомендуемое событие уже просмотрено — пропускаем
                    if (interactedEvents.contains(recommendedEventId)) {
                        return null;
                    }

                    return RecommendedEventProto.newBuilder()
                            .setEventId(recommendedEventId)
                            .setScore(sim.getScore())
                            .build();
                })
                .filter(rec -> rec != null)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults > 0 ? maxResults : 10)
                .collect(Collectors.toList());

        return result;
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<RecommendedEventProto> result = new ArrayList<>();
        for (Long eventId : eventIds) {
            Double totalWeight = userActionRepository.sumWeightsByEventId(eventId);
            if (totalWeight == null) {
                totalWeight = 0.0;
            }

            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(totalWeight)
                    .build());
        }
        return result;

    }
}