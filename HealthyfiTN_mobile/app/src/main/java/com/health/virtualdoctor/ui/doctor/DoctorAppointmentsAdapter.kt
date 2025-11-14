package com.health.virtualdoctor.ui.doctor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DoctorAppointmentsAdapter(
    private var appointments: List<AppointmentResponse>,
    private val onActionClick: (AppointmentResponse, String) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentsAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPatientAvatar: ImageView = view.findViewById(R.id.ivPatientAvatar)
        val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        val tvPatientAge: TextView = view.findViewById(R.id.tvPatientAge)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)
        val tvAppointmentDate: TextView = view.findViewById(R.id.tvAppointmentDate)
        val tvAppointmentReason: TextView = view.findViewById(R.id.tvAppointmentReason)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val btnStartConsultation: MaterialButton = view.findViewById(R.id.btnStartConsultation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]

        // Patient info
        holder.tvPatientName.text = appointment.patientName
        holder.tvPatientAge.text = appointment.patientEmail

        // Parse date and time
        try {
            val dateTime = LocalDateTime.parse(appointment.appointmentDateTime)
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            holder.tvAppointmentDate.text = dateTime.format(dateFormatter)
            holder.tvAppointmentTime.text = dateTime.format(timeFormatter)
        } catch (e: Exception) {
            holder.tvAppointmentDate.text = appointment.appointmentDateTime.substringBefore("T")
            holder.tvAppointmentTime.text = appointment.appointmentDateTime.substringAfter("T").take(5)
        }

        // Reason
        holder.tvAppointmentReason.text = appointment.reason

        // Status chip
        when (appointment.status) {
            "SCHEDULED" -> {
                holder.chipStatus.text = "⏰ Programmé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                holder.chipStatus.setTextColor(Color.WHITE)
            }
            "COMPLETED" -> {
                holder.chipStatus.text = "✅ Complété"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                holder.chipStatus.setTextColor(Color.WHITE)
            }
            "CANCELLED" -> {
                holder.chipStatus.text = "❌ Annulé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                holder.chipStatus.setTextColor(Color.WHITE)
            }
            else -> {
                holder.chipStatus.text = appointment.status
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                holder.chipStatus.setTextColor(Color.WHITE)
            }
        }

        // Button visibility based on status
        when (appointment.status) {
            "SCHEDULED" -> {
                holder.btnStartConsultation.visibility = View.VISIBLE
                holder.btnStartConsultation.text = "Démarrer"
                holder.btnStartConsultation.setIconResource(R.drawable.ic_videocam)
            }
            "COMPLETED" -> {
                holder.btnStartConsultation.visibility = View.GONE
            }
            "CANCELLED" -> {
                holder.btnStartConsultation.visibility = View.GONE
            }
            else -> {
                holder.btnStartConsultation.visibility = View.VISIBLE
            }
        }

        // Button listeners
        holder.btnViewDetails.setOnClickListener {
            onActionClick(appointment, "view_details")
        }

        holder.btnStartConsultation.setOnClickListener {
            if (appointment.status == "SCHEDULED") {
                onActionClick(appointment, "start_consultation")
            }
        }
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<AppointmentResponse>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}