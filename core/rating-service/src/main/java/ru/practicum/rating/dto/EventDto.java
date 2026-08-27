package ru.practicum.rating.dto;

public record EventDto(
		Long id,
		Long initiatorId,
		Boolean published
) {
}