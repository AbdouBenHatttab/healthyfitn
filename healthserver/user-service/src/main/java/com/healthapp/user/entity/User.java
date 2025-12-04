package com.healthapp.user.entity;

import com.healthapp.user.Enums.AccountStatus;
import com.healthapp.user.Enums.Gender;
import com.healthapp.user.Enums.UserRole;
import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    // 🔐 Keycloak Integration
    @Indexed(unique = true)
    private String keycloakId;  // Keycloak user ID (sub claim)

    @Indexed(unique = true)
    private String email;

    // ⚠️ Password is managed by Keycloak - removed from MongoDB
    // private String password;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;

    @Builder.Default
    private Gender gender = Gender.MALE;

    private String phoneNumber;

    @Nullable
    private Double score;

    private String profilePictureUrl;

    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Builder.Default
    private Boolean isEmailVerified = false;

    @Builder.Default
    private Boolean isActivated = true;

    private LocalDateTime lastLoginAt;

    // Doctor-specific fields
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private LocalDateTime activationDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Business Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isDoctor() {
        return roles != null && roles.contains(UserRole.DOCTOR);
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(UserRole.ADMIN);
    }

    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }
}