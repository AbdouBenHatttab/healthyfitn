package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.DoctorLoginRequest;
import com.healthapp.doctor.dto.request.DoctorRegisterRequest;
import com.healthapp.doctor.dto.response.AuthResponse;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.service.DoctorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Contrôleur d'authentification des médecins avec Keycloak
 *
 * ⚠️ CHANGEMENTS IMPORTANTS:
 * - /register : Toujours public (crée compte sans mot de passe)
 * - /login : SUPPRIMÉ - Géré par Keycloak
 * - /forgot-password : SUPPRIMÉ - Géré par Keycloak
 *
 * Flow d'inscription:
 * 1. Doctor s'inscrit via /register (sans mot de passe)
 * 2. Admin approuve via /api/admin/doctors/activate
 * 3. Doctor reçoit email de Keycloak pour définir son mot de passe
 * 4. Doctor se connecte via Keycloak (votre frontend redirige vers Keycloak)
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorAuthController {

    private final DoctorAuthService doctorAuthService;

    /**
     * Inscrire un nouveau médecin (endpoint PUBLIC)
     *
     * ✅ Aucune authentification requise
     * ✅ Crée le compte SANS mot de passe
     * ✅ Le mot de passe sera défini après activation par l'admin
     *
     * @param request Données d'inscription du médecin
     * @return DoctorResponse avec le statut de l'inscription
     */
    @PostMapping("/register")
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        log.info("🏥 Demande d'inscription d'un médecin reçue pour : {}", request.getEmail());

        DoctorResponse response = doctorAuthService.registerDoctor(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ❌ ENDPOINT SUPPRIMÉ: /login
     *
     * La connexion est maintenant gérée par Keycloak.
     *
     * Pour se connecter, le frontend doit:
     * 1. Rediriger vers Keycloak: http://localhost:8080/realms/health-app-realm/protocol/openid-connect/auth
     * 2. Keycloak gère l'authentification
     * 3. Keycloak redirige vers votre callback avec un code
     * 4. Échangez le code contre un token JWT
     *
     * Voir la documentation Keycloak OAuth2/OIDC pour plus de détails.
     */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody DoctorLoginRequest request) {
        log.info("🔐 Doctor login attempt: {}", request.getEmail());
        AuthResponse response = doctorAuthService.login(request);
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint de vérification de l'état du service
     *
     * @return Statut du service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "doctor-activation-service",
                "authentication", "Keycloak OAuth2",
                "message", "Service opérationnel"
        ));
    }

    /**
     * ✅ NOUVEAU: Endpoint d'information sur l'authentification
     */
    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        return ResponseEntity.ok(Map.of(
                "authProvider", "Keycloak",
                "realm", "health-app-realm",
                "authUrl", "http://localhost:8080/realms/health-app-realm/protocol/openid-connect/auth",
                "tokenUrl", "http://localhost:8080/realms/health-app-realm/protocol/openid-connect/token",
                "logoutUrl", "http://localhost:8080/realms/health-app-realm/protocol/openid-connect/logout",
                "passwordResetUrl", "http://localhost:8080/realms/health-app-realm/login-actions/reset-credentials",
                "registration", Map.of(
                        "endpoint", "/api/doctors/register",
                        "method", "POST",
                        "requiresPassword", false,
                        "note", "Password will be set after admin approval"
                )
        ));
    }
}