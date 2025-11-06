package com.healthapp.doctor.security;

import com.healthapp.shared.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenValidator extends OncePerRequestFilter {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("🔍 Processing request to: {}", path);
        
        // Check for API Gateway headers first
        String userId = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");
        
        if (userId != null && userEmail != null) {
            // Request came through API Gateway
            log.info("✅ Gateway authentication: user={}, roles={}", userEmail, rolesHeader);
            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // Direct request - validate JWT token
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    
                    log.debug("🔍 Validating JWT token...");
                    
                    if (JwtUtil.validateToken(token, jwtSecret)) {
                        // Extract claims manually using extractAllClaims
                        Claims claims = JwtUtil.extractAllClaims(token, jwtSecret);
                        
                        String email = claims.getSubject();
                        
                        // Extract userId - try both formats
                        String userIdFromToken = claims.get("userId", String.class);
                        if (userIdFromToken == null) {
                            userIdFromToken = claims.get("user_id", String.class);
                        }
                        
                        // Extract roles
                        @SuppressWarnings("unchecked")
                        List<String> roles = claims.get("roles", List.class);
                        
                        if (roles == null || roles.isEmpty()) {
                            log.warn("⚠️ No roles found in token for user: {}", email);
                            roles = new ArrayList<>();
                        }
                        
                        log.info("✅ Token validated: user={}, userId={}, roles={}", 
                                email, userIdFromToken, roles);
                        
                        // Convert roles to authorities with ROLE_ prefix
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> {
                                if (role.startsWith("ROLE_")) {
                                    return new SimpleGrantedAuthority(role);
                                } else {
                                    return new SimpleGrantedAuthority("ROLE_" + role);
                                }
                            })
                            .collect(Collectors.toList());
                        
                        // Use email as principal
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.info("✅ User {} authenticated with authorities: {}", email, authorities);
                    } else {
                        log.error("❌ JWT validation failed - invalid or expired token");
                        sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                                "Invalid or expired token");
                        return;
                    }
                    
                } catch (Exception e) {
                    log.error("❌ JWT validation error: {}", e.getMessage(), e);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                            "Invalid or expired token: " + e.getMessage());
                    return;
                }
            } else {
                log.debug("⚠️ No Authorization header found for path: {}", path);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Send JSON error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"error\": \"%s\", \"status\": %d, \"timestamp\": \"%s\"}", 
            message, status, java.time.LocalDateTime.now()
        ));
    }
    
    /**
     * Parse roles from header
     */
    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return List.of();
        }
        
        return List.of(rolesHeader.replace("[", "").replace("]", "").split(","))
                .stream()
                .map(String::trim)
                .map(role -> {
                    if (!role.startsWith("ROLE_")) {
                        return new SimpleGrantedAuthority("ROLE_" + role);
                    }
                    return new SimpleGrantedAuthority(role);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/api/doctors/register") ||
               path.startsWith("/api/doctors/login") ||
               path.startsWith("/api/doctors/health") ||
               path.startsWith("/actuator");
        
        if (skip) {
            log.debug("⏭️ Skipping JWT validation for public path: {}", path);
        }
        
        return skip;
    }
}