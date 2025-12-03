package com.healthapp.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité Spring Security pour l'Auth Service
 *
 * Cette configuration :
 * 1. Désactive CSRF (car API REST stateless)
 * 2. Rend publics les endpoints d'authentification (/login, /register, etc.)
 * 3. Configure CORS pour le frontend
 * 4. Utilise une session stateless (pas de cookies de session)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF car nous utilisons des tokens JWT (API REST stateless)
                .csrf(csrf -> csrf.disable())

                // Configuration CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configuration des autorisations
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints publics (pas d'authentification requise)
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/health",
                                "/actuator/**",
                                "/error"
                        ).
                        permitAll()

                        // Tous les autres endpoints nécessitent une authentification
                        .anyRequest().authenticated()
                )

                // Désactiver la gestion de session (stateless - on utilise des tokens)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Désactiver la page de login par défaut
                .formLogin(form -> form.disable())

                // Désactiver l'authentification HTTP Basic
                .httpBasic(basic -> basic.disable())

                // Désactiver la redirection de logout
                .logout(logout -> logout.disable());

        return http.build();
    }

    /**
     * Configuration CORS pour autoriser les requêtes depuis le frontend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées (frontend Angular et mobile)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",      // Angular dev
                "http://localhost:8080",      // Frontend alternatif
                "healthapp://*"               // Application mobile
        ));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers exposés (pour que le frontend puisse les lire)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Autoriser les credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Durée de cache de la configuration CORS (en secondes)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}