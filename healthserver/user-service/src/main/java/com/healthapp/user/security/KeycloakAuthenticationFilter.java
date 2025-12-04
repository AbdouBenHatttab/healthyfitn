package com.healthapp.user.security;

import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtre pour synchroniser les informations JWT Keycloak avec MongoDB
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            String keycloakId = jwt.getSubject();
            String email = jwt.getClaim("email");

            // Synchroniser avec MongoDB
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Créer un CustomUserPrincipal enrichi
                CustomUserPrincipal principal = CustomUserPrincipal.fromJwt(jwt, authentication.getAuthorities());
                principal.setId(user.getId()); // ID MongoDB
                principal.setKeycloakId(keycloakId);

                // Mettre à jour le contexte de sécurité
                JwtAuthenticationToken newAuth = new JwtAuthenticationToken(
                        jwt,
                        authentication.getAuthorities(),
                        principal.getEmail()
                );
                newAuth.setDetails(principal);

                SecurityContextHolder.getContext().setAuthentication(newAuth);

                log.debug("✅ User synchronized - MongoDB ID: {}, Keycloak ID: {}", user.getId(), keycloakId);
            } else {
                log.warn("⚠️ User not found in MongoDB: {} (Keycloak ID: {})", email, keycloakId);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/public/") ||
                path.startsWith("/actuator/") ||
                path.equals("/error");
    }
}