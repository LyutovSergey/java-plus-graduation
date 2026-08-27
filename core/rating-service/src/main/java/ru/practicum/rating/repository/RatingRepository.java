package ru.practicum.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.rating.model.Rating;
import ru.practicum.rating.model.Reaction;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	Optional<Rating> findByUserIdAndEventId(Long userId, Long eventId);

	boolean existsByUserIdAndEventId(Long userId, Long eventId);

	long countByEventIdAndReaction(Long eventId, Reaction reaction);
}
