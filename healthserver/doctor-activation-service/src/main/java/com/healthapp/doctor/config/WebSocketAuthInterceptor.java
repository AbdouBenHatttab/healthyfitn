package com.healthapp.doctor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket Authentication Interceptor avec Keycloak
 * ✅ Valide les tokens JWT Keycloak
 * ✅ Extrait les rôles depuis realm_access.roles
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder; // ✅ Injecté par Spring Boot (OAuth2 Resource Server)

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        log.info("═══════════════════════════════════════");
        log.info("🔌 WebSocket Handshake Starting (KEYCLOAK)");
        log.info("═══════════════════════════════════════");
        log.info("   Request URI: {}", request.getURI());

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 🔑 Extract token from query parameter
            String token = servletRequest.getServletRequest().getParameter("token");
            String userId = servletRequest.getServletRequest().getParameter("userId");

            log.info("   User ID: {}", userId);
            log.info("   Token present: {}", token != null && !token.isEmpty());

            if (token != null && !token.isEmpty()) {
                try {
                    // ✅ Validate JWT token using Keycloak's JwtDecoder
                    Jwt jwt = jwtDecoder.decode(token);

                    // Extract email (preferred_username)
                    String email = jwt.getClaimAsString("preferred_username");
                    if (email == null) {
                        email = jwt.getClaimAsString("email");
                    }

                    // Extract roles from realm_access.roles
                    List<String> roles = extractRoles(jwt);

                    log.info("✅ Token validated: {}", email);
                    log.info("   Roles: {}", roles);

                    // Store user info in WebSocket session attributes
                    attributes.put("email", email);
                    attributes.put("userId", userId);
                    attributes.put("roles", roles);
                    attributes.put("authenticated", true);

                    // Set security context
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("✅ WebSocket authentication successful (Keycloak)");
                    log.info("═══════════════════════════════════════");
                    return true;

                } catch (Exception e) {
                    log.error("❌ Token validation failed", e);
                    log.error("   Error type: {}", e.getClass().getSimpleName());
                    log.error("   Error message: {}", e.getMessage());
                }
            } else {
                log.error("❌ No token provided in WebSocket URL");
            }
        }

        log.error("❌ WebSocket authentication failed");
        log.info("═══════════════════════════════════════");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.error("❌ WebSocket handshake error", exception);
        } else {
            log.info("✅ WebSocket handshake completed successfully");
        }
    }

    /**
     * Extract roles from Keycloak JWT token
     * Roles are stored in: realm_access.roles
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Extract realm_access claim
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Object rolesObj = realmAccess.get("roles");

                if (rolesObj instanceof List) {
                    return (List<String>) rolesObj;
                }
            }

            log.warn("⚠️ No roles found in token");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("❌ Error extracting roles from token", e);
            return new ArrayList<>();
        }
    }
}