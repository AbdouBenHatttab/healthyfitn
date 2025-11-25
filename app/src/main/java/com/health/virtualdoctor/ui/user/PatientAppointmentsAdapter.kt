package com.health.virtualdoctor.ui.user

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PatientAppointmentsAdapter(
    private var appointments: List<AppointmentResponse>,
    private val onViewDetails: (AppointmentResponse) -> Unit,
    private val onCancel: (AppointmentResponse) -> Unit
) : RecyclerView.Adapter<PatientAppointmentsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = view.findViewById(R.id.tvSpecialization)
        val tvAppointmentDate: TextView = view.findViewById(R.id.tvAppointmentDate)
        val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]

        holder.tvDoctorName.text = "Dr. ${appointment.doctorName}"
        holder.tvSpecialization.text = appointment.specialization
        holder.tvReason.text = appointment.reason

        // Parse and format date/time
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

        // Status chip styling
        when (appointment.status) {
            "SCHEDULED" -> {
                holder.chipStatus.text = "⏰ Programmé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                holder.chipStatus.setTextColor(Color.WHITE)
                holder.btnCancel.visibility = View.VISIBLE
            }
            "COMPLETED" -> {
                holder.chipStatus.text = "✅ Complété"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                holder.chipStatus.setTextColor(Color.WHITE)
                holder.btnCancel.visibility = View.GONE
            }
            "CANCELLED" -> {
                holder.chipStatus.text = "❌ Annulé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                holder.chipStatus.setTextColor(Color.WHITE)
                holder.btnCancel.visibility = View.GONE
            }
            else -> {
                holder.chipStatus.text = appointment.status
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                holder.chipStatus.setTextColor(Color.WHITE)
                holder.btnCancel.visibility = View.GONE
            }
        }

        // Button listeners
        holder.btnViewDetails.setOnClickListener {
            onViewDetails(appointment)
        }

        holder.btnCancel.setOnClickListener {
            onCancel(appointment)
        }
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<AppointmentResponse>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}