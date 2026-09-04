package ru.practicum.common.dto;

public record EventDto(
		Long id,
		Long initiatorId,
		Boolean published,
		Integer participantLimit,
		Boolean requestModeration
) {
}