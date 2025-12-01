package com.healthapp.user.controller;

import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class PublicUserController {

    private final PasswordResetService passwordResetService;

    /**
     * ✅ Mot de passe oublié - Envoie réellement l'email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("L'email est requis"));
        }

        log.info("🔐 Réinitialisation du mot de passe demandée pour l'utilisateur : {}", email);

        try {
            // ✅ Appeler le service pour envoyer l'email
            passwordResetService.sendPasswordResetEmailForUser(email);

            return ResponseEntity.ok(
                    ApiResponse.success("Email de réinitialisation du mot de passe envoyé avec succès", null)
            );

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email de réinitialisation : {}", e.getMessage());

            // ⚠️ NE PAS révéler si l'email existe ou pas (sécurité)
            return ResponseEntity.ok(
                    ApiResponse.success("Si l'email existe, un lien de réinitialisation sera envoyé", null)
            );
        }
    }
}
