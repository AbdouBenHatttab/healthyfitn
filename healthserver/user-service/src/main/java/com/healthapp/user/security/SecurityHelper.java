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
            // ✅ Tous ces claims viennent d'OIDC
            String keycloakId = jwt.getSubject();  // ✅ Utiliser keycloakId
            String emailInJwt = jwt.getClaim("email");

            // ✅ RECHERCHER PAR KEYCLOAK ID (qui ne change jamais)
            Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);

            if (userOpt.isEmpty()) {
                // ✅ Fallback : rechercher par email si keycloakId ne trouve rien
                log.warn("⚠️ User not found by keycloakId: {}, trying email: {}", keycloakId, emailInJwt);
                userOpt = userRepository.findByEmail(emailInJwt);
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                CustomUserPrincipal principal = CustomUserPrincipal.fromJwt(jwt, auth.getAuthorities());
                principal.setId(user.getId());
                principal.setKeycloakId(keycloakId);
                principal.setEmail(user.getEmail());  // ✅ Utiliser l'email de MongoDB (mis à jour)
                return principal;
            }

            log.warn("⚠️ User not found in MongoDB: keycloakId={}, email={}", keycloakId, emailInJwt);
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