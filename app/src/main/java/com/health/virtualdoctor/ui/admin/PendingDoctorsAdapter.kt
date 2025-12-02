package com.health.virtualdoctor.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.DoctorPendingResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PendingDoctorsAdapter(
    private val doctors: List<DoctorPendingResponse>,
    private val onAction: (DoctorPendingResponse, String) -> Unit
) : RecyclerView.Adapter<PendingDoctorsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvDoctorEmail: TextView = view.findViewById(R.id.tvDoctorEmail)
        val tvSpecialization: TextView = view.findViewById(R.id.tvSpecialization)
        val tvHospital: TextView = view.findViewById(R.id.tvHospital)
        val tvExperience: TextView = view.findViewById(R.id.tvExperience)
        val tvLicense: TextView = view.findViewById(R.id.tvLicense)
        val tvRegistrationDate: TextView = view.findViewById(R.id.tvRegistrationDate)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_doctor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvDoctorName.text = "👨‍⚕️ Dr. ${doctor.fullName}"
        holder.tvDoctorEmail.text = "📧 ${doctor.email}"
        holder.tvDoctorEmail.text = "📧 ${doctor.email}"
        holder.tvSpecialization.text = "🏥 ${doctor.specialization}"
        holder.tvHospital.text = "🏨 ${doctor.hospitalAffiliation}"
        holder.tvExperience.text = "📅 ${doctor.yearsOfExperience} ans d'expérience"
        holder.tvLicense.text = "🆔 Licence: ${doctor.medicalLicenseNumber}"

        // ✅ Format date from String
        holder.tvRegistrationDate.text = formatDate(doctor.registrationDate)

        // Status chip
        holder.chipStatus.text = "⏳ EN ATTENTE"
        holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
        holder.chipStatus.setTextColor(Color.WHITE)

        // Button listeners
        holder.btnApprove.setOnClickListener {
            onAction(doctor, "approve")
        }

        holder.btnReject.setOnClickListener {
            onAction(doctor, "reject")
        }

        holder.btnViewDetails.setOnClickListener {
            onAction(doctor, "view")
        }
    }

    override fun getItemCount() = doctors.size

    /**
     * ✅ Format ISO 8601 date string to readable format
     * Input: "2025-01-15T10:30:00"
     * Output: "📆 15/01/2025 10:30"
     */
    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString.isNullOrEmpty()) {
                "📆 N/A"
            } else {
                val dateTime = LocalDateTime.parse(dateString)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                "📆 ${dateTime.format(formatter)}"
            }
        } catch (e: Exception) {
            "📆 $dateString"
        }
    }
}