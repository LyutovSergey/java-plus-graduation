package ru.practicum.event.dto;

import lombok.Builder;
import ru.practicum.event.model.ParticipationStatus;

@Builder
public record ParticipationRequestDto(
		Long id,
		String created,
		Long event,
		Long requester,
		ParticipationStatus status
) {
}
