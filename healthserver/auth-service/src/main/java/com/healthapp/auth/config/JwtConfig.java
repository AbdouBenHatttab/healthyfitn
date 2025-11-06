package com.healthapp.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret;
    private long accessTokenExpiration = 900000;
    private long refreshTokenExpiration = 604800000;
    private String issuer = "health-app";
}
