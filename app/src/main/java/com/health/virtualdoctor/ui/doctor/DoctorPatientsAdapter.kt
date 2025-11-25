package com.health.virtualdoctor.ui.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.PatientInfoResponse

class DoctorPatientsAdapter(
    private var patients: List<PatientInfoResponse>,
    private val onPatientClick: (PatientInfoResponse) -> Unit
) : RecyclerView.Adapter<DoctorPatientsAdapter.PatientViewHolder>() {

    inner class PatientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPatientAvatar: ImageView = view.findViewById(R.id.ivPatientAvatar)
        val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        val tvPatientEmail: TextView = view.findViewById(R.id.tvPatientEmail)
        val tvPatientPhone: TextView = view.findViewById(R.id.tvPatientPhone)
        val tvLastAppointment: TextView = view.findViewById(R.id.tvLastAppointment)
        val btnViewPatient: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnViewPatient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]

        // Use the correct field names from your PatientInfoResponse
        holder.tvPatientName.text = patient.patientName
        holder.tvPatientEmail.text = patient.patientEmail
        holder.tvPatientPhone.text = patient.patientPhone ?: "Non disponible"

        // Format last appointment date if available
        if (!patient.lastAppointmentDate.isNullOrEmpty()) {
            holder.tvLastAppointment.text = "Dernier RDV: ${formatDate(patient.lastAppointmentDate)}"
            holder.tvLastAppointment.visibility = View.VISIBLE
        } else {
            holder.tvLastAppointment.visibility = View.GONE
        }

        // For now, use default avatar since your PatientInfoResponse doesn't have profilePictureUrl
        holder.ivPatientAvatar.setImageResource(R.drawable.ic_person)

        // Set click listener
        holder.btnViewPatient.setOnClickListener {
            onPatientClick(patient)
        }

        holder.itemView.setOnClickListener {
            onPatientClick(patient)
        }
    }

    override fun getItemCount() = patients.size

    fun updatePatients(newPatients: List<PatientInfoResponse>) {
        patients = newPatients
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Simple date formatting
            dateString.substringBefore("T") // Shows date like "2024-11-15"
        } catch (e: Exception) {
            "Récent"
        }
    }
}