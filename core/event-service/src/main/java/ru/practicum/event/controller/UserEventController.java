package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.client.RequestClient;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;


import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
public class UserEventController {

	private final EventService eventService;

	/**
	 * Получение событий, добавленных текущим пользователем
	 *
	 * @param userId id текущего пользователя
	 * @param from   количество элементов, которые нужно пропустить для формирования текущего набора
	 *               Default value : 0
	 * @param size   количество элементов в наборе
	 *               Default value : 10
	 * @return List<{@link EventShortDto}>
	 */
	@GetMapping
	public List<EventShortDto> findEventsByUserId(@PathVariable @Positive Long userId,
												  @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
												  @RequestParam(defaultValue = "10") @Positive Integer size) {
		return eventService.findByUserId(userId, from, size);
	}

	/**
	 * Получение полной информации о событии добавленном текущим пользователем
	 *
	 * @param userId  id текущего пользователя
	 * @param eventId id события
	 * @return {@link EventFullDto}
	 */
	@GetMapping("/{eventId}")
	public EventFullDto findEventById(@PathVariable @Positive Long userId,
									  @PathVariable @Positive Long eventId) {
		return eventService.findEventById(userId, eventId);
	}

	/**
	 * Добавление нового события
	 * <p>
	 * Обратите внимание: дата и время на которые намечено событие не может быть раньше,
	 * чем через два часа от текущего момента
	 *
	 * @param userId      id текущего пользователя
	 * @param newEventDto данные добавляемого события
	 * @return {@link EventFullDto}
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventFullDto addEvent(@PathVariable @Positive Long userId,
	                             @RequestBody @Valid NewEventDto newEventDto) {

		return eventService.userAddNewEvent(userId, newEventDto);
	}

	/**
	 * Изменение события добавленного текущим пользователем
	 * <p>
	 * Обратите внимание:
	 * <p>
	 * - изменить можно только отмененные события или события в состоянии ожидания модерации (Ожидается код ошибки 409)
	 * <p>
	 * - дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента
	 * (Ожидается код ошибки 409)
	 *
	 * @param userId  id текущего пользователя
	 * @param eventId id редактируемого события
	 * @param request Данные HTTP-запроса
	 * @return {@link EventFullDto}
	 */
	@PatchMapping("/{eventId}")
	public EventFullDto patchEvent(@PathVariable @Positive Long userId,
	                               @PathVariable @Positive Long eventId,
	                               @RequestBody @Valid UpdateEventUserRequest request) {
		return eventService.patchEvent(userId, eventId, request);
	}
}

