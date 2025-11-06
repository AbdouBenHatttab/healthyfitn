package com.healthapp.shared;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SharedLibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SharedLibraryApplication.class, args);
        System.out.println("✅ Shared Library is running successfully!");
    }
}
