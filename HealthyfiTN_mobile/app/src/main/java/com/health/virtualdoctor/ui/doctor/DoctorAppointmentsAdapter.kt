package com.health.virtualdoctor.ui.doctor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
        // Patient info
        val ivPatientAvatar: ImageView = view.findViewById(R.id.ivPatientAvatar)
        val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        val tvPatientAge: TextView = view.findViewById(R.id.tvPatientAge)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)
        val tvAppointmentDate: TextView = view.findViewById(R.id.tvAppointmentDate)
        val tvAppointmentReason: TextView = view.findViewById(R.id.tvAppointmentReason)

        // Button containers
        val llActionButtons: LinearLayout = view.findViewById(R.id.llActionButtons)
        val rowAcceptReject: LinearLayout = view.findViewById(R.id.rowAcceptReject)
        val rowActionButtons: LinearLayout = view.findViewById(R.id.rowActionButtons)

        // Individual buttons
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val btnComplete: MaterialButton = view.findViewById(R.id.btnComplete)
        val btnCancelAppt: MaterialButton = view.findViewById(R.id.btnCancelAppt)
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

        // Reset all button visibility
        holder.llActionButtons.visibility = View.GONE
        holder.rowAcceptReject.visibility = View.GONE
        holder.rowActionButtons.visibility = View.GONE
        holder.btnStartConsultation.visibility = View.GONE
        // ✅ FIX: Check if appointment type is VIDEO_CALL and show video button
        val isVideoCall = appointment.appointmentType.equals("VIDEO_CALL", ignoreCase = true)

        when (appointment.status.uppercase()) {
            "PENDING" -> {
                holder.chipStatus.text = "⏳ En attente"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                holder.chipStatus.setTextColor(Color.WHITE)

                // Show accept/reject buttons
                holder.llActionButtons.visibility = View.VISIBLE
                holder.rowAcceptReject.visibility = View.VISIBLE

                holder.btnAccept.setOnClickListener {
                    onActionClick(appointment, "accept")
                }
                holder.btnReject.setOnClickListener {
                    onActionClick(appointment, "reject")
                }
            }

            "SCHEDULED" -> {
                holder.chipStatus.text = "⏰ Programmé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                holder.chipStatus.setTextColor(Color.WHITE)

                // Show action buttons (View Details + Complete + Cancel)
                holder.llActionButtons.visibility = View.VISIBLE
                holder.rowActionButtons.visibility = View.VISIBLE
                holder.btnStartConsultation.visibility = View.GONE
             // ✅ FIX: Add VIDEO CALL button if appointment type is VIDEO_CALL
                if (isVideoCall) {
                    // Add video call button
                    holder.btnComplete.text = "📹 Appel Vidéo"
                    holder.btnComplete.setOnClickListener {
                        onActionClick(appointment, "video_call")
                    }
                } else {
                    holder.btnComplete.text = "✅ Compléter"
                    holder.btnComplete.setOnClickListener {
                        onActionClick(appointment, "complete")
                    }
                }

                holder.btnViewDetails.setOnClickListener {
                    onActionClick(appointment, "view_details")
                }
//                holder.btnComplete.setOnClickListener {
//                    onActionClick(appointment, "complete")
//                }
                holder.btnCancelAppt.setOnClickListener {
                    onActionClick(appointment, "cancel")
                }
            }

            "COMPLETED" -> {
                holder.chipStatus.text = "✅ Terminé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                holder.chipStatus.setTextColor(Color.WHITE)

                // Show only "Voir les détails" button
                holder.btnStartConsultation.visibility = View.VISIBLE
                holder.btnStartConsultation.setOnClickListener {
                    onActionClick(appointment, "view_details")
                }
            }

            "CANCELLED" -> {
                holder.chipStatus.text = "❌ Annulé"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                holder.chipStatus.setTextColor(Color.WHITE)

                // Show only "Voir les détails" button
                holder.llActionButtons.visibility = View.GONE
                holder.btnStartConsultation.visibility = View.VISIBLE
                holder.btnStartConsultation.text = "👁️ Voir les détails"
                holder.btnStartConsultation.setOnClickListener {
                    onActionClick(appointment, "view_details")
                }
            }

            else -> {
                holder.chipStatus.text = appointment.status
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                holder.chipStatus.setTextColor(Color.WHITE)
                holder.llActionButtons.visibility = View.GONE

                holder.btnStartConsultation.visibility = View.VISIBLE
                holder.btnStartConsultation.setOnClickListener {
                    onActionClick(appointment, "view_details")
                }
            }
        }
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<AppointmentResponse>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}