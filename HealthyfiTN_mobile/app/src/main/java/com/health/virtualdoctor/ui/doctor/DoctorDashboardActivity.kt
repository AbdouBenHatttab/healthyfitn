package com.health.virtualdoctor.ui.doctor

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.health.virtualdoctor.R

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivDoctorProfile: ImageView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var tvRating: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var tvTodayAppointments: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var chipFilter: Chip
    private lateinit var rvAppointments: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabNewAppointment: ExtendedFloatingActionButton

    private lateinit var appointmentsAdapter: AppointmentsAdapter
    private val appointmentsList = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        window.statusBarColor = resources.getColor(R.color.white, theme)

        initViews()
        setupToolbar()
        setupListeners()
        loadDoctorData()
        loadAppointments()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivDoctorProfile = findViewById(R.id.ivDoctorProfile)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        tvRating = findViewById(R.id.tvRating)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        tvTodayAppointments = findViewById(R.id.tvTodayAppointments)
        tvTotalPatients = findViewById(R.id.tvTotalPatients)
        tvRevenue = findViewById(R.id.tvRevenue)
        chipFilter = findViewById(R.id.chipFilter)
        rvAppointments = findViewById(R.id.rvAppointments)
        llEmptyState = findViewById(R.id.llEmptyState)
        fabNewAppointment = findViewById(R.id.fabNewAppointment)

        // Setup RecyclerView
        appointmentsAdapter = AppointmentsAdapter(appointmentsList) { appointment, action ->
            handleAppointmentAction(appointment, action)
        }
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.adapter = appointmentsAdapter
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Paramètres", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                Toast.makeText(this, "Déconnexion", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        chipFilter.setOnClickListener {
            Toast.makeText(this, "Filtrer les consultations", Toast.LENGTH_SHORT).show()
        }

        fabNewAppointment.setOnClickListener {
            Toast.makeText(this, "Nouvelle consultation", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDoctorData() {
        // Simuler le chargement des données du médecin
        tvDoctorName.text = "Dr. Jean Dupont"
        tvSpecialization.text = "Cardiologue"
        tvRating.text = "4.8 (120 avis)"
        tvTodayAppointments.text = "8"
        tvTotalPatients.text = "142"
        tvRevenue.text = "12.5K"
    }

    private fun loadAppointments() {
        // Simuler le chargement des consultations
        appointmentsList.clear()
        appointmentsList.addAll(
            listOf(
                Appointment(
                    id = "1",
                    patientName = "Alice Martin",
                    patientAge = "32 ans • Femme",
                    time = "14:30",
                    date = "05 Nov 2025",
                    reason = "Consultation de suivi",
                    status = "En ligne",
                    statusColor = "#E8F5E9",
                    statusTextColor = "#2E7D32"
                ),
                Appointment(
                    id = "2",
                    patientName = "Marc Dubois",
                    patientAge = "45 ans • Homme",
                    time = "15:00",
                    date = "05 Nov 2025",
                    reason = "Contrôle cardiaque",
                    status = "Présentiel",
                    statusColor = "#E3F2FD",
                    statusTextColor = "#1976D2"
                ),
                Appointment(
                    id = "3",
                    patientName = "Sophie Laurent",
                    patientAge = "28 ans • Femme",
                    time = "16:00",
                    date = "05 Nov 2025",
                    reason = "Première consultation",
                    status = "En ligne",
                    statusColor = "#E8F5E9",
                    statusTextColor = "#2E7D32"
                ),
                Appointment(
                    id = "4",
                    patientName = "Pierre Bernard",
                    patientAge = "55 ans • Homme",
                    time = "16:30",
                    date = "05 Nov 2025",
                    reason = "Résultats d'examens",
                    status = "Présentiel",
                    statusColor = "#E3F2FD",
                    statusTextColor = "#1976D2"
                )
            )
        )

        appointmentsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (appointmentsList.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            rvAppointments.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvAppointments.visibility = View.VISIBLE
        }
    }

    private fun handleAppointmentAction(appointment: Appointment, action: String) {
        when (action) {
            "view_details" -> {
                Toast.makeText(
                    this,
                    "Détails: ${appointment.patientName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            "start_consultation" -> {
                Toast.makeText(
                    this,
                    "Démarrer consultation avec ${appointment.patientName}",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Démarrer la consultation vidéo
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Initialiser les vues du dialog
        val etFirstName = dialog.findViewById<TextInputEditText>(R.id.etEditFirstName)
        val etLastName = dialog.findViewById<TextInputEditText>(R.id.etEditLastName)
        val etSpecialization = dialog.findViewById<TextInputEditText>(R.id.etEditSpecialization)
        val etPhone = dialog.findViewById<TextInputEditText>(R.id.etEditPhone)
        val etClinicName = dialog.findViewById<TextInputEditText>(R.id.etEditClinicName)
        val etConsultationFee = dialog.findViewById<TextInputEditText>(R.id.etEditConsultationFee)
        val etBio = dialog.findViewById<TextInputEditText>(R.id.etEditBio)
        val btnChangePhoto = dialog.findViewById<MaterialButton>(R.id.btnChangePhoto)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSave)

        // Pré-remplir avec les données actuelles
        etFirstName.setText("Jean")
        etLastName.setText("Dupont")
        etSpecialization.setText("Cardiologue")
        etPhone.setText("+216 20 123 456")
        etClinicName.setText("Clinique du Cœur")
        etConsultationFee.setText("80")
        etBio.setText("Cardiologue expérimenté avec plus de 15 ans de pratique dans le domaine de la cardiologie interventionnelle.")

        btnChangePhoto.setOnClickListener {
            Toast.makeText(this, "Changer la photo", Toast.LENGTH_SHORT).show()
            // TODO: Implémenter le sélecteur d'image
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            // Sauvegarder les modifications
            val firstName = etFirstName.text.toString()
            val lastName = etLastName.text.toString()
            val specialization = etSpecialization.text.toString()

            tvDoctorName.text = "Dr. $firstName $lastName"
            tvSpecialization.text = specialization

            Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}

// Data class pour les consultations
data class Appointment(
    val id: String,
    val patientName: String,
    val patientAge: String,
    val time: String,
    val date: String,
    val reason: String,
    val status: String,
    val statusColor: String,
    val statusTextColor: String
)