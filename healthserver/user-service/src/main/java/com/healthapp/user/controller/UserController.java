package com.healthapp.user.controller;

import com.healthapp.user.client.DoctorServiceClient;
import com.healthapp.user.dto.request.UpdateUserRequest;
import com.healthapp.user.dto.request.ChangePasswordRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.UserResponse;
import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
import com.healthapp.user.security.SecurityHelper;
import com.healthapp.user.service.UserService;
import com.healthapp.user.service.PasswordService;
import com.healthapp.user.service.UserKeycloakSyncService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les utilisateurs authentifiés avec Keycloak
 *
 * ✅ CHANGEMENTS AVEC KEYCLOAK:
 * - Extraction de l'ID Keycloak depuis le JWT (authentication.getName() = sub claim)
 * - Recherche de l'utilisateur par keycloakId au lieu de email
 * - /forgot-password : Déclenche l'action Keycloak
 * - /change-password : Met à jour dans Keycloak (avec limitations)
 *
 * ⚠️ RECOMMANDATION:
 * Pour un changement de mot de passe complet, redirigez vers:
 * http://localhost:8080/realms/health-app-realm/account/password
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'ADMIN')")
@Slf4j
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final UserKeycloakSyncService keycloakSyncService;
    private final DoctorServiceClient doctorServiceClient;

    @Value("${keycloak.realm:health-app-realm}")
    private String keycloakRealm;

    @Value("${keycloak.serverUrl:http://localhost:8080}")
    private String keycloakServerUrl;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("✅ UserController INITIALIZED (KEYCLOAK)");
        log.info("✅ Base path: /api/v1/users");
        log.info("========================================");
    }

    /**
     * ENDPOINT DE TEST
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        log.info("🧪 Endpoint TEST appelé avec succès !");
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "UserController fonctionne correctement avec Keycloak !",
                "authentication", "Keycloak OAuth2",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * ENDPOINT DEBUG - Affiche tous les emails des utilisateurs
     */
    @GetMapping("/debug/all-emails")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllEmails() {
        List<User> allUsers = userRepository.findAll();

        Map<String, Object> debug = new HashMap<>();
        debug.put("totalUsers", allUsers.size());
        debug.put("users", allUsers.stream()
                .map(u -> Map.of(
                        "email", u.getEmail(),
                        "keycloakId", u.getKeycloakId() != null ? u.getKeycloakId() : "N/A",
                        "isActivated", u.getIsActivated(),
                        "roles", u.getRoles().toString()
                ))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(debug);
    }

    /**
     * ENDPOINT DEBUG - Affiche les informations du JWT
     */
    @GetMapping("/debug/jwt-info")
    public ResponseEntity<Map<String, Object>> getJwtInfo(Authentication authentication) {
        Map<String, Object> jwtInfo = new HashMap<>();

        jwtInfo.put("authenticationType", authentication.getClass().getSimpleName());
        jwtInfo.put("name", authentication.getName());
        jwtInfo.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            jwtInfo.put("subject", jwt.getSubject());
            jwtInfo.put("email", jwt.getClaim("email"));
            jwtInfo.put("preferredUsername", jwt.getClaim("preferred_username"));
            jwtInfo.put("givenName", jwt.getClaim("given_name"));
            jwtInfo.put("familyName", jwt.getClaim("family_name"));
        }

        return ResponseEntity.ok(jwtInfo);
    }

    /**
     * Récupérer le profil de l'utilisateur authentifié
     *
     * ✅ CORRECTION: Recherche par keycloakId au lieu de email
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(Authentication auth) {
        // ✅ Extraire l'ID Keycloak depuis le JWT
        String keycloakUserId = extractKeycloakUserIdAndEmail(auth).get("keycloakId");
        String email = extractKeycloakUserIdAndEmail(auth).get("email");

        log.info("🔍 [PROFIL] Recherche du profil utilisateur pour Keycloak ID : '{}'", keycloakUserId);

        // ✅ Rechercher par keycloakId
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur non trouvé pour Keycloak ID : {}", email);
                    return new RuntimeException("Utilisateur non trouvé pour cet utilisateur");
                });

        log.info("✅ [PROFIL] Utilisateur trouvé : id={}, email='{}', keycloakId='{}'",
                user.getId(), user.getEmail(), user.getKeycloakId());

        UserResponse userResponse = userService.mapToUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", userResponse));
    }

    /**
     * Mettre à jour le profil de l'utilisateur
     *
     * ✅ CORRECTION: Recherche par keycloakId au lieu de email
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication auth) {

        String keycloakUserId = extractKeycloakUserIdAndEmail(auth).get("keycloakId");
        String oldEmail = extractKeycloakUserIdAndEmail(auth).get("email");

        log.info("🔄 [MISE À JOUR] Mise à jour du profil pour Keycloak ID : '{}'", keycloakUserId);

        // ✅ Rechercher par email
        User user = userRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé pour cet utilisateur"));

        // ✅ VÉRIFIER SI L'EMAIL CHANGE
        if (request.getEmail() != null && !request.getEmail().equals(oldEmail)) {
            log.info("📧 Changement d'email détecté : {} -> {}", oldEmail, request.getEmail());

            // ✅ METTRE À JOUR LES RENDEZ-VOUS DANS LE DOCTOR SERVICE
            try {
                Map<String, String> updateResult = doctorServiceClient.updateAppointmentsPatientEmail(
                        oldEmail,
                        Map.of("newEmail", request.getEmail())  // ✅ Envoyer comme Map
                );

                log.info("✅ Appointments mis à jour : {}", updateResult.get("message"));
                log.info("✅ Nombre de rendez-vous mis à jour : {}", updateResult.get("updatedRecords"));

            } catch (Exception e) {
                log.error("❌ Erreur lors de la mise à jour des appointments : {}", e.getMessage(), e);
            }
        }

        // ✅ MISE À JOUR KEYCLOAK
        keycloakSyncService.updateUserInKeycloak(keycloakUserId, request);

        // Mise à jour MongoDB via UserService
        UserResponse updated = userService.updateUser(user.getId(), request);

        log.info("✅ [MISE À JOUR] Profil utilisateur mis à jour : {} ({})",
                user.getEmail(), user.getKeycloakId());

        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", updated));
    }

    /**
     * ⚠️ CHANGEMENT DE MOT DE PASSE AVEC KEYCLOAK
     *
     * LIMITATIONS:
     * - Impossible de vérifier l'ancien mot de passe via Admin API
     * - Le mot de passe est mis à jour directement dans Keycloak
     *
     * RECOMMANDATION:
     * Utilisez plutôt l'endpoint /password-change-url et redirigez vers Keycloak
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {

        log.info("========================================");
        log.info("🔐 PASSWORD CHANGE REQUEST (KEYCLOAK)");
        log.info("========================================");
        log.info("User: {}", auth.getName());

        log.warn("⚠️ LIMITATION: Current password verification not available with Keycloak Admin API");
        log.warn("⚠️ RECOMMENDATION: Use Keycloak Account Console for secure password change");

        try {
            // ✅ Extraire l'ID Keycloak depuis le JWT
            String keycloakUserId = extractKeycloakUserIdAndEmail(auth).get("keycloakId");
            String email = extractKeycloakUserIdAndEmail(auth).get("email");

            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                log.error("❌ Nouveau mot de passe manquant");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "error", "Le nouveau mot de passe est requis"
                        ));
            }

            // ✅ Rechercher par keycloakId
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé pour cet utilisateur"));

            // ⚠️ Le service changera le mot de passe dans Keycloak
            // mais ne pourra pas vérifier l'ancien mot de passe
            passwordService.changePassword(user.getId(), request);

            log.info("✅ Mot de passe changé avec succès dans Keycloak !");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe changé avec succès",
                    "note", "Password updated in Keycloak"
            ));

        } catch (RuntimeException e) {
            log.error("❌ Erreur mot de passe : {}", e.getMessage());

            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * ✅ NOUVEAU: Obtenir l'URL de changement de mot de passe Keycloak
     *
     * RECOMMANDÉ: Redirigez l'utilisateur vers cette URL pour un changement
     * de mot de passe sécurisé avec vérification de l'ancien mot de passe.
     */
    @GetMapping("/password-change-url")
    public ResponseEntity<Map<String, String>> getPasswordChangeUrl() {
        String url = String.format(
                "%s/realms/%s/account/password",
                keycloakServerUrl,
                keycloakRealm
        );

        return ResponseEntity.ok(Map.of(
                "url", url,
                "message", "Redirect user to this URL for secure password change",
                "note", "Keycloak will handle old password verification"
        ));
    }

    /**
     * Mettre à jour le score d'un utilisateur
     */
    @PutMapping("/{email}/score")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserScore(
            @PathVariable String email,
            @RequestBody Map<String, Double> request) {

        log.info("📊 Mise à jour du score pour l'utilisateur {}", email);

        if (!request.containsKey("score")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le score est requis"));
        }

        Double score = request.get("score");
        UserResponse updated = userService.updateUserScore(email, score);

        return ResponseEntity.ok(ApiResponse.success("Score mis à jour avec succès", updated));
    }

    /**
     * ✅ MÉTHODE UTILITAIRE: Extraire l'ID Keycloak depuis le JWT
     *
     * Le JWT Keycloak contient:
     * - sub (subject): L'ID utilisateur Keycloak (UUID)
     * - email: L'email de l'utilisateur
     * - preferred_username: Le nom d'utilisateur
     *
     * authentication.getName() retourne le "sub" (subject) qui est l'ID Keycloak
     */
    private Map<String, String> extractKeycloakUserIdAndEmail(Authentication authentication) {
        Map<String, String> result = new HashMap<>();

        // Keycloak ID = "sub" claim = authentication.getName()
        String keycloakUserId = authentication.getName();
        result.put("keycloakId", keycloakUserId);

        String email = null;
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            // ✅ OIDC Standard Claims
            email = jwt.getClaim("email");
        }
        result.put("email", email);

        log.debug("🔑 Extracted Keycloak User ID: {}", keycloakUserId);
        log.debug("📧 Extracted email from JWT: {}", email);

        return result;
    }

}