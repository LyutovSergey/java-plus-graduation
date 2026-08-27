package ru.practicum.request.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.client.EventClient;
import ru.practicum.common.client.UserClient;
import ru.practicum.common.dto.EventDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.ParticipationStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.service.RequestService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {

	RequestRepository requestRepository;
	UserClient userClient;
	EventClient eventClient;

	@Override
	public List<ParticipationRequestDto> findByEventId(Long userId, Long eventId) {
		EventDto event = getEventById(eventId);
		if (!event.initiatorId().equals(userId)) {
			throw new NotFoundException("Событие не найдено");
		}

		return requestRepository.findByEventId(eventId)
				.stream()
				.map(RequestMapper::toParticipationRequestDto)
				.toList();
	}

	@Override
	public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
															  EventRequestStatusUpdateRequest request) {
		EventDto event = getEventById(eventId);
		if (!event.initiatorId().equals(userId)) {
			throw new NotFoundException("Событие не найдено");
		}

		int limit = event.participantLimit() != null ? event.participantLimit() : 0;
		List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
		List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

		boolean isModerationOff = !event.requestModeration() || limit == 0;
		boolean idsEmpty = request.requestIds() == null || request.requestIds().isEmpty();

		if (isModerationOff || idsEmpty) {
			return EventRequestStatusUpdateResult.builder()
					.confirmedRequests(Collections.emptyList())
					.rejectedRequests(Collections.emptyList())
					.build();
		}

		int countConfirmed = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
		List<ParticipationRequest> requests = requestRepository.findAllByIdIn(request.requestIds());

		if (request.status().name().equals(ParticipationStatus.CONFIRMED.name()) && countConfirmed >= limit) {
			throw new ConflictException("Достигнут лимит подтвержденных заявок");
		}

		for (ParticipationRequest pr : requests) {
			if (!pr.getStatus().equals(ParticipationStatus.PENDING)) {
				throw new ConflictException("Статус можно изменить только у заявок в состоянии рассмотрения");
			}

			if (request.status().name().equals(ParticipationStatus.CONFIRMED.name()) && countConfirmed < limit) {
				pr.setStatus(ParticipationStatus.CONFIRMED);
				countConfirmed++;
				confirmedRequests.add(RequestMapper.toParticipationRequestDto(pr));
			} else {
				pr.setStatus(ParticipationStatus.REJECTED);
				rejectedRequests.add(RequestMapper.toParticipationRequestDto(pr));
			}
		}

		requestRepository.saveAll(requests);

		if (request.status().name().equals(ParticipationStatus.CONFIRMED.name()) && countConfirmed >= limit) {
			requestRepository.rejectPendingRequests(eventId, ParticipationStatus.PENDING);
		}

		return EventRequestStatusUpdateResult.builder()
				.confirmedRequests(confirmedRequests)
				.rejectedRequests(rejectedRequests)
				.build();
	}

	@Override
	public List<ParticipationRequestDto> findByRequesterId(Long userId) {
		return requestRepository.findByRequesterId(userId)
				.stream()
				.map(RequestMapper::toParticipationRequestDto)
				.toList();
	}

	@Override
	@Transactional
	public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
		checkUserExists(userId);
		EventDto event = getEventById(eventId);

		if (!event.published()) {
			throw new ConflictException("Нельзя участвовать в неопубликованном событии");
		}

		if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
			throw new ConflictException("Запрос уже существует");
		}

		if (event.initiatorId().equals(userId)) {
			throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
		}

		int limit = event.participantLimit() != null ? event.participantLimit() : 0;
		if (limit != 0) {
			long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

			if (event.requestModeration()) {
				long pendingCount = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.PENDING);
				if (confirmedCount + pendingCount >= limit) {
					throw new ConflictException("Достигнут лимит запросов на участие");
				}
			} else {
				if (confirmedCount >= limit) {
					throw new ConflictException("Достигнут лимит запросов на участие");
				}
			}
		}

		ParticipationStatus status;
		if (!event.requestModeration() || limit == 0) {
			status = ParticipationStatus.CONFIRMED;
		} else {
			status = ParticipationStatus.PENDING;
		}

		ParticipationRequest request = ParticipationRequest.builder()
				.requesterId(userId)
				.eventId(eventId)
				.status(status)
				.created(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
				.build();

		return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
	}

	@Override
	public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
		ParticipationRequest request = getRequestById(requestId);

		if (!request.getRequesterId().equals(userId)) {
			throw new ConflictException("Нельзя отменить чужую заявку");
		}
		request.setStatus(ParticipationStatus.CANCELED);
		return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
	}

	@Override
	public Map<Long, Long> countConfirmedRequestsByEventIds(List<Long> eventIds) {
		if (eventIds == null || eventIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<EventRequestCount> counts = requestRepository
				.countConfirmedRequestsByEventIds(eventIds, ParticipationStatus.CONFIRMED);

		return counts.stream()
				.collect(Collectors.toMap(
						EventRequestCount::getEventId,
						EventRequestCount::getCount
				));
	}

	private void checkUserExists(Long userId) {
		List<UserDto> users = userClient.getUsersByIds(List.of(userId));
		if (users.isEmpty()) {
			throw new NotFoundException("Пользователь с id=" + userId + " не найден");
		}
	}

	private EventDto getEventById(long eventId) {
		List<EventDto> events = eventClient.getEventsByIds(List.of(eventId));
		if (events.isEmpty()) {
			throw new NotFoundException("Событие с id=" + eventId + " не найдено");
		}
		return events.get(0);
	}

	@NonNull
	private ParticipationRequest getRequestById(Long requestId) {
		return requestRepository.findById(requestId)
				.orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));
	}
}