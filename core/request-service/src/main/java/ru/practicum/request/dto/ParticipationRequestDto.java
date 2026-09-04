package ru.practicum.request.dto;

import lombok.Builder;
import ru.practicum.request.model.ParticipationStatus;

@Builder
public record ParticipationRequestDto(
		Long id,
		String created,
		Long event,
		Long requester,
		ParticipationStatus status
) {
}
