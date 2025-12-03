package com.healthapp.auth.controller;

import com.healthapp.auth.dto.request.LoginRequest;
import com.healthapp.auth.dto.request.RefreshTokenRequest;
import com.healthapp.auth.dto.request.RegisterRequest;
import com.healthapp.auth.dto.response.AuthResponse;
import com.healthapp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur d'authentification avec Keycloak
 *
 * ⚠️ CHANGEMENTS IMPORTANTS :
 * - Le login ne fait plus d'authentification locale, il demande un token à Keycloak
 * - Le register crée l'utilisateur dans Keycloak
 * - Le refresh utilise le refresh_token de Keycloak
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:4200", "healthapp://*"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Inscription d'un nouvel utilisateur
     *
     * Pour un USER/ADMIN : Compte activé immédiatement, token retourné
     * Pour un DOCTOR : Compte créé mais désactivé, en attente d'activation admin
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Demande d'inscription reçue pour: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        if (response.getAccessToken() == null) {
            // Médecin : compte créé mais pas de token (désactivé)
            log.info("👨‍⚕️ Médecin inscrit (en attente d'activation) : {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            // Utilisateur normal : compte activé et token généré
            log.info("✅ Utilisateur inscrit et connecté : {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }

    /**
     * Connexion d'un utilisateur
     *
     * Cette méthode demande un token à Keycloak avec les identifiants fournis.
     * Keycloak vérifie le mot de passe et retourne un access_token et refresh_token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Demande de connexion reçue pour: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        log.info("✅ Connexion réussie pour: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Rafraîchissement du token d'accès
     *
     * Utilise le refresh_token fourni par Keycloak pour obtenir un nouveau access_token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🔄 Demande de rafraîchissement de token reçue");

        AuthResponse response = authService.refreshToken(request.getRefreshToken());

        log.info("✅ Token rafraîchi avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * Déconnexion de l'utilisateur
     *
     * Révoque le refresh_token dans Keycloak.
     * Le frontend doit également supprimer le token de son côté.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🚪 Demande de déconnexion reçue");

        authService.logout(request.getRefreshToken());

        log.info("✅ Déconnexion réussie");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Déconnexion réussie"
        ));
    }

    /**
     * Endpoint de santé (health check)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-service",
                "auth_provider", "Keycloak",
                "message", "Service d'authentification fonctionnel"
        ));
    }
}