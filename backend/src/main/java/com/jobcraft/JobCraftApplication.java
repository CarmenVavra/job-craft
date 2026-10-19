package com.jobcraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Einstiegspunkt der JobCraft-Backend-Anwendung.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class JobCraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobCraftApplication.class, args);
    }
}
