package ru.practicum.request.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@Transactional
public interface RequestService {

	List<ParticipationRequestDto> findByEventId(Long userId, Long eventId);

	EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
													   EventRequestStatusUpdateRequest request);

	List<ParticipationRequestDto> findByRequesterId(Long userId);

	ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

	ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);

	Map<Long, Long> countConfirmedRequestsByEventIds(List<Long> eventIds);
}