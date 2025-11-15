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
    private var availableDoctors = listOf<Map<String, Any>>()

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
                // If no chip is selected, default to showing upcoming appointments
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
                    allAppointments = apiResponse.data?.sortedByDescending { LocalDateTime.parse(it.appointmentDateTime, DateTimeFormatter.ISO_DATE_TIME) } ?: emptyList()

                    // Default filter
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

        detailsView.findViewById<TextView>(R.id.tvDoctorNameDetails).text = "Dr. ${appointment.doctorName}"
        detailsView.findViewById<TextView>(R.id.tvAppointmentDateDetails).text = formatDisplayDate(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentTimeDetails).text = formatDisplayTime(appointment.appointmentDateTime)
        detailsView.findViewById<TextView>(R.id.tvAppointmentReasonDetails).text = appointment.reason
        detailsView.findViewById<TextView>(R.id.tvAppointmentStatusDetails).text = appointment.status
        detailsView.findViewById<Chip>(R.id.chipAppointmentTypeDetails).text = appointment.appointmentType

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
                    loadAppointments() // Refresh list
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

        // Doctor Spinner
        val doctorNames = availableDoctors.map { "Dr. ${it["firstName"]} ${it["lastName"]}" }
        val doctorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, doctorNames)
        doctorSpinner.setAdapter(doctorAdapter)

        var selectedDoctorId: String? = null
        doctorSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedDoctorId = availableDoctors[position]["id"] as? String
        }

        // Date Picker
        etAppointmentDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    etAppointmentDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time Picker
        etAppointmentTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    etAppointmentTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
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

            val appointmentDateTime = "${dateStr}T${timeStr}:00"
            val request = AppointmentRequest(
                doctorId = selectedDoctorId!!,
                appointmentDateTime = appointmentDateTime,
                reason = reason,
                notes = "",
                appointmentType = "VIDEO_CALL" // Or get from UI
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
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val response = RetrofitClient.getUserService(this@PatientAppointmentsActivity)
                    .createAppointment(token, request)

                if (response.isSuccessful) {
                    Toast.makeText(this@PatientAppointmentsActivity, "✅ Rendez-vous créé!", Toast.LENGTH_SHORT).show()
                    loadAppointments() // Refresh list
                } else {
                     val errorBody = response.errorBody()?.string()
                    Log.e("PatientAppointments", "Failed to create appointment: ${response.code()} - $errorBody")
                    Toast.makeText(this@PatientAppointmentsActivity, "❌ Erreur: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("PatientAppointments", "❌ Exception on create appointment: ${e.message}", e)
                Toast.makeText(this@PatientAppointmentsActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    // =========================================
    // HELPER FUNCTIONS
    // =========================================
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
