package com.health.virtualdoctor.ui.data.models

import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDateTime
import com.google.gson.annotations.SerializedName

// ==========================================
// ADMIN ENDPOINTS FOR DOCTOR MANAGEMENT
// ==========================================

data class DoctorPendingResponse(
    val id: String,
    val doctorId: String,
    val email: String,
    val fullName: String,
    val medicalLicenseNumber: String,
    val specialization: String,
    val hospitalAffiliation: String,
    val yearsOfExperience: Int,


    // ✅ Changed from LocalDateTime to String
     @SerializedName("registrationDate")
     val registrationDate: String?,  // ISO 8601: "2025-01-15T10:30:00"

    @SerializedName("activationRequestDate")
    val activationRequestDate: String?  // ISO 8601: "2025-01-15T10:30:00"
)

