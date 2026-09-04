package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import ru.practicum.common.model.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "requests")
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest extends BaseEntity {

	@Column(name = "created")
	private LocalDateTime created;

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(name = "requester_id", nullable = false)
	private Long requesterId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ParticipationStatus status;
}