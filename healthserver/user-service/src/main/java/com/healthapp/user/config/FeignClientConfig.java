package com.healthapp.user.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
}