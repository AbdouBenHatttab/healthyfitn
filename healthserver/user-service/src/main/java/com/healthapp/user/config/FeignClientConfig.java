package com.healthapp.user.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Configuration Feign avec gestion d'erreurs pour Circuit Breaker
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();
                    String tokenValue = jwt.getTokenValue();

                    // Ne pas ajouter le token aux endpoints publics
                    if (!template.url().contains("/api/public/")) {
                        template.header("Authorization", "Bearer " + tokenValue);
                        log.debug("🔑 Token Keycloak ajouté à la requête Feign: {}",
                                template.url());
                    }
                } else {
                    log.debug("⚠️ Pas de token JWT disponible pour la requête Feign");
                }
            }
        };
    }

    /**
     * ✅ AJOUT: Error Decoder personnalisé pour Circuit Breaker
     *
     * Permet au Circuit Breaker de détecter correctement les erreurs
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("❌ Feign Error - Method: {}, Status: {}, Reason: {}",
                    methodKey, response.status(), response.reason());

            // ✅ Pour les erreurs 5xx et timeouts, déclencher le Circuit Breaker
            if (response.status() >= 500 || response.status() == 408) {
                log.warn("⚠️ Triggering Circuit Breaker for status: {}", response.status());
                return new RetryableException(
                        response.status(),
                        "Service unavailable: " + response.reason(),
                        response.request().httpMethod(),
                        (Long) null,
                        response.request()
                );
            }

            // Pour les autres erreurs, ne pas déclencher le Circuit Breaker
            return new ErrorDecoder.Default().decode(methodKey, response);
        };
    }
}