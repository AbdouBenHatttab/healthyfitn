package com.healthapp.auth.service;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import org.springframework.stereotype.Service;

/**
 * Simple UserService for auth-service
 * Only handles mapping User entity to UserResponse
 * 
 * This is separate from the full UserService in user-service microservice
 */
@Service
public class UserService {
    
    /**
     * Map User entity to UserResponse DTO
     */
    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles())
                .accountStatus(user.getAccountStatus())
                .isEmailVerified(user.getIsEmailVerified())
                .isActivated(user.getIsActivated())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .medicalLicenseNumber(user.getMedicalLicenseNumber())
                .specialization(user.getSpecialization())
                .hospitalAffiliation(user.getHospitalAffiliation())
                .yearsOfExperience(user.getYearsOfExperience())
                .activationDate(user.getActivationDate())
                .build();
    }
}