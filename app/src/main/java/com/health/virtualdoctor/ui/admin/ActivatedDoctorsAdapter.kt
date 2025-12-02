package com.health.virtualdoctor.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.DoctorPendingResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivatedDoctorsAdapter(
    private val doctors: List<DoctorPendingResponse>
) : RecyclerView.Adapter<ActivatedDoctorsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvDoctorEmail: TextView = view.findViewById(R.id.tvDoctorEmail)
        val tvSpecialization: TextView = view.findViewById(R.id.tvSpecialization)
        val tvHospital: TextView = view.findViewById(R.id.tvHospital)
        val tvExperience: TextView = view.findViewById(R.id.tvExperience)
        val tvActivationDate: TextView = view.findViewById(R.id.tvActivationDate)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activated_doctor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvDoctorName.text = "👨‍⚕️ Dr. ${doctor.fullName}"
        holder.tvDoctorEmail.text = "📧 ${doctor.email}"
        holder.tvSpecialization.text = "🏥 ${doctor.specialization}"
        holder.tvHospital.text = "🏨 ${doctor.hospitalAffiliation}"
        holder.tvExperience.text = "📅 ${doctor.yearsOfExperience} ans d'expérience"

        // ✅ Format activation date
        holder.tvActivationDate.text = formatDate(doctor.activationRequestDate)

        // Status chip
        holder.chipStatus.text = "✅ ACTIVÉ"
        holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
        holder.chipStatus.setTextColor(Color.WHITE)
    }

    override fun getItemCount() = doctors.size

    /**
     * ✅ Format ISO 8601 date string to readable format
     * Input: "2025-01-15T10:30:00"
     * Output: "✅ Activé le: 15/01/2025 10:30"
     */
    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString.isNullOrEmpty()) {
                "✅ Activé"
            } else {
                val dateTime = LocalDateTime.parse(dateString)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                "✅ Activé le: ${dateTime.format(formatter)}"
            }
        } catch (e: Exception) {
            "✅ Activé"
        }
    }
}