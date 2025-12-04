package com.healthapp.user.controller;

import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur public pour les opérations sans authentification
 */
@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class PublicUserController {

    private final PasswordResetService passwordResetService;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    /**
     * Mot de passe oublié - Déclenche l'action Keycloak
     *
     * ✅ AVEC KEYCLOAK:
     * - Keycloak envoie automatiquement l'email de réinitialisation
     * - Pas besoin de gérer les tokens manuellement
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "L'email est requis"
                    ));
        }

        log.info("========================================");
        log.info("🔐 PASSWORD RESET REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("Email: {}", email);

        try {
            passwordResetService.sendPasswordResetEmailForUser(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si l'email existe, un lien de réinitialisation sera envoyé par Keycloak",
                    "provider", "Keycloak"
            ));

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email de réinitialisation : {}", e.getMessage());

            // ⚠️ NE PAS révéler si l'email existe ou pas (sécurité)
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si l'email existe, un lien de réinitialisation sera envoyé"
            ));
        }
    }

    /**
     * ✅ NOUVEAU: Obtenir l'URL de réinitialisation de mot de passe Keycloak
     */
    @GetMapping("/password-reset-url")
    public ResponseEntity<Map<String, String>> getPasswordResetUrl() {
        String url = String.format(
                "%s/realms/%s/login-actions/reset-credentials",
                keycloakServerUrl,
                keycloakRealm
        );

        return ResponseEntity.ok(Map.of(
                "url", url,
                "message", "Redirect user to this URL for password reset",
                "note", "User will receive an email from Keycloak"
        ));
    }
}