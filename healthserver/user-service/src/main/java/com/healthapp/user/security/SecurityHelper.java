package com.healthapp.user.security;

import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper pour extraire les informations utilisateur depuis le contexte de sécurité
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityHelper {

    private final UserRepository userRepository;

    /**
     * Extrait le CustomUserPrincipal depuis l'Authentication
     */
    public CustomUserPrincipal getPrincipal(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {

            // Vérifier si le principal est déjà un CustomUserPrincipal
            if (jwtAuth.getDetails() instanceof CustomUserPrincipal) {
                return (CustomUserPrincipal) jwtAuth.getDetails();
            }

            // Sinon, créer depuis JWT
            Jwt jwt = jwtAuth.getToken();
            String email = jwt.getClaim("email");

            // Récupérer depuis MongoDB
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                CustomUserPrincipal principal = CustomUserPrincipal.fromJwt(jwt, auth.getAuthorities());
                principal.setId(user.getId());
                principal.setKeycloakId(jwt.getSubject());
                return principal;
            }

            log.warn("⚠️ User not found in MongoDB: {}", email);
            return CustomUserPrincipal.fromJwt(jwt, auth.getAuthorities());
        }

        throw new IllegalStateException("Authentication is not a JWT token");
    }

    /**
     * Extrait l'ID utilisateur MongoDB
     */
    public String getUserId(Authentication auth) {
        return getPrincipal(auth).getId();
    }

    /**
     * Extrait l'email utilisateur
     */
    public String getUserEmail(Authentication auth) {
        return getPrincipal(auth).getEmail();
    }

    /**
     * Extrait le Keycloak ID
     */
    public String getKeycloakId(Authentication auth) {
        return getPrincipal(auth).getKeycloakId();
    }
}