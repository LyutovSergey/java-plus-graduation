package ru.practicum.rating.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.rating.dto.EventDto;

import java.util.List;

@FeignClient(name = "event-service", fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient {

	@PostMapping("/internal/events/by-ids")
	List<EventDto> getEventsByIds(@RequestBody List<Long> ids);

	@PostMapping("/internal/events/{eventId}/rate")
	void updateEventRate(@PathVariable("eventId") Long eventId, @RequestParam("rate") Long rate);
}