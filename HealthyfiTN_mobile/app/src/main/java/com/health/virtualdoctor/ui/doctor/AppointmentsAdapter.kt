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

class AppointmentsAdapter(
    private val appointments: List<Appointment>,
    private val onActionClick: (Appointment, String) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

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

        holder.tvPatientName.text = appointment.patientName
        holder.tvPatientAge.text = appointment.patientAge
        holder.tvAppointmentTime.text = appointment.time
        holder.tvAppointmentDate.text = appointment.date
        holder.tvAppointmentReason.text = appointment.reason

        // Status chip styling
        holder.chipStatus.text = appointment.status
        holder.chipStatus.setChipBackgroundColorResource(android.R.color.transparent)
        holder.chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            Color.parseColor(appointment.statusColor)
        )
        holder.chipStatus.setTextColor(Color.parseColor(appointment.statusTextColor))

        // Button listeners
        holder.btnViewDetails.setOnClickListener {
            onActionClick(appointment, "view_details")
        }

        holder.btnStartConsultation.setOnClickListener {
            onActionClick(appointment, "start_consultation")
        }
    }

    override fun getItemCount() = appointments.size
}