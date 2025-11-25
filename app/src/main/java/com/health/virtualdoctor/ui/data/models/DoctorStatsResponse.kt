package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

data class DoctorStatsResponse(
    @SerializedName("doctorId")
    val doctorId: String,

    @SerializedName("doctorName")
    val doctorName: String,

    @SerializedName("specialization")
    val specialization: String,

    @SerializedName("todayAppointments")
    val todayAppointments: Int,

    @SerializedName("todayCompleted")
    val todayCompleted: Int,

    @SerializedName("todayPending")
    val todayPending: Int,

    @SerializedName("totalAppointments")
    val totalAppointments: Int,

    @SerializedName("totalPatients")
    val totalPatients: Int,

    @SerializedName("upcomingAppointments")
    val upcomingAppointments: Int,

    @SerializedName("completedAppointments")
    val completedAppointments: Int,

    @SerializedName("cancelledAppointments")
    val cancelledAppointments: Int,

    @SerializedName("thisWeekAppointments")
    val thisWeekAppointments: Int,

    @SerializedName("thisMonthAppointments")
    val thisMonthAppointments: Int,

    @SerializedName("generatedAt")
    val generatedAt: String
)