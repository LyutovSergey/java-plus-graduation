package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.EventDto;
import ru.practicum.event.service.EventService;
import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {

	private final EventService eventService;

	@PostMapping("/by-ids")
	public List<EventDto> getEventsByIds(@RequestBody List<Long> ids) {
		return eventService.getEventsByIds(ids);
	}

	@PostMapping("/{eventId}/rate")
	public void updateEventRate(@PathVariable Long eventId, @RequestParam Long rate) {
		eventService.updateEventRate(eventId, rate);
	}
}