package ru.practicum.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.ParticipationRequestDto;
import ru.practicum.common.model.EventRequestStatusUpdateRequest;
import ru.practicum.common.model.EventRequestStatusUpdateResult;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestClient {

	@PostMapping("/internal/requests/count/by-events")
	Map<Long, Long> getConfirmedRequestsByEventIds(@RequestBody List<Long> eventIds);

	@GetMapping("/internal/requests/event/{eventId}")
	List<ParticipationRequestDto> getRequestsByEventId(@PathVariable("eventId") Long eventId);

	@PatchMapping("/internal/requests/event/{eventId}")
	EventRequestStatusUpdateResult updateStatusRequest(@PathVariable("eventId") Long eventId,
													   @RequestBody EventRequestStatusUpdateRequest request);
}