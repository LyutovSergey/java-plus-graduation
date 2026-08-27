package ru.practicum.rating.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.rating.dto.EventDto;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {

	@Override
	public EventClient create(Throwable cause) {
		return new EventClient() {

			@Override
			public List<EventDto> getEventsByIds(List<Long> ids) {
				if (cause instanceof FeignException feignEx && feignEx.status() == 404) {
					log.warn("События не найдены для ID: {}", ids);
					return List.of();
				}

				log.warn("event-service недоступен, возвращаем заглушки для ID: {}", ids);
				return ids.stream()
						.map(id -> new EventDto(id, null, false))
						.collect(Collectors.toList());
			}

			@Override
			public void updateEventRate(Long eventId, Long rate) {
				log.warn("event-service недоступен, рейтинг не обновлён для события {}", eventId);
			}
		};
	}
}