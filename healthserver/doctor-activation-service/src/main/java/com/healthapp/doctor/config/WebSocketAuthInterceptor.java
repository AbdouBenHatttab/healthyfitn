package com.healthapp.doctor.config;

import com.healthapp.doctor.security.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenValidator JwtTokenValidator;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        log.info("═══════════════════════════════════════");
        log.info("🔌 WebSocket Handshake Starting");
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
                    // ✅ Validate JWT token
                    if (JwtTokenValidator.validateToken(token)) {
                        String email = JwtTokenValidator.getEmailFromToken(token);
                        List<String> roles = JwtTokenValidator.getRolesFromToken(token);
                        
                        log.info("✅ Token validated: {}", email);
                        log.info("   Roles: {}", roles);

                        // Store user info in WebSocket session attributes
                        attributes.put("email", email);
                        attributes.put("userId", userId);
                        attributes.put("roles", roles);
                        attributes.put("authenticated", true);

                        // Set security context
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                        
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.info("✅ WebSocket authentication successful");
                        log.info("═══════════════════════════════════════");
                        return true;
                    } else {
                        log.error("❌ Invalid token");
                    }
                } catch (Exception e) {
                    log.error("❌ Token validation failed", e);
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
}