package com.health.virtualdoctor.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.view.LayoutInflater
import com.health.virtualdoctor.ui.data.models.AppointmentResponseRequest
import com.health.virtualdoctor.ui.consultation.VideoCallActivity


class DoctorAppointmentsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var rvAppointments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var btnBack: ImageButton

    private lateinit var appointmentsAdapter: DoctorAppointmentsAdapter

    private var allAppointments = listOf<AppointmentResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_appointments)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupRecyclerView()
        loadAppointments()
    }

    private fun initViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
        btnBack = findViewById(R.id.btnBack)

    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

    }

    private fun setupRecyclerView() {
        appointmentsAdapter = DoctorAppointmentsAdapter(emptyList()) { appointment, action ->
            handleAppointmentAction(appointment, action)
        }
        rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@DoctorAppointmentsActivity)
            adapter = appointmentsAdapter
        }
    }

    private fun handleAppointmentAction(appointment: AppointmentResponse, action: String) {
        when (action) {
            "accept" -> acceptAppointment(appointment)
            "reject" -> showRejectAppointmentDialog(appointment)
            "view_details" -> showAppointmentDetails(appointment)
            "complete" -> showCompleteAppointmentDialog(appointment)
            "cancel" -> showCancelAppointmentDialog(appointment)
            "start_consultation" -> showConsultationOptions(appointment)
            "video_call" -> startVideoCall(appointment)

        }
    }
    private fun startVideoCall(appointment: AppointmentResponse) {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("appointmentId", appointment.id)
        startActivity(intent)
    }

    private fun acceptAppointment(appointment: AppointmentResponse) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val response = RetrofitClient.getDoctorService(this@DoctorAppointmentsActivity)
                    .acceptAppointment(token, appointment.id)
                showResponseToast(response.isSuccessful, "Rendez-vous accepté", response.code())
                if (response.isSuccessful) loadAppointments()
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }


    private fun showRejectAppointmentDialog(appointment: AppointmentResponse) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reject_appointment, null)

        // Set patient name
        val tvPatientName = dialogView.findViewById<TextView>(R.id.tvPatientNameReject)
        tvPatientName.text = appointment.patientName

        val etRejectReason = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRejectReason)
        val etAvailableHours = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAvailableHours)
        val btnCancelReject = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelReject)
        val btnConfirmReject = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmReject)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancelReject.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmReject.setOnClickListener {
            val reason = etRejectReason.text.toString().trim()
            val availableHoursText = etAvailableHours.text.toString().trim()

            if (reason.isEmpty()) {
                etRejectReason.error = "La raison du refus est obligatoire"
                return@setOnClickListener
            }

            // Convert available hours text to list if provided
            val availableHours = if (availableHoursText.isNotEmpty()) {
                listOf(availableHoursText)
            } else {
                null
            }

            rejectAppointmentWithReason(appointment, reason, availableHours)
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun rejectAppointmentWithReason(
        appointment: AppointmentResponse,
        reason: String,
        availableHours: List<String>? = null  // Keep parameter as List for now, we'll handle conversion
    ) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val token = "Bearer ${tokenManager.getAccessToken()}"

                // Convert List<String> to String (join with comma if multiple)
                val availableHoursString = if (availableHours != null && availableHours.isNotEmpty()) {
                    availableHours.joinToString(", ")
                } else {
                    null
                }

                val rejectRequest = AppointmentResponseRequest(
                    reason = reason,
                    availableHours = availableHoursString  // Send as String
                )

                Log.d("REJECT_DEBUG", "Sending request: $rejectRequest")

                val response = RetrofitClient.getDoctorService(this@DoctorAppointmentsActivity)
                    .rejectAppointment(token, appointment.id, rejectRequest)

                if (response.isSuccessful) {
                    Log.d("REJECT_DEBUG", "✅ SUCCESS: ${response.body()}")
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "✅ Rendez-vous refusé avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAppointments()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("REJECT_DEBUG", "❌ ERROR ${response.code()}: $errorBody")
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("REJECT_DEBUG", "🚨 EXCEPTION: ${e.message}", e)
                showErrorToast(e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadAppointments() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                rvAppointments.visibility = View.GONE
                llEmptyState.visibility = View.GONE

                val token = "Bearer ${tokenManager.getAccessToken()}"
                val response = RetrofitClient.getDoctorService(this@DoctorAppointmentsActivity)
                    .getDoctorAppointments(token)

                if (response.isSuccessful && response.body() != null) {
                    allAppointments = response.body()!!
                        .sortedWith(compareBy<AppointmentResponse> { it.status != "PENDING" }
                            .thenByDescending {
                                LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME)
                            })
                    if (allAppointments.isEmpty()) {
                        rvAppointments.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvAppointments.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        appointmentsAdapter.updateAppointments(allAppointments)
                    }
                }
            } catch (e: Exception) {
                Log.e("DoctorAppointments", "Erreur lors du chargement: ${e.message}", e)
                showErrorToast(e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAppointmentDetails(appointment: AppointmentResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_appointment_details, null)

        // Find all views
        val tvAppointmentDateDetails = dialogView.findViewById<TextView>(R.id.tvAppointmentDateDetails)
        val tvAppointmentTimeDetails = dialogView.findViewById<TextView>(R.id.tvAppointmentTimeDetails)
        val chipAppointmentTypeDetails = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAppointmentTypeDetails)
        val tvAppointmentReasonDetails = dialogView.findViewById<TextView>(R.id.tvAppointmentReasonDetails)
        val tvAppointmentStatusDetails = dialogView.findViewById<TextView>(R.id.tvAppointmentStatusDetails)

        val cardRejectionDetails = dialogView.findViewById<View>(R.id.cardRejectionDetails)
        val tvRejectionReason = dialogView.findViewById<TextView>(R.id.tvRejectionReason)
        val tvAvailableHours = dialogView.findViewById<TextView>(R.id.tvAvailableHours)
        val tvRespondedAt = dialogView.findViewById<TextView>(R.id.tvRespondedAt)

        val cardCancellationDetails = dialogView.findViewById<View>(R.id.cardCancellationDetails)
        val tvCancelledBy = dialogView.findViewById<TextView>(R.id.tvCancelledBy)
        val tvCancellationReason = dialogView.findViewById<TextView>(R.id.tvCancellationReason)

        val cardCompletedDetails = dialogView.findViewById<View>(R.id.cardCompletedDetails)
        val tvDiagnosis = dialogView.findViewById<TextView>(R.id.tvDiagnosis)
        val tvPrescription = dialogView.findViewById<TextView>(R.id.tvPrescription)
        val tvDoctorNotes = dialogView.findViewById<TextView>(R.id.tvDoctorNotes)

        val cardNotesDialog = dialogView.findViewById<View>(R.id.cardNotesDialog)
        val tvNotesDialog = dialogView.findViewById<TextView>(R.id.tvNotesDialog)

        val btnCloseDialog = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDialog)


        // Populate Patient Details
        // (Patient details are not displayed in this dialog anymore)

        // Populate Date and Time
        try {
            val dateTime = LocalDateTime.parse(appointment.appointmentDateTime)
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            tvAppointmentDateDetails.text = dateTime.format(dateFormatter)
            tvAppointmentTimeDetails.text = dateTime.format(timeFormatter)
        } catch (_: Exception) {
            tvAppointmentDateDetails.text = appointment.appointmentDateTime.substringBefore("T")
            tvAppointmentTimeDetails.text = appointment.appointmentDateTime.substringAfter("T").take(5)
        }

        // Populate Type, Reason, Status
        chipAppointmentTypeDetails.text = appointment.appointmentType
        tvAppointmentReasonDetails.text = appointment.reason
        tvAppointmentStatusDetails.text = appointment.status


        // Handle conditional display based on status
        when (appointment.status) {
            "REJECTED" -> {
                cardRejectionDetails.visibility = View.VISIBLE
                tvRejectionReason.text = appointment.doctorResponseReason ?: "Non spécifié"
                tvAvailableHours.text = "Heures suggérées: ${appointment.availableHoursSuggestion ?: "N/A"}"
                tvRespondedAt.text = "Date de réponse: ${appointment.respondedAt ?: "N/A"}"
            }
            "COMPLETED" -> {
                cardCompletedDetails.visibility = View.VISIBLE
                tvDiagnosis.text = appointment.diagnosis ?: "Non spécifié"
                tvPrescription.text = appointment.prescription ?: "Non spécifié"

                val llDoctorNotes = dialogView.findViewById<LinearLayout>(R.id.llDoctorNotes)
                if (!appointment.doctorNotes.isNullOrBlank()) {
                    llDoctorNotes.visibility = View.VISIBLE
                    tvDoctorNotes.text = appointment.doctorNotes
                } else {
                    llDoctorNotes.visibility = View.GONE
                }
            }
        }

        // Handle general notes
        if (!appointment.notes.isNullOrEmpty()) {
            cardNotesDialog.visibility = View.VISIBLE
            tvNotesDialog.text = appointment.notes
        }

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showConsultationOptions(appointment: AppointmentResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Options de Consultation")
            .setItems(arrayOf("✅ Terminer la consultation", "❌ Annuler le rendez-vous")) { _, which ->
                when (which) {
                    0 -> showCompleteAppointmentDialog(appointment)
                    1 -> showCancelAppointmentDialog(appointment)
                }
            }
            .setNegativeButton("Retour", null)
            .show()
    }

    private fun showCompleteAppointmentDialog(appointment: AppointmentResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_appointment, null)

        dialogView.findViewById<TextView>(R.id.tvPatientNameComplete).text = "Patient: ${appointment.patientName}"
        val etDiagnosis = dialogView.findViewById<EditText>(R.id.etDiagnosis)
        val etPrescription = dialogView.findViewById<EditText>(R.id.etPrescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etConsultationNotes)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelComplete)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmComplete)
            .setOnClickListener {
                val diagnosis = etDiagnosis.text.toString().trim()
                val prescription = etPrescription.text.toString().trim()
                val notes = etNotes.text.toString().trim()
                if (diagnosis.isEmpty() || prescription.isEmpty()) {
                    Toast.makeText(this, "⚠️ Diagnostic et prescription requis", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                completeAppointment(appointment.id, diagnosis, prescription, notes)
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showCancelAppointmentDialog(appointment: AppointmentResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_appointment, null)
        dialogView.findViewById<TextView>(R.id.tvPatientNameCancel).text = "Patient: ${appointment.patientName}"
        val etReason = dialogView.findViewById<EditText>(R.id.etCancelReason)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelDialogCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmCancel)
            .setOnClickListener {
                val reason = etReason.text.toString().trim().ifEmpty { "Aucune raison fournie" }
                cancelAppointment(appointment.id, reason)
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun completeAppointment(appointmentId: String, diagnosis: String, prescription: String, notes: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = mapOf(
                    "diagnosis" to diagnosis,
                    "prescription" to prescription,
                    "notes" to notes
                )
                val response = RetrofitClient.getDoctorService(this@DoctorAppointmentsActivity)
                    .completeAppointment(token, appointmentId, request)
                showResponseToast(response.isSuccessful, "Consultation terminée avec succès", response.code())
                if (response.isSuccessful) loadAppointments()
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun cancelAppointment(appointmentId: String, reason: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = mapOf("reason" to reason)
                val response = RetrofitClient.getDoctorService(this@DoctorAppointmentsActivity)
                    .cancelAppointmentByDoctor(token, appointmentId, request)
                showResponseToast(response.isSuccessful, "Rendez-vous annulé avec succès", response.code())
                if (response.isSuccessful) loadAppointments()
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }



    private fun showResponseToast(success: Boolean, message: String, code: Int) {
        Toast.makeText(this, if (success) "✅ $message" else "❌ Erreur $code", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorToast(e: Exception) {
        Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("DoctorAppointments", "Erreur: ${e.message}", e)
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}
