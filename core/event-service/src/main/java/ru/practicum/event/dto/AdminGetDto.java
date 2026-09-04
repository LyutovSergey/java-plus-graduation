package ru.practicum.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@NotNull
@Builder
public record AdminGetDto(
		List<Integer> users,
		List<EventState> states,
		List<Integer> categories,
		LocalDateTime rangeStart,
		LocalDateTime rangeEnd,
		Integer from,
		Integer size
) {
}