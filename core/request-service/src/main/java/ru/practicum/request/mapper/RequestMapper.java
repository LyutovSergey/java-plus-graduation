package ru.practicum.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Component
public class RequestMapper {

	public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest participationRequest) {
		if (participationRequest == null) return null;
		return ParticipationRequestDto.builder()
				.event(participationRequest.getEventId())
				.requester(participationRequest.getRequesterId())
				.status(participationRequest.getStatus())
				.created(formatDateTime(participationRequest.getCreated()))
				.id(participationRequest.getId())
				.build();
	}

	private static String formatDateTime(LocalDateTime dateTime) {
		if (dateTime == null) return null;

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
		return dateTime.format(formatter);
	}
}
