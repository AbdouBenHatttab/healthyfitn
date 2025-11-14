package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class PatientInfoResponse(
    @SerializedName("patientId")
    val patientId: String,

    @SerializedName("patientName")
    val patientName: String,

    @SerializedName("patientEmail")
    val patientEmail: String,

    @SerializedName("patientPhone")
    val patientPhone: String?,

    @SerializedName("totalAppointments")
    val totalAppointments: Int,

    @SerializedName("completedAppointments")
    val completedAppointments: Int,

    @SerializedName("cancelledAppointments")
    val cancelledAppointments: Int,

    @SerializedName("lastAppointmentDate")
    val lastAppointmentDate: String?,

    @SerializedName("nextAppointmentDate")
    val nextAppointmentDate: String?,

    @SerializedName("firstVisitDate")
    val firstVisitDate: String?
)