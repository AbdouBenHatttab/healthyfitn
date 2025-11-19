package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class AppointmentRequest(
    @SerializedName("doctorId")
    val doctorId: String,

    @SerializedName("appointmentDateTime")
    val appointmentDateTime: String,  // ← String format: "2025-12-15T10:00:00"

    @SerializedName("appointmentType")
    val appointmentType: String,      // ← "CONSULTATION" or "VIDEO_CALL"

    @SerializedName("reason")
    val reason: String,

    @SerializedName("notes")
    val notes: String? = null         // ← Optional field
)