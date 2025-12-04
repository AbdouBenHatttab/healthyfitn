package com.healthapp.auth.entity;

import com.healthapp.auth.Enums.AccountStatus;
import com.healthapp.auth.Enums.Gender;
import com.healthapp.auth.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String keycloakId; // ✅ LIEN AVEC KEYCLOAK
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Indexed(unique = true)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @Builder.Default
    private Gender gender = Gender.MALE;
    
    private String phoneNumber;
    
    private String profilePictureUrl;
    
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();
    
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    
    @Builder.Default
    private Boolean isEmailVerified = false;
    
    @Builder.Default
    private Boolean isActivated = true; // Default true for USER, false for DOCTOR
    
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime emailVerificationTokenExpiry;
    private String emailVerificationToken;
    
    // Doctor-specific fields
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private LocalDateTime activationRequestDate;
    private String activatedBy; // Admin ID who activated the account
    private LocalDateTime activationDate;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Business Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isAccountNonLocked() {
        return !AccountStatus.LOCKED.equals(accountStatus) && failedLoginAttempts < 5;
    }
    
    public boolean isAccountNonExpired() {
        return !AccountStatus.SUSPENDED.equals(accountStatus);
    }
    
    public boolean isEnabled() {
        return AccountStatus.ACTIVE.equals(accountStatus) && isActivated;
    }

    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }
    public boolean isDoctor() {
        return roles.contains(UserRole.DOCTOR);
    }
    
    public boolean isAdmin() {
        return roles.contains(UserRole.ADMIN);
    }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;
        if (this.failedLoginAttempts >= 5) {
            this.accountStatus = AccountStatus.LOCKED;
        }
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        if (AccountStatus.LOCKED.equals(this.accountStatus)) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
        this.lastLoginAt = LocalDateTime.now();
    }
}

