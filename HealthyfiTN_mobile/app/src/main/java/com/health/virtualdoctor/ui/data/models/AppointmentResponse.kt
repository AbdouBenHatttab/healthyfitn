package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class AppointmentResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("patientId")
    val patientId: String,

    @SerializedName("patientEmail")
    val patientEmail: String,

    @SerializedName("patientName")
    val patientName: String,

    @SerializedName("patientPhone")
    val patientPhone: String?,

    @SerializedName("doctorId")
    val doctorId: String,

    @SerializedName("doctorEmail")
    val doctorEmail: String,

    @SerializedName("doctorName")
    val doctorName: String,

    @SerializedName("specialization")
    val specialization: String,

    @SerializedName("appointmentDateTime")
    val appointmentDateTime: String,

    @SerializedName("appointmentType")
    val appointmentType: String,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("status")
    val status: String,

    @SerializedName("diagnosis")
    val diagnosis: String?,

    @SerializedName("prescription")
    val prescription: String?,

    @SerializedName("doctorNotes")
    val doctorNotes: String?,

    @SerializedName("completedAt")
    val completedAt: String?,

    @SerializedName("createdAt")
    val createdAt: String,

    // CORRECT SERIALIZED NAMES FOR SNAKE_CASE FIELDS:
    @SerializedName("doctorResponse")
    val doctorResponse: String? = null,

    @SerializedName("doctorResponseReason")
    val doctorResponseReason: String? = null,

    @SerializedName("availableHoursSuggestion")
    val availableHoursSuggestion: String? = null,

    @SerializedName("respondedAt")
    val respondedAt: String? = null,

    @SerializedName("cancellationReason")
    val cancellationReason: String? = null,

    @SerializedName("cancelled_by")
    val cancelledBy: String? = null
)