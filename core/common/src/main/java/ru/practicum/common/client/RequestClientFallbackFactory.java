package ru.practicum.common.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

	@Override
	public RequestClient create(Throwable cause) {
		return new RequestClient() {

			@Override
			public Map<Long, Long> getConfirmedRequestsByEventIds(List<Long> eventIds) {
				if (cause instanceof FeignException feignEx && feignEx.status() == 404) {
					log.warn("Запросы не найдены для событий: {}", eventIds);
					return new HashMap<>();
				}

				log.warn("request-service недоступен, возвращаем заглушки для событий: {}", eventIds);

				Map<Long, Long> fallback = new HashMap<>();
				if (eventIds != null) {
					eventIds.forEach(id -> fallback.put(id, 0L));
				}
				return fallback;
			}
		};
	}
}