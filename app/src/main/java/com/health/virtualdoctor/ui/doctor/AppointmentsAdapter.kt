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

        // 🔥 These were missing — YOU MUST ADD THEM
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        val btnComplete: MaterialButton = view.findViewById(R.id.btnComplete)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancel)

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

        // STATUS CHIP
        holder.chipStatus.text = appointment.status
        holder.chipStatus.chipBackgroundColor =
            android.content.res.ColorStateList.valueOf(Color.parseColor(appointment.statusColor))
        holder.chipStatus.setTextColor(Color.parseColor(appointment.statusTextColor))

        // ----- BUTTON VISIBILITY BASED ON STATUS -----
        when (appointment.status.uppercase()) {

            "PENDING" -> {
                holder.btnAccept.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE

                holder.btnViewDetails.visibility = View.GONE
                holder.btnComplete.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
                holder.btnStartConsultation.visibility = View.GONE
            }

            "ACCEPTED" -> {
                holder.btnAccept.visibility = View.GONE
                holder.btnReject.visibility = View.GONE

                holder.btnViewDetails.visibility = View.VISIBLE
                holder.btnComplete.visibility = View.VISIBLE
                holder.btnCancel.visibility = View.VISIBLE
                holder.btnStartConsultation.visibility = View.GONE
            }

            "COMPLETED", "CANCELED" -> {
                holder.btnAccept.visibility = View.GONE
                holder.btnReject.visibility = View.GONE
                holder.btnComplete.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE

                holder.btnViewDetails.visibility = View.VISIBLE
                holder.btnStartConsultation.visibility = View.GONE
            }
        }

        // ----- BUTTON ACTIONS -----
        holder.btnAccept.setOnClickListener { onActionClick(appointment, "accept") }
        holder.btnReject.setOnClickListener { onActionClick(appointment, "reject") }
        holder.btnViewDetails.setOnClickListener { onActionClick(appointment, "view_details") }
        holder.btnComplete.setOnClickListener { onActionClick(appointment, "complete") }
        holder.btnCancel.setOnClickListener { onActionClick(appointment, "cancel") }
    }

    override fun getItemCount() = appointments.size
}
