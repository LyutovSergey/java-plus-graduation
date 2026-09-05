package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.EventSimilarityEntity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE e.eventA = :eventId OR e.eventB = :eventId ORDER BY e.score DESC")
    List<EventSimilarityEntity> findSimilarEvents(@Param("eventId") Long eventId);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE (e.eventA = :eventId OR e.eventB = :eventId) " +
            "AND e.eventA NOT IN :excludeIds AND e.eventB NOT IN :excludeIds " +
            "ORDER BY e.score DESC")
    List<EventSimilarityEntity> findSimilarEventsExcluding(@Param("eventId") Long eventId,
                                                           @Param("excludeIds") List<Long> excludeIds);
}