package com.healthapp.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthapp.user.Enums.AccountStatus;
import com.healthapp.user.Enums.Gender;
import com.healthapp.user.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private Gender gender;
    private Double score;
    private String phoneNumber;
    private String profilePictureUrl;
    private Set<UserRole> roles;
    private AccountStatus accountStatus;
    private Boolean isEmailVerified;
    private Boolean isActivated;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Doctor-specific fields
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private LocalDateTime activationDate;
}
