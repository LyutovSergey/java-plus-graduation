package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

	private final UserService userService;

	/**
	 * Получение пользователей по списку ID (для межсервисного взаимодействия)
	 *
	 * @param ids список ID пользователей
	 * @return List<UserDto>
	 */
	@PostMapping("/by-ids")
	public List<UserDto> getUsersByIds(@RequestBody List<Long> ids) {
		return userService.getUsersByIds(ids);
	}
}