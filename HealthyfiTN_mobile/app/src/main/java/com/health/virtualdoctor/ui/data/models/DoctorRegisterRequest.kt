package com.health.virtualdoctor.ui.data.models


data class DoctorRegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String?,  // Format: "yyyy-MM-dd"
    val gender: String?,     // "MALE" ou "FEMALE"
    val phoneNumber: String?,

    // Informations médicales (OBLIGATOIRES pour doctor)
    val medicalLicenseNumber: String,
    val specialization: String,
    val hospitalAffiliation: String,
    val yearsOfExperience: Int,
    val officeAddress: String?,
    val consultationHours: String?,



)