package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;

import java.time.LocalDateTime;

@Builder
public record EventFullDto(

		Long id,

		@NotBlank
		String annotation,

		@NotNull
		CategoryDto category,

		Long confirmedRequests,

		LocalDateTime createdOn,

		String description,

		@NotNull
		LocalDateTime eventDate,

		@NotNull
		Long initiator,

		@NotNull
		Location location,

		boolean paid,

		Integer participantLimit,

		LocalDateTime publishedOn,

		boolean requestModeration,

		EventState state,

		@NotBlank
		String title,

		Long views,

		long rate
) {
}