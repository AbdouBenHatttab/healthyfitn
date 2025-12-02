package com.health.virtualdoctor.ui.data.models

import java.time.LocalDate


data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate?,
    val gender: String?,
    val phoneNumber: String?,
    val role: String, // "USER", "DOCTOR", "ADMIN"

    // Doctor-specific fields (optional)
    val medicalLicenseNumber: String? = null,
    val specialization: String? = null,
    val hospitalAffiliation: String? = null,
    val yearsOfExperience: Int? = null
)