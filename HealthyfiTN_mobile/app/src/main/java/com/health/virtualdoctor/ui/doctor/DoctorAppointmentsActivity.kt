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
import com.health.virtualdoctor.ui.data.models.AppointmentRequest
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Activité des Rendez-vous du Docteur
 * Affiche la liste des rendez-vous avec les options pour :
 * - Voir les détails
 * - Terminer la consultation
 * - Annuler le rendez-vous
 * - Créer un nouveau rendez-vous
 */
class DoctorAppointmentsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var rvAppointments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnCreateAppointment: com.google.android.material.button.MaterialButton
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
        btnCreateAppointment = findViewById(R.id.btnCreateAppointment)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCreateAppointment.setOnClickListener {
            showCreateAppointmentDialog()
        }
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
            "view_details" -> showAppointmentDetails(appointment)
            "complete" -> showCompleteAppointmentDialog(appointment)
            "cancel" -> showCancelAppointmentDialog(appointment)
            "start_consultation" -> showConsultationOptions(appointment) // Gardé pour la compatibilité
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
                    allAppointments = response.body()!!.sortedByDescending {
                        LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME)
                    }

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
                Log.e("DoctorAppointments", "Erreur lors du chargement des rendez-vous: ${e.message}", e)
                Toast.makeText(
                    this@DoctorAppointmentsActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAppointmentDetails(appointment: AppointmentResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_appointment_details, null)

        // Définir les détails du patient
        dialogView.findViewById<TextView>(R.id.tvPatientNameDialog).apply {
            visibility = View.VISIBLE
            text = appointment.patientName
        }
        dialogView.findViewById<TextView>(R.id.tvPatientEmailDialog).apply {
            visibility = View.VISIBLE
            text = appointment.patientEmail
        }
        dialogView.findViewById<TextView>(R.id.tvPatientPhoneDialog).apply {
            visibility = View.VISIBLE
            text = appointment.patientPhone ?: "Non disponible"
        }

        // Afficher les labels
        dialogView.findViewById<TextView>(R.id.lblPatientEmail)?.visibility = View.VISIBLE
        dialogView.findViewById<TextView>(R.id.lblPatientPhone)?.visibility = View.VISIBLE

        // Définir les détails du rendez-vous
        try {
            val dateTime = LocalDateTime.parse(appointment.appointmentDateTime)
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            dialogView.findViewById<TextView>(R.id.tvAppointmentDateDialog).apply {
                visibility = View.VISIBLE
                text = dateTime.format(dateFormatter)
            }
            dialogView.findViewById<TextView>(R.id.tvAppointmentTimeDialog).apply {
                visibility = View.VISIBLE
                text = dateTime.format(timeFormatter)
            }
        } catch (e: Exception) {
            dialogView.findViewById<TextView>(R.id.tvAppointmentDateDialog).text =
                appointment.appointmentDateTime.substringBefore("T")
            dialogView.findViewById<TextView>(R.id.tvAppointmentTimeDialog).text =
                appointment.appointmentDateTime.substringAfter("T").take(5)
        }

        dialogView.findViewById<TextView>(R.id.tvAppointmentTypeDialog).apply {
            visibility = View.VISIBLE
            text = appointment.appointmentType
        }
        dialogView.findViewById<TextView>(R.id.tvReasonDialog).apply {
            visibility = View.VISIBLE
            text = appointment.reason
        }

        val chipStatus = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipStatusDialog)
        chipStatus.apply {
            visibility = View.VISIBLE
            text = appointment.status
        }

        if (!appointment.notes.isNullOrEmpty()) {
            dialogView.findViewById<TextView>(R.id.tvNotesDialog).text = appointment.notes
            dialogView.findViewById<View>(R.id.cardNotesDialog).visibility = View.VISIBLE
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDialog)
            .setOnClickListener {
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showConsultationOptions(appointment: AppointmentResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Options de Consultation")
            .setItems(arrayOf(
                "✅ Terminer la consultation",
                "❌ Annuler le rendez-vous"
            )) { _, which ->
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

        dialogView.findViewById<TextView>(R.id.tvPatientNameComplete).text =
            "Patient: ${appointment.patientName}"

        val etDiagnosis = dialogView.findViewById<EditText>(R.id.etDiagnosis)
        val etPrescription = dialogView.findViewById<EditText>(R.id.etPrescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etConsultationNotes)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelComplete)
            .setOnClickListener {
                dialog.dismiss()
            }

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

        dialogView.findViewById<TextView>(R.id.tvPatientNameCancel).text =
            "Patient: ${appointment.patientName}"

        val etReason = dialogView.findViewById<EditText>(R.id.etCancelReason)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelDialogCancel)
            .setOnClickListener {
                dialog.dismiss()
            }

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

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "✅ Consultation terminée avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAppointments()
                } else {
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DoctorAppointments", "Erreur lors de la finalisation: ${e.message}", e)
                Toast.makeText(
                    this@DoctorAppointmentsActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "✅ Rendez-vous annulé avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAppointments()
                } else {
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DoctorAppointments", "Erreur lors de l'annulation: ${e.message}", e)
                Toast.makeText(
                    this@DoctorAppointmentsActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showCreateAppointmentDialog() {
        // TODO: Implémenter la création de rendez-vous pour le docteur
        // Cela nécessiterait d'obtenir la liste des patients
        Toast.makeText(this, "Créer un rendez-vous - Fonctionnalité à implémenter", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}