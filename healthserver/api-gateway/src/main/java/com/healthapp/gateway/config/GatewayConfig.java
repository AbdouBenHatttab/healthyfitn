package com.healthapp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class GatewayConfig {

    /**
     * Filtre global pour logger les requêtes
     * Le Gateway ne valide PAS les JWT, il les transmet seulement
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            log.debug("🌐 Gateway: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI());

            // Transmettre l'Authorization header tel quel
            return chain.filter(exchange);
        };
    }
}