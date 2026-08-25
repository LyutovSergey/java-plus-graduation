package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStatsDto;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class StatClient {

	private final RestTemplate rest;
	private final DiscoveryClient discoveryClient;
	private final String statsServiceId = "stats-server";
	private final RetryTemplate retryTemplate;


	public StatClient(RestTemplateBuilder builder, DiscoveryClient discoveryClient) {

		this.rest = builder.build();
		this.discoveryClient = discoveryClient;

		this.retryTemplate = new RetryTemplate();

		FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
		fixedBackOffPolicy.setBackOffPeriod(3000L);
		this.retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

		MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
		retryPolicy.setMaxAttempts(10);
		this.retryTemplate.setRetryPolicy(retryPolicy);
	}

	private ServiceInstance getInstance() {
		try {
			return discoveryClient
					.getInstances(statsServiceId)
					.getFirst();
		} catch (Exception exception) {
			throw new RuntimeException(
					"Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
					exception
			);
		}
	}

	private URI makeUri(String path) {
		ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
		return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
	}

	public void hit(EndpointHitDto endpointHitDto) {
		try {
			URI uri = makeUri("/hit");
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(endpointHitDto, headers);

			rest.exchange(
					uri,
					HttpMethod.POST,
					requestEntity,
					Void.class
			);
		} catch (Exception e) {
			log.error("Ошибка записи: {}", endpointHitDto, e);
		}

	}
	public List<ViewStatsDto> getStat(StatsRequest statsRequest) {
		try {
			URI baseUri = makeUri("/stats");
			StringBuilder urlBuilder = new StringBuilder(baseUri.toString())
					.append("?start={start}&end={end}&unique={unique}");
			List<String> uris = statsRequest.getUris();
			if (uris != null && !uris.isEmpty()) {
				urlBuilder.append("&uris={uris}");
			}

			String urlTemplate = urlBuilder.toString();

			java.util.Map<String, Object> params = new java.util.HashMap<>();
			params.put("start", statsRequest.getStart()); // Сюда уйдет чистая строка "2020-05-05 00:00:00" с пробелом
			params.put("end", statsRequest.getEnd());
			params.put("unique", statsRequest.getUnique());
			if (uris != null && !uris.isEmpty()) {
				params.put("uris", String.join(",", uris));
			}

			return rest.exchange(
					urlTemplate,
					HttpMethod.GET,
					null,
					new ParameterizedTypeReference<List<ViewStatsDto>>() {},
					params
			).getBody();

		} catch (Exception e) {
			log.error("Ошибка чтения статистики: {}", statsRequest, e);
			return null;
		}
	}



}
