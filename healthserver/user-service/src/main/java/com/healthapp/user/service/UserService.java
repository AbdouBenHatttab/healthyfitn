package com.healthapp.user.service;

//import com.healthapp.user.dto.request.ChangePasswordRequest;
import com.healthapp.user.client.DoctorServiceClient;
import com.healthapp.user.dto.request.UpdateUserRequest;
import com.healthapp.user.dto.request.UserSearchRequest;
import com.healthapp.user.dto.response.PageResponse;
import com.healthapp.user.dto.response.UserResponse;
//import com.healthapp.user.Enums.Gender;
//import com.healthapp.user.Enums.AccountStatus;
import com.healthapp.user.entity.User;
import com.healthapp.user.Enums.UserRole;
//import com.healthapp.user.exception.InvalidPasswordException;
import com.healthapp.user.exception.UserAlreadyExistsException;
import com.healthapp.user.exception.UserNotFoundException;
import com.healthapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    @Autowired
    private final KeycloakSyncService keycloakSyncService;
    @Autowired
    private DoctorServiceClient doctorServiceClient;

    public UserResponse getUserById(String userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return mapToUserResponse(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user);
    }
    
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    public PageResponse<UserResponse> searchUsers(UserSearchRequest request) {
        log.info("Searching users with criteria: {}", request);
        
        Pageable pageable = PageRequest.of(
                request.getPage(), 
                request.getSize(), 
                Sort.by("createdAt").descending()
        );
        
        Page<User> userPage;
        
        if (request.getEmail() != null || request.getFirstName() != null || request.getLastName() != null) {
            String keyword = request.getEmail() != null ?  request.getEmail() :
                           request.getFirstName() != null ? request.getFirstName() : 
                           request.getLastName();
            userPage = userRepository.searchUsers(keyword, pageable);
        } else if (request.getRole() != null) {
            List<User> users = userRepository.findByRolesContaining(request.getRole());
            userPage = new org.springframework.data.domain.PageImpl<>(users, pageable, users.size());
        } else {
            userPage = userRepository.findAll(pageable);
        }
        
        return PageResponse.<UserResponse>builder()
                .content(userPage.getContent().stream()
                        .map(this::mapToUserResponse)
                        .collect(Collectors.toList()))
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
            }

            // ✅ CORRECTION : Stocker l'ancien email AVANT de le changer
            String oldEmail = user.getEmail();
            String newEmail = request.getEmail();

            // Mettre à jour l'email de l'utilisateur
            user.setEmail(newEmail);

            // ✅ CORRECTION : Envoyer un Map au lieu de deux Strings
            try {
                Map<String, String> result = doctorServiceClient.updateAppointmentsPatientEmail(
                        oldEmail,
                        Map.of("newEmail", newEmail)  // ✅ Envoyer comme Map
                );
                log.info("✅ Appointments updated: {}", result.get("message"));
            } catch (Exception e) {
                log.error("❌ Failed to update appointments email: {}", e.getMessage());
                // Continuer quand même pour ne pas bloquer la mise à jour du profil
            }
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return mapToUserResponse(updatedUser);
    }

    // Modifiez la méthode deleteUser existante
    public void deleteUser(String userId) {
        log.info("========================================");
        log.info("🗑️ DELETING USER: {}", userId);
        log.info("========================================");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        log.info("📝 User found: {} ({})", user.getFullName(), user.getEmail());

        try {
            // ✅ STEP 1: Supprimer tous les rendez-vous du patient
            log.info("========================================");
            log.info("📅 STEP 1: Deleting all patient appointments");
            log.info("========================================");

            try {
                Map<String, Object> appointmentResult = doctorServiceClient.deletePatientAppointments(
                        user.getId(),      // Patient MongoDB ID
                        user.getEmail()    // Patient email (optionnel)
                );

                Object deletedCount = appointmentResult.get("deletedAppointments");
                log.info("✅ {} appointments deleted successfully", deletedCount);

            } catch (Exception e) {
                log.error("❌ Failed to delete appointments: {}", e.getMessage());
                log.warn("⚠️ Continuing with user deletion despite appointment deletion failure");
            }

            // ✅ STEP 2: Supprimer de Keycloak si l'utilisateur a un keycloakId
            if (user.getKeycloakId() != null && !user.getKeycloakId().isEmpty()) {
                log.info("========================================");
                log.info("🔐 STEP 2: Deleting from Keycloak");
                log.info("========================================");
                log.info("Keycloak User ID: {}", user.getKeycloakId());

                try {
                    keycloakSyncService.deleteUserFromKeycloak(user.getKeycloakId());
                    log.info("✅ User deleted from Keycloak");
                } catch (Exception e) {
                    log.warn("⚠️ Failed to delete from Keycloak (continuing with MongoDB deletion): {}", e.getMessage());
                }
            } else {
                log.warn("⚠️ No Keycloak User ID found, skipping Keycloak deletion");
            }

            // ✅ STEP 3: Supprimer de MongoDB
            log.info("========================================");
            log.info("📝 STEP 3: Deleting user from MongoDB");
            log.info("========================================");

            userRepository.delete(user);

            log.info("========================================");
            log.info("✅ USER DELETION COMPLETED");
            log.info("========================================");
            log.info("User: {} successfully deleted", user.getEmail());
            log.info("All associated appointments have been deleted");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Failed to delete user", e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }
    
    public List<UserResponse> getUsersByRole(UserRole role) {
        log.info("Fetching users by role: {}", role);
        return userRepository.findByRolesContaining(role).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRolesContaining(role);
    }
    
    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .score(user.getScore())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles())
                .accountStatus(user.getAccountStatus())
                .isEmailVerified(user.getIsEmailVerified())
                .isActivated(user.getIsActivated())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .medicalLicenseNumber(user.getMedicalLicenseNumber())
                .specialization(user.getSpecialization())
                .hospitalAffiliation(user.getHospitalAffiliation())
                .yearsOfExperience(user.getYearsOfExperience())
                .activationDate(user.getActivationDate())
                .build();
    }

    public UserResponse updateUserScore(String email, Double score) {
        log.info("Setting score {} for user {}", score, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + email));

        user.setScore(score);

        User saved = userRepository.save(user);

        return mapToUserResponse(saved);
    }

}