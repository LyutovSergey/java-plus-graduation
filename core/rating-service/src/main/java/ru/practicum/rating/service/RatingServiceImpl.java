package ru.practicum.rating.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.rating.repository.RatingRepository;
import ru.practicum.rating.dto.RatingRequest;
import ru.practicum.rating.dto.RatingResponse;
import ru.practicum.rating.model.Rating;
import ru.practicum.rating.model.Reaction;
import ru.practicum.common.client.EventClient;
import ru.practicum.common.client.UserClient;
import ru.practicum.common.dto.EventDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;



import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

	private final RatingRepository ratingRepository;
	private final UserClient userClient;
	private final EventClient eventClient;

	@Override
	public RatingResponse addOrUpdateReaction(Long userId, Long eventId, RatingRequest request) {
		List<UserDto> users = userClient.getUsersByIds(List.of(userId));
		if (users.isEmpty()) {
			throw new NotFoundException("Пользователь с id=" + userId + " не найден");
		}
		UserDto user = users.get(0);

		List<EventDto> events = eventClient.getEventsByIds(List.of(eventId));
		if (events.isEmpty()) {
			throw new NotFoundException("Событие с id=" + eventId + " не существует или не опубликовано.");
		}
		EventDto event = events.get(0);

		if (!event.published()) {
			throw new NotFoundException("Событие с id=" + eventId + " не существует или не опубликовано.");
		}

		if (Objects.equals(user.id(), event.initiatorId())) {
			throw new ValidationException("Нельзя ставить реакции своим событиям");
		}

		Rating rating = ratingRepository.findByUserIdAndEventId(userId, eventId).orElse(null);

		if (rating != null) {
			if (rating.getReaction() == request.getReaction()) {
				ratingRepository.delete(rating);
				updateEventRate(eventId);
				throw new ConflictException("Reaction removed");
			} else {
				rating.setReaction(request.getReaction());
				ratingRepository.save(rating);
				updateEventRate(eventId);
				return mapToResponse(rating);
			}
		} else {
			rating = Rating.builder()
					.userId(userId)
					.userId(eventId)
					.reaction(request.getReaction())
					.build();
			ratingRepository.save(rating);
			updateEventRate(eventId);
			return mapToResponse(rating);
		}
	}

	@Override
	public void removeReaction(Long userId, Long eventId) {
		Rating rating = ratingRepository.findByUserIdAndEventId(userId, eventId)
				.orElseThrow(() -> new NotFoundException("Реакция не найдена"));
		ratingRepository.delete(rating);
		updateEventRate(eventId);
	}

	private void updateEventRate(Long eventId) {
		long likes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.LIKE);
		long dislikes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.DISLIKE);
		eventClient.updateEventRate(eventId, likes - dislikes);
	}

	private RatingResponse mapToResponse(@NonNull Rating rating) {
		return RatingResponse.builder()
				.id(rating.getId())
				.userId(rating.getUserId())
				.eventId(rating.getEventId())
				.reaction(rating.getReaction())
				.build();
	}

	@Override
	public Map<Long, Long> getRatingsByEventIds(List<Long> eventIds) {
		if (eventIds == null || eventIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, Long> result = new HashMap<>();
		for (Long eventId : eventIds) {
			long likes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.LIKE);
			long dislikes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.DISLIKE);
			result.put(eventId, likes - dislikes);
		}
		return result;
	}
}
