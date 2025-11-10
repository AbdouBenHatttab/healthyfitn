package com.healthapp.doctor.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final JwtTokenValidator jwtTokenValidator;
    
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ SecurityConfig INITIALIZED");
        log.info("✅ Method security enabled");
        log.info("========================================");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔒 Configuring Security Filter Chain...");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                log.info("🔒 Configuring authorization rules:");
                log.info("  - Public: /api/doctors/register, /api/doctors/login, /api/doctors/health, /api/doctors/forgot-password");
                log.info("  - Public: /api/doctors/test, /api/doctors/debug/**");
                log.info("  - Authenticated: PUT /api/doctors/change-password");
                log.info("  - Authenticated: GET /api/doctors/profile");
                log.info("  - Admin: /api/admin/**");
                
                auth
                    // Public endpoints
                    .requestMatchers(
                        "/api/doctors/register",
                        "/api/doctors/login", 
                        "/api/doctors/health", 
                        "/api/doctors/forgot-password",
                        "/api/doctors/test",
                        "/api/doctors/debug/**",
                        "/actuator/**"
                    ).permitAll()
                    
                    // Admin endpoints - MUST BE BEFORE authenticated()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    
                    // Authenticated doctor endpoints
                    .requestMatchers(HttpMethod.PUT, "/api/doctors/change-password").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/doctors/profile").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/doctors/profile").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/doctors/activation-status").authenticated()
                    
                    // All other requests
                    .anyRequest().authenticated();
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class);
        
        log.info("✅ Security Filter Chain configured successfully");
        return http.build();
    }
}