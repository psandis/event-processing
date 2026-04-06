package com.eventprocessing.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EventEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventEngineApplication.class, args);
    }
}
