package ru.practicum.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
		"ru.practicum.common.client",
		"ru.practicum.stat.client"
})
@ComponentScan(basePackages = {
		"ru.practicum.event",
		"ru.practicum.stat.client",
		"ru.practicum.common"
})
public class EventServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(EventServiceApp.class, args);
	}
}