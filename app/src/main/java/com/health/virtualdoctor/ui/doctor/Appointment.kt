package com.health.virtualdoctor.ui.doctor

data class Appointment(
    val id: String,
    val patientName: String,
    val patientAge: String,
    val date: String,
    val time: String,
    val reason: String,
    val status: String,
    val statusColor: String = "#E0E0E0", // Couleur du chip (fond)
    val statusTextColor: String = "#000000" // Couleur du texte
)
