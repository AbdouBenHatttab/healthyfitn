//package com.healthapp.gateway.filter;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
////import org.springframework.web.server.ServerWebExchange;
//
//import javax.crypto.SecretKey;
//import java.util.List;
//
//@Component
//@Slf4j
//public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
//
//    @Value("${app.jwt.secret}")
//    private String jwtSecret;
//
//    public JwtAuthenticationFilter() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//
//            // Skip JWT validation for auth endpoints
//            if (isAuthEndpoint(request.getURI().getPath())) {
//                return chain.filter(exchange);
//            }
//
//            // Extract JWT token
//            String token = extractToken(request);
//            if (token == null) {
//                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                return exchange.getResponse().setComplete();
//            }
//
//            try {
//                Claims claims = validateToken(token);
//                ServerHttpRequest modifiedRequest = request.mutate()
//                        .header("X-User-Id", claims.get("user_id", String.class))
//                        .header("X-User-Email", claims.get("email", String.class))
//                        .build();
//
//                return chain.filter(exchange.mutate().request(modifiedRequest).build());
//            } catch (Exception e) {
//                log.error("JWT validation failed: {}", e.getMessage());
//                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                return exchange.getResponse().setComplete();
//            }
//        };
//    }
//
//    private boolean isAuthEndpoint(String path) {
//        List<String> publicPaths = List.of(
//            "/api/v1/auth/register",
//            "/api/v1/auth/login",
//            "/api/v1/auth/refresh"
//        );
//        return publicPaths.stream().anyMatch(path::startsWith);
//    }
//
//    private String extractToken(ServerHttpRequest request) {
//        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//        return null;
//    }
//
//    private Claims validateToken(String token) {
//        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
//        return Jwts.parser()
//                .verifyWith(key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//    }
//
//    public static class Config {
//        // Configuration properties if needed
//    }
//}
