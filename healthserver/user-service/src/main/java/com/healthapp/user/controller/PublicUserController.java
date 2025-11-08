package com.healthapp.user.controller;

import com.healthapp.user.dto.response.ApiResponse;
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
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody Map<String, String> request) {
        
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Email is required"));
        }
        
        log.info("Password reset requested for user: {}", email);
        
        return ResponseEntity.ok(ApiResponse.success("Reset link sent to email", null));
    }
}