package ru.practicum.analyzer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import ru.practicum.analyzer.processor.EventSimilarityProcessor;
import ru.practicum.analyzer.processor.UserActionProcessor;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }

    @Bean
    public CommandLineRunner runAnalyzer(UserActionProcessor userActionProcessor,
                                         EventSimilarityProcessor similarityProcessor) {
        return args -> {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                userActionProcessor.shutdown();
                similarityProcessor.shutdown();
            }));

            Thread userActionThread = new Thread(userActionProcessor);
            userActionThread.setName("UserActionProcessorThread");
            userActionThread.start();

            similarityProcessor.start();
        };
    }
}