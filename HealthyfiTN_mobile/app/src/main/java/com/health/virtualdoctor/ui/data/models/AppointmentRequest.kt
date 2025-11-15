package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName
data class AppointmentRequest(
    @SerializedName("doctorId")
    val doctorId: String,

    @SerializedName("appointmentDateTime")
    val appointmentDateTime: String,

    @SerializedName("reason") // ← CHANGE THIS from "reasonForVisit"
    val reason: String, // ← ALSO CHANGE THE PROPERTY NAME

    @SerializedName("appointmentType")
    val appointmentType: String
)