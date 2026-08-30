package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.common.client")
@ComponentScan(basePackages = {
		"ru.practicum.request",
		"ru.practicum.common"
})
public class RequestServiceApp {
	public static void main(String[] args) {
		SpringApplication.run(RequestServiceApp.class, args);
	}
}