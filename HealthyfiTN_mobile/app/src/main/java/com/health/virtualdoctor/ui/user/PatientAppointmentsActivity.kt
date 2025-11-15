package com.health.virtualdoctor.ui.user

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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

                    filterAppointments(chipGroupFilter.checkedChipId)

                    Log.d("PatientAppointments", "✅ Loaded ${allAppointments.size} appointments")
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
                apptDateTime.isBefore(now) || !it.status.equals("SCHEDULED", ignoreCase = true)
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

        detailsView.findViewById<TextView>(R.id.tvDoctorNameDetails).text = "Dr. ${appointment.doctorName}"
        detailsView.findViewById<TextView>(R.id.tvAppointmentDateDetails).text = formatDisplayDate(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentTimeDetails).text = formatDisplayTime(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentReasonDetails).text = appointment.reason
        detailsView.findViewById<TextView>(R.id.tvAppointmentStatusDetails).text = appointment.status
        detailsView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAppointmentTypeDetails).text = appointment.appointmentType

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(detailsView)
            .setPositiveButton("Fermer") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun showCancelDialog(appointment: AppointmentResponse) {
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

        // ✅ FIXED: Use fullName or fallback to firstName + lastName
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
            Log.d("PatientAppointments", "Selected doctor ID: $selectedDoctorId")
        }

        // Date Picker - Only allow future dates
        etAppointmentDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Format: yyyy-MM-dd
                    val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etAppointmentDate.setText(formattedDate)
                    Log.d("PatientAppointments", "Date selected: $formattedDate")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set minimum date to today
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        // Time Picker
        etAppointmentTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    // Format: HH:mm
                    val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                    etAppointmentTime.setText(formattedTime)
                    Log.d("PatientAppointments", "Time selected: $formattedTime")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24-hour format
            )
            timePicker.show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnConfirmAppointment).setOnClickListener {
            val dateStr = etAppointmentDate.text.toString()
            val timeStr = etAppointmentTime.text.toString()
            val reason = etReasonForVisit.text.toString()

            if (selectedDoctorId == null || dateStr.isEmpty() || timeStr.isEmpty() || reason.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Format to match MongoDB: yyyy-MM-ddTHH:mm:ss.SSS+00:00
            val appointmentDateTime = try {
                val dateParts = dateStr.split("-")
                val timeParts = timeStr.split(":")

                if (dateParts.size != 3 || timeParts.size != 2) {
                    throw IllegalArgumentException("Format de date/heure invalide")
                }

                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt()
                val day = dateParts[2].toInt()
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // **FIX: Match MongoDB format exactly**
                // Format: yyyy-MM-dd'T'HH:mm:ss.SSS+00:00
                String.format(
                    Locale.US,
                    "%04d-%02d-%02dT%02d:%02d:00.000+00:00",
                    year, month, day, hour, minute
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Format de date/heure invalide: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PatientAppointments", "Date parsing error: ${e.message}")
                return@setOnClickListener
            }


            Log.d("PatientAppointments", "Creating appointment:")
            Log.d("PatientAppointments", "  - Doctor ID: $selectedDoctorId")
            Log.d("PatientAppointments", "  - Doctor Name: ${availableDoctors.find { it.id == selectedDoctorId }?.fullName}")
            Log.d("PatientAppointments", "  - DateTime: $appointmentDateTime")
            Log.d("PatientAppointments", "  - Reason: $reason")
            Log.d("PatientAppointments", "  - Type: VIDEO_CALL")

            val request = AppointmentRequest(
                doctorId = selectedDoctorId!!,
                appointmentDateTime = appointmentDateTime,
                reason  = reason,
                appointmentType = "VIDEO_CALL"
            )
            createAppointment(request)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // REPLACE ONLY the createAppointment function in your PatientAppointmentsActivity.kt
// Starting around line 416

    private fun createAppointment(request: AppointmentRequest) {
        Log.d("APPT_DEBUG", "===== CREATE APPOINTMENT REQUEST =====")
        Log.d("APPT_DEBUG", "Doctor ID: ${request.doctorId}")
        Log.d("APPT_DEBUG", "DateTime: ${request.appointmentDateTime}")
        Log.d("APPT_DEBUG", "Reason: ${request.reason}")
        Log.d("APPT_DEBUG", "Type: ${request.appointmentType}")

        lifecycleScope.launch {
            try {
                // Vérification du token
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@PatientAppointmentsActivity, "❌ Token invalide. Veuillez vous reconnecter.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val authHeader = "Bearer $token"

                // Appel API
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .createAppointment(authHeader, request)

                Log.d("APPT_DEBUG", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    Toast.makeText(this@PatientAppointmentsActivity, "✅ Rendez-vous créé avec succès!", Toast.LENGTH_LONG).show()
                    loadAppointments()
                    return@launch
                }

                // Gestion des erreurs HTTP
                val errorBody = response.errorBody()?.string()
                val statusCode = response.code()

                Log.e("APPT_DEBUG", "❌ HTTP Error $statusCode")
                Log.e("APPT_DEBUG", "Error body: $errorBody")

                val errorMessage = when (statusCode) {
                    400 -> {
                        when {
                            errorBody?.contains("date", ignoreCase = true) == true ->
                                "❌ Date invalide. Vérifiez le format et l'heure."
                            errorBody?.contains("doctor", ignoreCase = true) == true ->
                                "❌ Médecin non disponible à cette heure."
                            errorBody?.contains("time", ignoreCase = true) == true ->
                                "❌ Horaire invalide. Choisissez une heure de travail."
                            else -> "❌ Données invalides. Vérifiez tous les champs."
                        }
                    }
                    401 -> "❌ Session expirée. Veuillez vous reconnecter."
                    403 -> "❌ Vous n'avez pas l'autorisation de créer ce rendez-vous."
                    404 -> "❌ Médecin non trouvé."
                    409 -> "❌ Conflit: Un rendez-vous existe déjà à cette heure."
                    422 -> "❌ Données de validation incorrectes."
                    500 -> "❌ Erreur serveur temporaire. Réessayez dans quelques minutes."
                    502, 503, 504 -> "❌ Service temporairement indisponible."
                    else -> "❌ Erreur inconnue ($statusCode)"
                }

                Toast.makeText(this@PatientAppointmentsActivity, errorMessage, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e("APPT_DEBUG", "❌ Network exception: ${e.message}", e)
                Toast.makeText(this@PatientAppointmentsActivity, "❌ Erreur réseau: ${e.message ?: "Vérifiez votre connexion"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleServerError(response: retrofit2.Response<*>) {
        val errorBody = response.errorBody()?.string()
        android.util.Log.e("APPT_DEBUG", "❌ SERVER ERROR ${response.code()}")
        android.util.Log.e("APPT_DEBUG", "Error body: $errorBody")

        // Try to extract meaningful error message
        val errorMessage = extractErrorMessage(errorBody, response.code())

        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun extractErrorMessage(errorBody: String?, statusCode: Int): String {
        return try {
            if (!errorBody.isNullOrEmpty()) {
                // Try to parse as JSON first
                if (errorBody.trim().startsWith("{")) {
                    val errorJson = org.json.JSONObject(errorBody)

                    // Check common error field names
                    listOf("message", "error", "detail", "reason", "errorMessage").forEach { field ->
                        if (errorJson.has(field)) {
                            val value = errorJson.optString(field, "")
                            if (value.isNotEmpty()) {
                                android.util.Log.e("APPT_DEBUG", "Found error in field '$field': $value")
                                return value
                            }
                        }
                    }

                    // If no specific field found, return the whole JSON
                    errorJson.toString()
                } else {
                    // Not JSON, return raw body
                    errorBody
                }
            } else {
                when (statusCode) {
                    500 -> "Erreur interne du serveur. Veuillez réessayer plus tard."
                    400 -> "Données invalides envoyées au serveur."
                    else -> "Erreur $statusCode"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("APPT_DEBUG", "Error parsing error message: ${e.message}")
            "Erreur $statusCode: ${errorBody ?: "Unknown error"}"
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

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}