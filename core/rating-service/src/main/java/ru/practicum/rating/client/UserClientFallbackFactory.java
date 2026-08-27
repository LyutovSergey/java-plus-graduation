package ru.practicum.rating.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.rating.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

	@Override
	public UserClient create(Throwable cause) {
		return new UserClient() {

			@Override
			public List<UserDto> getUsersByIds(List<Long> ids) {
				if (cause instanceof FeignException feignEx && feignEx.status() == 404) {
					log.warn("Пользователи не найдены для ID: {}", ids);
					return List.of();
				}

				log.warn("user-service недоступен, возвращаем заглушки для ID: {}", ids);
				return ids.stream()
						.map(id -> new UserDto(id, "Unknown User", "unknown@example.com"))
						.collect(Collectors.toList());
			}
		};
	}
}