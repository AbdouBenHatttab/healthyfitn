package com.healthapp.user.controller;

//import com.healthapp.user.dto.request.UpdateUserRequest;
import com.healthapp.user.dto.request.UserSearchRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.PageResponse;
import com.healthapp.user.dto.response.UserResponse;
import com.healthapp.user.Enums.UserRole;
import com.healthapp.user.service.UserService;
//import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.info("Admin requesting all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestBody UserSearchRequest request) {
        
        log.info("Admin searching users with criteria: {}", request);
        PageResponse<UserResponse> result = userService.searchUsers(request);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        log.info("Admin requesting user: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        log.info("Admin deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserRole role) {
        log.info("Admin requesting users by role: {}", role);
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserStatistics() {
        log.info("Admin requesting user statistics");
        
        Map<String, Long> statistics = Map.of(
                "totalUsers", userService.countUsersByRole(UserRole.USER),
                "totalDoctors", userService.countUsersByRole(UserRole.DOCTOR),
                "totalAdmins", userService.countUsersByRole(UserRole.ADMIN)
        );
        
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));
    }
}