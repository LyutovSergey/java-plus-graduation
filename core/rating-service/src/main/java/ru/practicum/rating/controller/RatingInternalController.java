package ru.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.rating.service.RatingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/ratings")
@RequiredArgsConstructor
public class RatingInternalController {

	private final RatingService ratingService;

	@PostMapping("/by-events")
	public Map<Long, Long> getRatingsByEventIds(@RequestBody List<Long> eventIds) {
		return ratingService.getRatingsByEventIds(eventIds);
	}
}