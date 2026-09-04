package ru.practicum.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestClient {

	@PostMapping("/internal/requests/count/by-events")
	Map<Long, Long> getConfirmedRequestsByEventIds(@RequestBody List<Long> eventIds);

}