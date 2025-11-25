package com.health.virtualdoctor.ui.user

data class Appointment(
    val id: String,
    val patientId: String,
    val patientEmail: String,
    val patientName: String,
    val doctorId: String,
    val doctorEmail: String,
    val doctorName: String,
    val specialization: String,
    val appointmentDateTime: String,
    val appointmentType: String,
    val reason: String,
    val notes: String?,
    val status: String,
    val createdAt: String
)