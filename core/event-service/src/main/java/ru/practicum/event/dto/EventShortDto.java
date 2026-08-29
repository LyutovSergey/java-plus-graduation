package ru.practicum.event.dto;

import lombok.Builder;


import java.time.LocalDateTime;

@Builder
public record EventShortDto(
		long id,
		String annotation,
		CategoryDto category,
		long confirmedRequests,
		LocalDateTime eventDate,
		Long initiatorId,
		boolean paid,
		String title,
		long views,
		long rate
) {
}
