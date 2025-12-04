package com.healthapp.doctor.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

/**
 * Configuration de sécurité avec Keycloak OAuth2
 * ✅ Remplace JWT manuel par OAuth2 Resource Server
 * ✅ Extrait les rôles depuis les tokens Keycloak
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("🔐 SecurityConfig INITIALIZED (KEYCLOAK)");
        log.info("✅ Method security enabled");
        log.info("✅ OAuth2 Resource Server enabled");
        log.info("========================================");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔒 Configuring Security Filter Chain with Keycloak...");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> {
                    log.info("🔒 Configuring authorization rules");

                    auth
                            // ✅ CRITICAL: WebSocket paths MUST be permitAll FIRST
                            .requestMatchers("/ws/**").permitAll()
                            .requestMatchers("/ws/webrtc/**").permitAll()

                            // Public endpoints - NO AUTHENTICATION
                            .requestMatchers(
                                    "/api/doctors/register",
                                    "/api/doctors/login",// ✅ Enregistrement public
                                    "/api/doctors/health",
                                    "/api/doctors/test",
                                    "/api/doctors/debug/**",
                                    "/api/doctors/available",
                                    "/api/doctors/appointments/from-patient",
                                    "/api/doctors/forgot-password",
                                    "/api/doctors/appointments/patient/**",
                                    "/actuator/**",
                                    "/api/public/**"
                            ).permitAll()

                            // ❌ SUPPRIMÉ: /api/doctors/login (géré par Keycloak)
                            // ❌ SUPPRIMÉ: /api/doctors/forgot-password (géré par Keycloak)

                            // WebRTC API - Accessible aux deux rôles
                            .requestMatchers("/api/webrtc/**")
                            .hasAnyRole("DOCTOR", "USER")

                            // Admin endpoints
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")

                            // Doctor endpoints - AUTHENTICATION REQUIRED
                            .requestMatchers("/api/doctors/appointments/**").hasRole("DOCTOR")
                            .requestMatchers(HttpMethod.GET, "/api/doctors/profile").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/api/doctors/profile").authenticated()

                            // All other requests
                            .anyRequest().authenticated();
                })

                // ✅ Configuration OAuth2 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        log.info("✅ Security Filter Chain configured successfully with Keycloak OAuth2");
        return http.build();
    }

    /**
     * Convertisseur JWT pour extraire les rôles depuis Keycloak
     * ✅ Extrait les rôles depuis realm_access.roles
     * ✅ Ajoute le préfixe ROLE_ pour Spring Security
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || realmAccess.get("roles") == null) {
                return List.<org.springframework.security.core.GrantedAuthority>of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(java.util.stream.Collectors.toList());
        });

        return converter;
    }


}