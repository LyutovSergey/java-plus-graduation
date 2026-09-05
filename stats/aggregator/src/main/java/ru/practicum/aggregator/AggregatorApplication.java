package ru.practicum.aggregator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import ru.practicum.aggregator.service.AggregationStarter;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class AggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregatorApplication.class, args);
    }

    @Bean
    public CommandLineRunner runKafkaAggregator(AggregationStarter aggregator) {
        return args -> {
            log.info("Контекст и веб-сервер готовы. Запускаем бесконечный цикл Кафки...");
            aggregator.start();
        };
    }
}