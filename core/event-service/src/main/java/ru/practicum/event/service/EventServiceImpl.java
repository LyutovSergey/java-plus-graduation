package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.practicum.common.client.RequestClient;
import ru.practicum.common.client.UserClient;
import ru.practicum.common.dto.EventDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.StateMapper;
import ru.practicum.event.model.*;
import ru.practicum.event.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.util.specification.EventSpecifications;
import ru.practicum.event.util.specification.SpecBuilder;
import ru.practicum.event.util.statistic.StatRepository;
import ru.practicum.stat.dto.ViewStatsDto;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

	StatRepository statRepository;
	EventRepository eventRepository;
	UserClient userClient;
	CategoryRepository categoryRepository;
	RequestClient requestClient;

	@Override
	public List<EventShortDto> getFreeEvents(@NonNull FreeGetDto dto, HttpServletRequest request) {

		if (dto.rangeStart() != null && dto.rangeEnd() != null) {
			if (dto.rangeEnd().isBefore(dto.rangeStart())) {
				throw new ValidationException("Окончание события не может быть раньше начала");
			}
		}

		statRepository.sendHitRequest(request);

		SpecBuilder<Event> builder = SpecBuilder.<Event>builder()
				.and(EventSpecifications.isPublished())
				.andIf(dto.text() != null && !dto.text().isBlank(),
						() -> EventSpecifications.textContains(dto.text()))
				.andIf(dto.categories() != null && !dto.categories().isEmpty(),
						() -> EventSpecifications.hasCategories(dto.categories()))
				.andIf(dto.paid() != null,
						() -> EventSpecifications.isPaid(dto.paid()))
				.andIf(Boolean.TRUE.equals(dto.onlyAvailable()),
						() -> EventSpecifications.onlyAvailable(true));

		boolean hasStart = dto.rangeStart() != null;
		boolean hasEnd = dto.rangeEnd() != null;

		if (!hasStart && !hasEnd) {
			builder.and(EventSpecifications.eventDateAfterNow(LocalDateTime.now()));
		} else {
			builder
					.andIf(hasStart,
							() -> EventSpecifications.dateAfter(dto.rangeStart()))
					.andIf(hasEnd,
							() -> EventSpecifications.dateBefore(dto.rangeEnd()));
		}

		Specification<Event> spec = builder.build();

		Sort sort = Sort.unsorted();

		if (dto.sort() != null) {
			switch (dto.sort()) {
				case EVENT_DATE -> sort = Sort.by("eventDate").ascending();
				case VIEWS -> sort = Sort.by("views").descending();
			}
		}

		Pageable pageable = PageRequest.of(
				dto.from() / dto.size(),
				dto.size(),
				sort
		);

		List<Event> events = eventRepository.findAll(spec, pageable).getContent();
		if (events.isEmpty()) {
			return Collections.emptyList();
		}

		// Получаем через  Feign
		Map<Long, Long> requestCounts = requestClient.getConfirmedRequestsByEventIds(
				events.stream().map(Event::getId).toList()
		);


		List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
		List<ViewStatsDto> stats = statRepository.getStat(
				uris,
				dto.rangeStart(),
				dto.rangeEnd(),
				false);

		return events.stream()
				.map(event -> EventMapper.toEventShortDto(
								event,
								requestCounts.getOrDefault(event.getId(), 0L),
								getHits(stats, event.getId())
						)
				).toList();
	}

	@Override
	public EventFullDto getFreeEventById(Long eventId, HttpServletRequest request) {
		if (!eventRepository.existsByIdAndState(eventId, EventState.PUBLISHED)) {
			throw new NotFoundException("Событие с id=" + eventId + " не существует или не опубликовано.");
		}
		statRepository.sendHitRequest(request);
		Event event = getEventById(eventId);

		long views = statRepository.getStat(List.of(request.getRequestURI()), true).getFirst().getHits();

		long confirmedRequests = getConfirmedRequests(eventId);

		return EventMapper.toEventFullDto(event, confirmedRequests, views);
	}

	@Override
	public EventFullDto userAddNewEvent(Long userId, @NonNull NewEventDto newEventDto) {
		if (newEventDto.participantLimit() != null && newEventDto.participantLimit() < 0) {
			throw new ValidationException("Ограничение на количество участников должно быть положительным числом");
		}

		if (newEventDto.eventDate() != null && newEventDto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
			throw new ValidationException("Начало события не может быть раньше, " +
					"чем через два часа от текущего момента");
		}

		UserDto initiator =getUserById(userId);
		Category category = getCategoryById(newEventDto.category());

		Event event = EventMapper.toEntity(
				newEventDto,
				category,
				LocalDateTime.now(),
				initiator.id(),
				null,
				EventState.PENDING
		);

		return EventMapper.toEventFullDto(eventRepository.save(event), 0L, 0L);
	}

	@Override
	public List<EventFullDto> adminGetEvents(@NonNull AdminGetDto dto) {
		Specification<Event> spec = SpecBuilder.<Event>builder()
				.andIf(dto.users() != null && !dto.users().isEmpty(),
						() -> EventSpecifications.hasUsers(dto.users()))
				.andIf(dto.states() != null && !dto.states().isEmpty(),
						() -> EventSpecifications.hasStates(dto.states()))
				.andIf(dto.categories() != null && !dto.categories().isEmpty(),
						() -> EventSpecifications.hasCategories(dto.categories()))
				.andIf(dto.rangeStart() != null,
						() -> EventSpecifications.dateAfter(dto.rangeStart()))
				.andIf(dto.rangeEnd() != null,
						() -> EventSpecifications.dateBefore(dto.rangeEnd()))
				.build();

		Pageable pageable = PageRequest.of(
				dto.from() / dto.size(),
				dto.size()
		);

		List<Event> events = eventRepository.findAll(spec, pageable).getContent();
		if (events.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, Long> requestCounts = requestClient.getConfirmedRequestsByEventIds(
				events.stream().map(Event::getId).toList()
		);

		List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
		List<ViewStatsDto> stats = statRepository.getStat(
				uris,
				dto.rangeStart(),
				dto.rangeEnd(),
				false);

		return events.stream()
				.map(event -> EventMapper.toEventFullDto(
								event,
								requestCounts.getOrDefault(event.getId(), 0L),
								getHits(stats, event.getId())
						)
				).toList();
	}

	@Override
	public EventFullDto adminUpdateEvent(Long eventId, @NonNull UpdateEventAdminRequest request) {
		Event oldEvent = getEventById(eventId);
		Event newEvent;

		if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
			throw new ValidationException("Дата события должна быть не может быть раньше, " +
					"чем через два часа от текущего момента");
		}

		if (request.stateAction() == null) {
			newEvent = EventMapper.update(
					oldEvent,
					request,
					oldEvent.getState(),
					oldEvent.getPublishedOn(),
					request.category() == null ?
							Optional.empty() :
							Optional.of(getCategoryById(request.category()))
			);
		} else {

			if (request.stateAction().equals(AdminStateAction.PUBLISH_EVENT) &&
					oldEvent.getState().equals(EventState.PUBLISHED)) {
				throw new ConflictException("Событие с id=" + oldEvent.getId() + " уже опубликовано");
			}

			if (request.stateAction().equals(AdminStateAction.PUBLISH_EVENT) &&
					oldEvent.getState().equals(EventState.CANCELED)) {
				throw new ConflictException("Публикация события с id=" + oldEvent.getId() +
						" уже отменена пользователем");
			}

			if (request.stateAction().equals(AdminStateAction.REJECT_EVENT) &&
					oldEvent.getState().equals(EventState.PUBLISHED)) {
				throw new ConflictException("Событие с id=" + oldEvent.getId() +
						" уже опубликовано, отмена не возможна");
			}

			newEvent = EventMapper.update(
					oldEvent,
					request,
					request.stateAction().equals(AdminStateAction.REJECT_EVENT) ?
							EventState.CANCELED : EventState.PUBLISHED,
					request.stateAction().equals(AdminStateAction.REJECT_EVENT) ?
							null : LocalDateTime.now(),
					request.category() == null ?
							Optional.empty() :
							Optional.of(getCategoryById(request.category()))
			);
		}

		return EventMapper.toEventFullDto(
				eventRepository.save(newEvent),
				getConfirmedRequests(oldEvent.getId()),
				getHits(oldEvent.getId())
		);
	}

	@Override
	public List<EventShortDto> findByUserId(Long userId, Integer from, Integer size) {
		checkUser(userId);

		PageRequest pageRequest = PageRequest.of(from / size, size);
		Collection<Event> events = eventRepository.findByInitiatorId(userId, pageRequest);
		if (events.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, Long> requestCounts = requestClient.getConfirmedRequestsByEventIds(
				events.stream().map(Event::getId).toList()
		);

		List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
		List<ViewStatsDto> stats = statRepository.getStat(uris, true);

		return events.stream()
				.map(event -> EventMapper.toEventShortDto(
								event,
								requestCounts.getOrDefault(event.getId(), 0L),
								getHits(stats, event.getId())
						)
				).toList();
	}

	@Override
	public EventFullDto findEventById(Long userId, Long eventId) {
		checkUser(userId);

		Event event = getEventById(eventId);

		if (!event.getInitiatorId().equals(userId)) {
			throw new ConflictException("Пользователь должен быть инициатором");
		}

		return EventMapper.toEventFullDto(
				event,
				getConfirmedRequests(event.getId()),
				getHits(event.getId())
		);
	}

	@Override
	public EventFullDto patchEvent(Long userId, Long eventId, @NonNull UpdateEventUserRequest request) {
		if (request.participantLimit() != null && request.participantLimit() < 0) {
			throw new ValidationException("Ограничение на количество участников должно быть положительным числом");
		}

		checkUser(userId);
		return patchEvent(eventId, request, 2, false);
	}

	private EventFullDto patchEvent(Long eventId, @NonNull UpdateEventUserRequest request, long hoursBeforeStart,
	                                boolean isAdmin) {
		try {
			Event event = getEventById(eventId);

			if (!isAdmin && event.getState() == EventState.PUBLISHED) {
				throw new ConflictException("Нельзя редактировать опубликованное событие");
			}

			if (request.eventDate() != null) {
				LocalDateTime eventDateTime = request.eventDate();
				LocalDateTime minDateTime = LocalDateTime.now().plusHours(hoursBeforeStart);

				if (!isAdmin && eventDateTime.isBefore(minDateTime)) {
					throw new ValidationException(
							String.format("Дата события должна быть не ранее чем за %d часа(ов) до начала",
									hoursBeforeStart)
					);
				}
			}

			UserStateAction action = request.stateAction();

			if (action != null) {
				EventState newState = isAdmin
						? StateMapper.mapAdminEventAction(action)
						: StateMapper.mapUserEventAction(action);

				if (EventState.PUBLISHED.equals(newState)) {
					event.setPublishedOn(LocalDateTime.from(Instant.now()));
				}
				if (newState != null) {
					event.setState(newState);
				}
			}

			if (request.category() != null) {
				Category category = getCategoryById(request.category());
				event.setCategory(category);
			}

			EventMapper.merge(event, request);

			Event patched = eventRepository.save(event);

			log.info("Ивент обновлен: {}", patched.getId());

			return EventMapper.toEventFullDto(
					event,
					getConfirmedRequests(event.getId()),
					getHits(event.getId())
			);

		} catch (DataIntegrityViolationException e) {
			log.debug("Конфликт вовремя обновления ивента {}", request, e);
			throw new ConflictException("Конфликт с другим ивентом");
		}
	}

	private void checkUser(Long userId) {
		List<UserDto> users = userClient.getUsersByIds(List.of(userId));
		if (users.isEmpty()) {
			throw new NotFoundException("Пользователь с id=" + userId + " не найден");
		}
	}

	@NonNull
	private UserDto getUserById(long userId) {
		List<UserDto> users = userClient.getUsersByIds(List.of(userId));
		if (users.isEmpty()) {
			throw new NotFoundException("Пользователь с id=" + userId + " не найден");
		}
		return users.get(0);
	}

	@NonNull
	private Event getEventById(long eventId) {
		return eventRepository.findById(eventId).orElseThrow(
				() -> new NotFoundException("Событие с id=" + eventId + " не найдено")
		);
	}

	@NonNull
	private Category getCategoryById(long categoryId) {
		return categoryRepository.findById(categoryId).orElseThrow(
				() -> new NotFoundException("Категория с id=" + categoryId + " не найдена")
		);
	}

	private long getConfirmedRequests(@NonNull Map<Long, Long> requestCountMap, long eventId) {
		return requestCountMap.getOrDefault(eventId, 0L);
	}

	private long getHits(long eventId) {
		List<ViewStatsDto> stats = statRepository.getStat(List.of("/events/" + eventId), true);
		if (stats.isEmpty()) {
			return 0;
		}
		return stats.getFirst().getHits();
	}

	private long getHits(@NonNull List<ViewStatsDto> stats, long eventId) {
		if (stats.isEmpty()) {
			return 0;
		}
		for (ViewStatsDto stat : stats) {
			if (stat.getUri().equals("/events/" + eventId)) {
				return stat.getHits();
			}
		}
		return 0;
	}

	@Override
	public void updateEventRate(Long eventId, Long rate) {
		Event event = getEventById(eventId);
		event.setRate(rate);
		eventRepository.save(event);
	}

	@Override
	public List<EventDto> getEventsByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return eventRepository.findAllById(ids).stream()
				.map(event -> new EventDto(
						event.getId(),
						event.getInitiatorId() ,
						event.getState() == EventState.PUBLISHED,
						event.getParticipantLimit(),
						event.isRequestModeration()
				))
				.collect(Collectors.toList());
	}

	private long getConfirmedRequests(Long eventId) {
		Map<Long, Long> counts = requestClient.getConfirmedRequestsByEventIds(List.of(eventId));
		return counts.getOrDefault(eventId, 0L);
	}


}
