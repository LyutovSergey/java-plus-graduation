package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request.service.RequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {

	private final RequestService requestService;

	@PostMapping("/count/by-events")
	public Map<Long, Long> countConfirmedRequestsByEventIds(@RequestBody List<Long> eventIds) {
		return requestService.countConfirmedRequestsByEventIds(eventIds);
	}
}