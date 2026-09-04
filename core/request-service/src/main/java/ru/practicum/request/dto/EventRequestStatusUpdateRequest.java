package ru.practicum.request.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import ru.practicum.request.model.RequestUpdateStatus;

import java.util.List;

@Builder
public record EventRequestStatusUpdateRequest(

		List<Long> requestIds,

		@NotNull
		RequestUpdateStatus status
) {
}
