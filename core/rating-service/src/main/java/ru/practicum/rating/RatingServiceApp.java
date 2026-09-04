package ru.practicum.rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.common.client")
@ComponentScan(basePackages = {
		"ru.practicum.rating",
		"ru.practicum.common"
})
public class RatingServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(RatingServiceApp.class, args);
	}
}