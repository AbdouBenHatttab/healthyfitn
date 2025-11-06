package com.healthapp.auth.dto.request;
import com.healthapp.auth.Enums.Gender;
import com.healthapp.auth.Enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    @Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{8,128}$",
    message = "Password must contain at least: 1 lowercase, 1 uppercase, 1 digit, and 1 special character (@$!%*?&.)"
    )
    private String password;
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    private Gender gender;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotNull(message = "User role is required")
    private UserRole role;
    
    // Doctor-specific fields (unchanged)
    @Size(max = 50, message = "Medical license number cannot exceed 50 characters")
    private String medicalLicenseNumber;
    
    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;
    
    @Size(max = 200, message = "Hospital affiliation cannot exceed 200 characters")
    private String hospitalAffiliation;
    
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience cannot exceed 60")
    private Integer yearsOfExperience;
    
    @AssertTrue(message = "Doctor fields are required when registering as a doctor")
    private boolean isDoctorFieldsValid() {
        if (role == UserRole.DOCTOR) {
            return medicalLicenseNumber != null && !medicalLicenseNumber.trim().isEmpty()
                && specialization != null && !specialization.trim().isEmpty()
                && hospitalAffiliation != null && !hospitalAffiliation.trim().isEmpty()
                && yearsOfExperience != null && yearsOfExperience >= 0;
        }
        return true;
    }
}