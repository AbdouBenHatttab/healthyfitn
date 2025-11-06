package com.healthapp.doctor.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing  // ✅ IMPORTANT: Active les timestamps
@EnableMongoRepositories(basePackages = "com.healthapp.doctor.repository")
public class MongoConfig {
    // MongoDB configuration is auto-configured by Spring Boot
}