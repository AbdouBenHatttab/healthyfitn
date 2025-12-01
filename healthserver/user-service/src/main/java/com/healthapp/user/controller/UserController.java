package com.healthapp.user.controller;

import com.healthapp.user.dto.request.UpdateUserRequest;
import com.healthapp.user.dto.request.ChangePasswordRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.UserResponse;
import com.healthapp.user.security.CustomUserPrincipal;
import com.healthapp.user.service.UserService;
import com.healthapp.user.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'ADMIN')")
@Slf4j
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(Authentication auth) {
        log.info("Récupération du profil utilisateur courant");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        UserResponse user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profil récupéré avec succès", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication auth) {

        log.info("Mise à jour du profil utilisateur");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        UserResponse updated = userService.updateUser(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", updated));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {

        log.info("Demande de changement de mot de passe");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        passwordService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès", null));
    }

    @PutMapping("/{email}/score")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserScore(
            @PathVariable String email,
            @RequestBody Map<String, Double> request) {

        log.info("Mise à jour du score pour l'utilisateur {}", email);

        if (!request.containsKey("score")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le score est requis"));
        }

        Double score = request.get("score");
        UserResponse updated = userService.updateUserScore(email, score);

        return ResponseEntity.ok(ApiResponse.success("Score mis à jour avec succès", updated));
    }
}
