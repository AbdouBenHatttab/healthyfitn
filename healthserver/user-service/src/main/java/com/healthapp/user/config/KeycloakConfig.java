package com.healthapp.user.config;

import lombok.Data;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {

    private String serverUrl;
    private String realm;
    private AdminConfig admin;
    private RolesConfig roles;

    @Data
    public static class AdminConfig {
        private String realm;
        private String username;
        private String password;
        private String clientId;
    }

    @Data
    public static class RolesConfig {
        private String user;
        private String doctor;
        private String admin;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(admin.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(admin.getClientId())
                .username(admin.getUsername())
                .password(admin.getPassword())
                .build();
    }
}