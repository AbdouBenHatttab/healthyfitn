package com.healthapp.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.healthapp.auth.repository")
public class MongoConfig {
    // MongoDB configuration auto-configured by Spring Boot
}