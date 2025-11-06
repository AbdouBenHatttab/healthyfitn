package com.healthapp.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.healthapp.user.repository")
public class MongoConfig {
    // MongoDB configuration is auto-configured by Spring Boot
}
