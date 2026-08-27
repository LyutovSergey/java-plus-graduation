package ru.practicum.rating.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.rating.dto.UserDto;

import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

	@PostMapping("/internal/users/by-ids")
	List<UserDto> getUsersByIds(@RequestBody List<Long> ids);
}