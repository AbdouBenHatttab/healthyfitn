package com.health.virtualdoctor.ui.user

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.AppointmentRequest
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import com.health.virtualdoctor.ui.data.models.DoctorAvailableResponse
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class PatientAppointmentsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var rvAppointments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnCreateAppointment: MaterialButton
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var appointmentsAdapter: PatientAppointmentsAdapter

    private var allAppointments = listOf<AppointmentResponse>()
    private var availableDoctors = listOf<DoctorAvailableResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_appointments)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupRecyclerView()
        loadAppointments()
        loadAvailableDoctors()
    }

    private fun initViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
        btnBack = findViewById(R.id.btnBack)
        btnCreateAppointment = findViewById(R.id.btnCreateAppointment)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)

        // Select "Upcoming" by default
        findViewById<Chip>(R.id.chipUpcoming).isChecked = true
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCreateAppointment.setOnClickListener {
            showCreateAppointmentDialog()
        }

        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChipId = checkedIds[0]
                filterAppointments(selectedChipId)
            } else {
                appointmentsAdapter.updateAppointments(allAppointments.filter { it.status.equals("SCHEDULED", ignoreCase = true) })
            }
        }
    }

    private fun setupRecyclerView() {
        appointmentsAdapter = PatientAppointmentsAdapter(
            appointments = emptyList(),
            onViewDetails = { appointment ->
                showAppointmentDetails(appointment)
            },
            onCancel = { appointment ->
                showCancelDialog(appointment)
            }
        )

        rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@PatientAppointmentsActivity)
            adapter = appointmentsAdapter
        }
    }

    private fun loadAppointments() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                rvAppointments.visibility = View.GONE
                llEmptyState.visibility = View.GONE

                val token = "Bearer ${tokenManager.getAccessToken()}"
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .getPatientAppointments(token)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    allAppointments = apiResponse.data?.sortedByDescending {
                        LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME)
                    } ?: emptyList()

                    // DEBUG: Log rejected appointments with their details
                    val rejectedAppointments = allAppointments.filter { it.status == "REJECTED" }
                    Log.d("PatientAppointments", "🔍 Found ${rejectedAppointments.size} rejected appointments")

                    rejectedAppointments.forEach { appointment ->
                        Log.d("PatientAppointments", "🔍 Rejected Appointment Debug:")
                        Log.d("PatientAppointments", "   - ID: ${appointment.id}")
                        Log.d("PatientAppointments", "   - Status: ${appointment.status}")
                        Log.d("PatientAppointments", "   - Doctor Response: ${appointment.doctorResponse}")
                        Log.d("PatientAppointments", "   - Response Reason: ${appointment.doctorResponseReason}")
                        Log.d("PatientAppointments", "   - Available Hours: ${appointment.availableHoursSuggestion}")
                        Log.d("PatientAppointments", "   - Responded At: ${appointment.respondedAt}")
                    }

                    filterAppointments(chipGroupFilter.checkedChipId)

                    Log.d("PatientAppointments", "✅ Loaded ${allAppointments.size} appointments")
                    Log.d("PatientAppointments", "Rejected appointments: ${allAppointments.count { it.status == "REJECTED" }}")
                } else {
                    Toast.makeText(
                        this@PatientAppointmentsActivity,
                        "❌ Erreur de chargement: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    llEmptyState.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("PatientAppointments", "❌ Error loading appointments: ${e.message}", e)
                Toast.makeText(
                    this@PatientAppointmentsActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                llEmptyState.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadAvailableDoctors() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .getAvailableDoctors(token)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    availableDoctors = apiResponse.data ?: emptyList()

                    // Debug logging
                    availableDoctors.forEach { doctor ->
                        Log.d("PatientAppointments", "Doctor: ${doctor.fullName} | ${doctor.firstName} ${doctor.lastName}")
                    }

                    Log.d("PatientAppointments", "✅ Loaded ${availableDoctors.size} available doctors")
                } else {
                    Log.w("PatientAppointments", "Failed to load doctors: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PatientAppointments", "❌ Error loading doctors: ${e.message}", e)
            }
        }
    }

    private fun filterAppointments(chipId: Int) {
        val now = LocalDateTime.now()
        val filteredList = when (chipId) {
            R.id.chipUpcoming -> allAppointments.filter {
                val apptDateTime = LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME)
                apptDateTime.isAfter(now) && it.status.equals("SCHEDULED", ignoreCase = true)
            }
            R.id.chipPast -> allAppointments.filter {
                val apptDateTime = LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME)
                apptDateTime.isBefore(now) ||
                        it.status.equals("COMPLETED", ignoreCase = true) ||
                        it.status.equals("REJECTED", ignoreCase = true) ||
                        it.status.equals("CANCELLED", ignoreCase = true)
            }
            R.id.chipAll -> allAppointments
            else -> allAppointments
        }

        appointmentsAdapter.updateAppointments(filteredList)
        llEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        rvAppointments.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAppointmentDetails(appointment: AppointmentResponse) {
        val detailsView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_details, null)

        // Hide patient info for patient view
        detailsView.findViewById<TextView>(R.id.tvDoctorNameDetails).visibility = View.VISIBLE
        detailsView.findViewById<TextView>(R.id.tvPatientNameDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.lblPatientEmail)?.visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvPatientEmailDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.lblPatientPhone)?.visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvPatientPhoneDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvAppointmentDateDetails).visibility = View.VISIBLE
        detailsView.findViewById<TextView>(R.id.tvAppointmentDateDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvAppointmentTimeDetails).visibility = View.VISIBLE
        detailsView.findViewById<TextView>(R.id.tvAppointmentTimeDialog).visibility = View.GONE
        detailsView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAppointmentTypeDetails).visibility = View.VISIBLE
        detailsView.findViewById<TextView>(R.id.tvAppointmentTypeDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvAppointmentReasonDetails).visibility = View.VISIBLE
        detailsView.findViewById<TextView>(R.id.tvReasonDialog).visibility = View.GONE
        detailsView.findViewById<TextView>(R.id.tvAppointmentStatusDetails).visibility = View.VISIBLE
        detailsView.findViewById<com.google.android.material.chip.Chip>(R.id.chipStatusDialog).visibility = View.GONE
        detailsView.findViewById<View>(R.id.cardNotesDialog).visibility = View.GONE
        detailsView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDialog).visibility = View.GONE

        // Set basic appointment info
        detailsView.findViewById<TextView>(R.id.tvDoctorNameDetails).text = "Dr. ${appointment.doctorName}"
        detailsView.findViewById<TextView>(R.id.tvAppointmentDateDetails).text = formatDisplayDate(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentTimeDetails).text = formatDisplayTime(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentReasonDetails).text = appointment.reason
        detailsView.findViewById<TextView>(R.id.tvAppointmentStatusDetails).text = getStatusDisplayText(appointment.status)
        detailsView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAppointmentTypeDetails).text = appointment.appointmentType

        // Set status color
        val statusTextView = detailsView.findViewById<TextView>(R.id.tvAppointmentStatusDetails)
        statusTextView.setTextColor(getStatusColor(appointment.status))

        // Check if appointment was rejected or cancelled and show relevant details
        if (appointment.status.equals("REJECTED", ignoreCase = true) ||
            (appointment.doctorResponse != null && appointment.doctorResponse.equals("REJECTED", ignoreCase = true))) {
            showRejectionDetails(detailsView, appointment)
            hideCancellationDetails(detailsView) // Hide cancellation details if rejected
        } else if (appointment.status.equals("CANCELLED", ignoreCase = true)) {
            showCancellationDetails(detailsView, appointment)
            hideRejectionDetails(detailsView) // Hide rejection details if cancelled
        } else {
            hideRejectionDetails(detailsView)
            hideCancellationDetails(detailsView) // Hide both if neither rejected nor cancelled
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(detailsView)
            .setPositiveButton("Fermer") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun showRejectionDetails(detailsView: View, appointment: AppointmentResponse) {
        Log.d("PatientAppointments", "🔍 showRejectionDetails called for appointment: ${appointment.id}")
        Log.d("PatientAppointments", "   - Status: ${appointment.status}")
        Log.d("PatientAppointments", "   - Doctor Response: ${appointment.doctorResponse}")
        Log.d("PatientAppointments", "   - Response Reason: ${appointment.doctorResponseReason}")
        Log.d("PatientAppointments", "   - Available Hours: ${appointment.availableHoursSuggestion}")
        Log.d("PatientAppointments", "   - Responded At: ${appointment.respondedAt}")

        // Make sure these views exist in your dialog_appointment_details.xml
        val rejectionCard = detailsView.findViewById<MaterialCardView>(R.id.cardRejectionDetails) ?: return
        val tvRejectionReason = detailsView.findViewById<TextView>(R.id.tvRejectionReason) ?: return
        val tvAvailableHours = detailsView.findViewById<TextView>(R.id.tvAvailableHours) ?: return
        val tvRespondedAt = detailsView.findViewById<TextView>(R.id.tvRespondedAt) ?: return

        rejectionCard.visibility = View.VISIBLE

        tvRejectionReason.text = appointment.doctorResponseReason.let {
            if (it.isNullOrBlank()) "Aucune raison fournie" else it
        }

        tvAvailableHours.text = appointment.availableHoursSuggestion.let {
            if (it.isNullOrBlank()) "Heures suggérées: N/A" else "Heures suggérées: $it"
        }

        tvRespondedAt.visibility = View.GONE // Hide respondedAt as per user request
    }

    private fun hideRejectionDetails(detailsView: View) {
        val rejectionCard = detailsView.findViewById<MaterialCardView>(R.id.cardRejectionDetails)
        rejectionCard?.visibility = View.GONE
    }

    private fun showCancellationDetails(detailsView: View, appointment: AppointmentResponse) {
        val cancellationCard = detailsView.findViewById<MaterialCardView>(R.id.cardCancellationDetails) ?: return
        val tvCancellationReason = detailsView.findViewById<TextView>(R.id.tvCancellationReason) ?: return

        cancellationCard.visibility = View.VISIBLE
        tvCancellationReason.text = appointment.cancellationReason.let {
            if (it.isNullOrBlank()) "Aucune raison fournie" else it
        }
    }

    private fun hideCancellationDetails(detailsView: View) {
        val cancellationCard = detailsView.findViewById<MaterialCardView>(R.id.cardCancellationDetails)
        cancellationCard?.visibility = View.GONE
    }

    private fun getStatusDisplayText(status: String): String {
        return when (status.uppercase()) {
            "SCHEDULED" -> "Planifié"
            "REJECTED" -> "Refusé"
            "CANCELLED" -> "Annulé"
            "COMPLETED" -> "Terminé"
            "PENDING" -> "En attente"
            else -> status
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.uppercase()) {
            "SCHEDULED" -> Color.parseColor("#2196F3") // Blue
            "REJECTED" -> Color.parseColor("#F44336")  // Red
            "CANCELLED" -> Color.parseColor("#FF9800") // Orange
            "COMPLETED" -> Color.parseColor("#4CAF50") // Green
            "PENDING" -> Color.parseColor("#FF9800")   // Orange
            else -> Color.BLACK
        }
    }

    private fun showCancelDialog(appointment: AppointmentResponse) {
        // Don't allow cancellation of rejected or completed appointments
        if (appointment.status.equals("REJECTED", ignoreCase = true) ||
            appointment.status.equals("COMPLETED", ignoreCase = true)) {
            Toast.makeText(this, "Impossible d'annuler un rendez-vous ${getStatusDisplayText(appointment.status).lowercase()}", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_appointment, null)
        val etCancelReason = dialogView.findViewById<EditText>(R.id.etCancelReason)
        val tvPatientName = dialogView.findViewById<TextView>(R.id.tvPatientNameCancel)
        tvPatientName.text = "Rendez-vous avec Dr. ${appointment.doctorName}"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnConfirmCancel).setOnClickListener {
            val reason = etCancelReason.text.toString().trim()
            if (reason.isEmpty()) {
                etCancelReason.error = "La raison ne peut pas être vide"
                return@setOnClickListener
            }
            cancelAppointment(appointment.id, reason)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancelDialogCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun cancelAppointment(appointmentId: String, reason: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = mapOf("reason" to reason)
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .cancelAppointmentByUser(token, appointmentId, request)

                if (response.isSuccessful) {
                    Toast.makeText(this@PatientAppointmentsActivity, "✅ Rendez-vous annulé", Toast.LENGTH_SHORT).show()
                    loadAppointments()
                } else {
                    Toast.makeText(this@PatientAppointmentsActivity, "❌ Erreur: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PatientAppointments", "❌ Failed to cancel appointment: ${e.message}", e)
                Toast.makeText(this@PatientAppointmentsActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateAppointmentDialog() {
        if (availableDoctors.isEmpty()) {
            Toast.makeText(this, "Aucun médecin disponible pour le moment.", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_appointment, null)
        val doctorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerDoctor)
        val etAppointmentDate = dialogView.findViewById<EditText>(R.id.etAppointmentDate)
        val etAppointmentTime = dialogView.findViewById<EditText>(R.id.etAppointmentTime)
        val etReasonForVisit = dialogView.findViewById<EditText>(R.id.etReasonForVisit)

        // ✅ Setup doctor spinner
        val doctorNames = availableDoctors.map { doctor ->
            val name = doctor.fullName?.takeIf { it.isNotBlank() }
                ?: "${doctor.firstName} ${doctor.lastName}".trim()
            "Dr. $name"
        }
        val doctorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, doctorNames)
        doctorSpinner.setAdapter(doctorAdapter)

        var selectedDoctorId: String? = null
        doctorSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedDoctorId = availableDoctors[position].id
            Log.d("PatientAppointments", "✅ Selected doctor ID: $selectedDoctorId")
        }

        // ✅ Date Picker - Only future dates
        etAppointmentDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Format: yyyy-MM-dd
                    val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etAppointmentDate.setText(formattedDate)
                    Log.d("PatientAppointments", "📅 Date selected: $formattedDate")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        // ✅ Time Picker
        etAppointmentTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    // Format: HH:mm
                    val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                    etAppointmentTime.setText(formattedTime)
                    Log.d("PatientAppointments", "⏰ Time selected: $formattedTime")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnConfirmAppointment).setOnClickListener {
            val dateStr = etAppointmentDate.text.toString().trim()
            val timeStr = etAppointmentTime.text.toString().trim()
            val reason = etReasonForVisit.text.toString().trim()

            // ✅ Validation
            if (selectedDoctorId == null) {
                Toast.makeText(this, "⚠️ Veuillez sélectionner un médecin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dateStr.isEmpty()) {
                Toast.makeText(this, "⚠️ Veuillez sélectionner une date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (timeStr.isEmpty()) {
                Toast.makeText(this, "⚠️ Veuillez sélectionner une heure", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reason.isEmpty()) {
                Toast.makeText(this, "⚠️ Veuillez entrer un motif", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Format DateTime: yyyy-MM-ddTHH:mm:ss
            val appointmentDateTime = try {
                val dateParts = dateStr.split("-")
                val timeParts = timeStr.split(":")

                if (dateParts.size != 3 || timeParts.size != 2) {
                    throw IllegalArgumentException("Format invalide")
                }

                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt()
                val day = dateParts[2].toInt()
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // ✅ CRITICAL: Match Postman format exactly
                String.format(
                    Locale.US,
                    "%04d-%02d-%02dT%02d:%02d:00",
                    year, month, day, hour, minute
                )
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Format de date invalide: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PatientAppointments", "Date parsing error: ${e.message}")
                return@setOnClickListener
            }

            Log.d("PatientAppointments", "════════════════════════════════")
            Log.d("PatientAppointments", "📝 CREATING APPOINTMENT REQUEST")
            Log.d("PatientAppointments", "════════════════════════════════")
            Log.d("PatientAppointments", "Doctor ID: $selectedDoctorId")
            Log.d("PatientAppointments", "DateTime: $appointmentDateTime")
            Log.d("PatientAppointments", "Reason: $reason")
            Log.d("PatientAppointments", "Type: VIDEO_CALL")
            Log.d("PatientAppointments", "════════════════════════════════")

            // ✅ Create request matching Postman format
            val request = AppointmentRequest(
                doctorId = selectedDoctorId!!,
                appointmentDateTime = appointmentDateTime,
                reason = reason,
                appointmentType = "VIDEO_CALL",  // ← or "CONSULTATION"
                notes = null  // ← Optional
            )

            createAppointment(request)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createAppointment(request: AppointmentRequest) {
        Log.d("APPT_CREATE", "════════════════════════════════")
        Log.d("APPT_CREATE", "📤 SENDING REQUEST TO BACKEND")
        Log.d("APPT_CREATE", "════════════════════════════════")
        Log.d("APPT_CREATE", "URL: ${RetrofitClient.getUserBaseUrl()}/api/v1/appointments")
        Log.d("APPT_CREATE", "Doctor ID: ${request.doctorId}")
        Log.d("APPT_CREATE", "DateTime: ${request.appointmentDateTime}")
        Log.d("APPT_CREATE", "Type: ${request.appointmentType}")
        Log.d("APPT_CREATE", "Reason: ${request.reason}")
        Log.d("APPT_CREATE", "Notes: ${request.notes}")
        Log.d("APPT_CREATE", "════════════════════════════════")

        lifecycleScope.launch {
            try {
                // 1. Get token
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    showToast("❌ Session expirée. Reconnectez-vous.")
                    return@launch
                }

                val authHeader = "Bearer $token"
                Log.d("APPT_CREATE", "🔑 Token: ${token.take(30)}...")

                // 2. Call API
                Log.d("APPT_CREATE", "📡 Calling API...")
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .createAppointment(authHeader, request)

                Log.d("APPT_CREATE", "📥 Response Code: ${response.code()}")

                // 3. Handle Success
                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    Log.d("APPT_CREATE", "✅ SUCCESS!")
                    Log.d("APPT_CREATE", "Response: $apiResponse")

                    if (apiResponse?.success == true) {
                        showToast("✅ Rendez-vous créé avec succès!")
                        loadAppointments()
                    } else {
                        showToast("⚠️ ${apiResponse?.message ?: "Erreur inconnue"}")
                    }
                    return@launch
                }

                // 4. Handle Errors
                val errorBody = response.errorBody()?.string()
                Log.e("APPT_CREATE", "❌ ERROR ${response.code()}")
                Log.e("APPT_CREATE", "Error Body: $errorBody")

                // Parse error message
                val errorMessage = try {
                    if (!errorBody.isNullOrEmpty() && errorBody.trim().startsWith("{")) {
                        val errorJson = org.json.JSONObject(errorBody)
                        errorJson.optString("message", "Unknown error")
                    } else {
                        errorBody ?: "Unknown error"
                    }
                } catch (e: Exception) {
                    "Error ${response.code()}"
                }

                // User-friendly messages
                val displayMessage = when (response.code()) {
                    400 -> when {
                        errorMessage.contains("future", ignoreCase = true) ->
                            "❌ La date doit être dans le futur"
                        errorMessage.contains("doctor", ignoreCase = true) ->
                            "❌ Médecin non disponible"
                        errorMessage.contains("required", ignoreCase = true) ->
                            "❌ Tous les champs sont requis"
                        else -> "❌ Données invalides: $errorMessage"
                    }
                    401 -> "❌ Session expirée. Reconnectez-vous."
                    404 -> "❌ Médecin introuvable"
                    409 -> "❌ Rendez-vous déjà existant à cette heure"
                    500 -> "❌ Erreur serveur. Réessayez."
                    else -> "❌ Erreur $errorMessage"
                }

                showToast(displayMessage)

            } catch (e: java.net.UnknownHostException) {
                Log.e("APPT_CREATE", "❌ Network Error", e)
                showToast("❌ Impossible de joindre le serveur")

            } catch (e: Exception) {
                Log.e("APPT_CREATE", "❌ Exception: ${e.javaClass.simpleName}", e)
                Log.e("APPT_CREATE", "Message: ${e.message}")
                e.printStackTrace()
                showToast("❌ Erreur: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun formatDisplayDate(isoDateTime: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("eeee, d MMMM yyyy", Locale.FRENCH))
        } catch (e: Exception) {
            "Date invalide"
        }
    }

    private fun formatDisplayTime(isoDateTime: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH))
        } catch (e: Exception) {
            "Heure invalide"
        }
    }

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH))
        } catch (e: Exception) {
            "Date/Heure invalide"
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}