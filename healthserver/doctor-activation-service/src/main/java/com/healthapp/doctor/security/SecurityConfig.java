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
                                    "/api/doctors/login",
                                    "/api/doctors/health",
                                    "/api/doctors/forgot-password",
                                    "/api/doctors/test",
                                    "/api/doctors/debug/**",
                                    "/api/doctors/available",                    // ✅ NEW
                                    "/api/doctors/appointments/from-patient",    // ✅ NEW
                                    "/api/doctors/appointments/patient/**",      // ✅ NEW
                                    "/actuator/**",
                                    "/api/public/**"
                            ).permitAll()
                // ✅ WebRTC API - Accessible aux deux rôles
                .requestMatchers("/api/webrtc/**")
                    .hasAnyRole("DOCTOR", "USER")
                
                            // Admin endpoints
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")

                            // Doctor endpoints - AUTHENTICATION REQUIRED
                            .requestMatchers("/api/doctors/appointments/**").hasRole("DOCTOR")
                            .requestMatchers(HttpMethod.PUT, "/api/doctors/change-password").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/doctors/profile").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/api/doctors/profile").authenticated()

                            // All other requests
                            .anyRequest().authenticated();
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class);

        log.info("✅ Security Filter Chain configured successfully");
        return http.build();
    }}