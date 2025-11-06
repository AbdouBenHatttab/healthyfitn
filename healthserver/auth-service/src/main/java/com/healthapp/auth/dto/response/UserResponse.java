package com.healthapp.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthapp.auth.Enums.AccountStatus;
import com.healthapp.auth.Enums.Gender;
import com.healthapp.auth.Enums.UserRole;
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
    private String phoneNumber;
    private String profilePictureUrl;
    private Set<UserRole> roles;
    private AccountStatus accountStatus;
    private Boolean isEmailVerified;
    private Boolean isActivated;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String medicalLicenseNumber;
    private String specialization;
    private String hospitalAffiliation;
    private Integer yearsOfExperience;
    private LocalDateTime activationDate;
}

