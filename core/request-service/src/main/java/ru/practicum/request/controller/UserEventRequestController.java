package ru.practicum.request.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class UserEventRequestController {

	private final RequestService requestService;

	@GetMapping
	public List<ParticipationRequestDto> getRequests(@PathVariable @Positive Long userId,
													 @PathVariable @Positive Long eventId) {
		log.info("GET /users/{}/events/{}/requests", userId, eventId);
		return requestService.findByEventId(userId, eventId);
	}


	@PatchMapping
	public EventRequestStatusUpdateResult patchRequests(@PathVariable @Positive Long userId,
														@PathVariable @Positive Long eventId,
														@RequestBody @Valid EventRequestStatusUpdateRequest request) {
		log.info("PATCH /users/{}/events/{}/requests", userId, eventId);
		return requestService.updateStatusRequest(userId, eventId, request);
	}
}