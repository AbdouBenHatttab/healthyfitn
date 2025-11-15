package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class AppointmentRequest(
    @SerializedName("doctorId")
    val doctorId: String,
    
    @SerializedName("appointmentDateTime")
    val appointmentDateTime: String,
    
    @SerializedName("reasonForVisit")
    val reasonForVisit: String,
    
    @SerializedName("appointmentType")
    val appointmentType: String
)