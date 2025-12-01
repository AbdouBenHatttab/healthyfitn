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

/**
 * Contrôleur d'authentification - Gestion des comptes utilisateurs
 * Endpoints publics pour inscription, connexion, rafraîchissement de token et déconnexion
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Inscription d'un nouvel utilisateur
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Demande d'inscription reçue pour: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("✅ Inscription réussie pour: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Connexion d'un utilisateur
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
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🔄 Demande de rafraîchissement de token reçue");
        AuthResponse response = authService.refreshToken(request);
        log.info("✅ Token rafraîchi avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * Déconnexion de l'utilisateur (invalide le refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🚪 Demande de déconnexion reçue");
        authService.logout(request.getRefreshToken());
        log.info("✅ Déconnexion réussie");
        return ResponseEntity.ok().build();
    }
}
