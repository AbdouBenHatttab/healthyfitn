package com.health.virtualdoctor.ui.data.models


data class DoctorResponse(
    val id: String,
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phoneNumber: String?,
    val medicalLicenseNumber: String,
    val specialization: String,
    val hospitalAffiliation: String,
    val yearsOfExperience: Int,
    val officeAddress: String?,
    val consultationHours: String?,
    val profilePictureUrl: String?,
    val isActivated: Boolean,
    val activationStatus: String,
    val activationDate: String?,
    val activationRequestDate: String?,
    val totalPatients: Int?,
    val averageRating: Double?,
    val totalConsultations: Int?,
    val createdAt: String
)
