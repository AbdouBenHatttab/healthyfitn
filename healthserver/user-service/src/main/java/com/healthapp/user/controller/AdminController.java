package com.healthapp.user.controller;

import com.healthapp.user.dto.request.UserSearchRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.PageResponse;
import com.healthapp.user.dto.response.UserResponse;
import com.healthapp.user.Enums.UserRole;
import com.healthapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Admin demande tous les utilisateurs");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Liste des utilisateurs récupérée avec succès", users));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestBody UserSearchRequest request) {
        try {
            log.info("Admin recherche des utilisateurs avec les critères : {}", request);
            PageResponse<UserResponse> result = userService.searchUsers(request);
            return ResponseEntity.ok(ApiResponse.success("Recherche effectuée avec succès", result));
        } catch (Exception e) {
            log.error("Erreur lors de la recherche des utilisateurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Une erreur est survenue"));
        }
    }


    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        log.info("Admin demande l'utilisateur : {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur récupéré avec succès", user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        log.info("Admin supprime l'utilisateur : {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé avec succès", null));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserRole role) {
        log.info("Admin demande les utilisateurs par rôle : {}", role);
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés avec succès", users));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserStatistics() {
        log.info("Admin demande les statistiques des utilisateurs");

        Map<String, Long> statistics = Map.of(
                "totalUsers", userService.countUsersByRole(UserRole.USER),
                "totalDoctors", userService.countUsersByRole(UserRole.DOCTOR),
                "totalAdmins", userService.countUsersByRole(UserRole.ADMIN)
        );

        return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées avec succès", statistics));
    }
}
