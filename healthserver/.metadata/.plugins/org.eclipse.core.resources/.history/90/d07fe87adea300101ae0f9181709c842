package com.healthapp.doctor.dto.request;

import com.healthapp.shared.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request pour l'enregistrement d'un nouveau médecin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRegisterRequest {
    
    // Informations de base
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{8,128}$",
        message = "Password must contain at least: 1 lowercase, 1 uppercase, 1 digit, and 1 special character"
    )
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    private Gender gender;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;
    
    // Informations médicales (OBLIGATOIRES)
    @NotBlank(message = "Medical license number is required")
    @Size(max = 50)
    private String medicalLicenseNumber;
    
    @NotBlank(message = "Specialization is required")
    @Size(max = 100)
    private String specialization;
    
    @NotBlank(message = "Hospital affiliation is required")
    @Size(max = 200)
    private String hospitalAffiliation;
    
    @NotNull(message = "Years of experience is required")
    @Min(value = 0)
    @Max(value = 60)
    private Integer yearsOfExperience;
    
    // Informations de contact professionnel
    private String officeAddress;
    private String consultationHours;
}

