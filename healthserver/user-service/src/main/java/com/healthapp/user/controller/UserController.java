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
        log.info("Getting current user profile");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        UserResponse user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", user));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication auth) {
        
        log.info("Updating user profile");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        UserResponse updated = userService.updateUser(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        
        log.info("Change password request");
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        passwordService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @PutMapping("/{email}/score")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserScore(
            @PathVariable String email,
            @RequestBody Map<String, Double> request) {

        log.info("Updating score for user {}", email);

        if (!request.containsKey("score")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Score is required"));
        }

        Double score = request.get("score");
        UserResponse updated = userService.updateUserScore(email, score);

        return ResponseEntity.ok(ApiResponse.success("Score updated", updated));
    }


}
