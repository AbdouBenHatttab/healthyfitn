package com.health.virtualdoctor.ui.doctor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.AppointmentResponse
import com.health.virtualdoctor.ui.data.models.ChangePasswordRequest
import com.health.virtualdoctor.ui.data.models.DoctorStatsResponse
import com.health.virtualdoctor.ui.data.models.UpdateDoctorProfileRequest
import com.health.virtualdoctor.ui.utils.ImageUploadHelper
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import com.health.virtualdoctor.ui.data.models.PatientInfoResponse

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    private lateinit var btnManageAppointments: MaterialButton

    // Views - Profile Section
    private lateinit var ivDoctorProfile: ImageView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorEmail: TextView
    private lateinit var tvActivationStatus: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var btnEditProfile: MaterialCardView
    private lateinit var btnViewAllPatients: MaterialButton
    private lateinit var btnNotifications: ImageButton

    // Profile data for dialog
    private var currentFirstName: String = ""
    private var currentLastName: String = ""
    private var currentPhoneNumber: String = ""
    private var currentSpecialization: String = ""
    private var currentHospital: String = ""
    private var currentYearsOfExperience: Int = 0
    private var currentOfficeAddress: String = ""
    private var currentConsultationHours: String = ""

    // Views - Statistics Section
    private lateinit var tvTodayAppointments: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvPendingAppointments: TextView

    // Views - Appointments Section
    private lateinit var rvPatients: RecyclerView
    private lateinit var llEmptyPatients: LinearLayout
    private lateinit var tvPatientsCount: TextView
    private lateinit var patientsAdapter: DoctorPatientsAdapter

    // Image handling
    private var selectedImageBitmap: Bitmap? = null
    private var currentProfileImageUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                ivDoctorProfile.setImageBitmap(selectedImageBitmap)
                Toast.makeText(this, "✅ Image sélectionnée", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("DoctorDashboard", "Error loading image: ${e.message}")
                Toast.makeText(this, "❌ Erreur chargement image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickImage()
        } else {
            Toast.makeText(this, "❌ Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupRecyclerView()

        loadDoctorProfile()
        loadDashboardStats()
        loadPatients()
    }

    private fun initViews() {

        // Manage Appointments button
        btnManageAppointments = findViewById(R.id.btnManageAppointments)

        // Profile views
        ivDoctorProfile = findViewById(R.id.ivDoctorProfile)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDoctorEmail = findViewById(R.id.tvDoctorEmail)
        tvActivationStatus = findViewById(R.id.tvActivationStatus)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnViewAllPatients = findViewById(R.id.btnViewAllPatients)
        btnNotifications = findViewById(R.id.btnNotifications)

        // Statistics views
        tvTodayAppointments = findViewById(R.id.tvTodayAppointments)
        tvTotalPatients = findViewById(R.id.tvTotalPatients)
        tvPendingAppointments = findViewById(R.id.tvPendingAppointments)

        // Appointments views
        rvPatients = findViewById(R.id.rvPatients)
        llEmptyPatients = findViewById(R.id.llEmptyPatients)
        tvPatientsCount = findViewById(R.id.tvPatientsCount)

    }

    private fun setupListeners() {

        // Notifications button
        btnNotifications.setOnClickListener {
            Toast.makeText(this, "🔔 Notifications", Toast.LENGTH_SHORT).show()
        }

        // View All Patients button
        btnViewAllPatients.setOnClickListener {
            Toast.makeText(this, "👥 Liste des patients", Toast.LENGTH_SHORT).show()
        }

        // Manage Appointments
        btnManageAppointments.setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
        }

        // Edit profile - show dialog
        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Profile image click
        ivDoctorProfile.setOnClickListener {
            showImagePickerDialog()
        }
    }


    private fun setupRecyclerView() {
        patientsAdapter = DoctorPatientsAdapter(emptyList()) { patient ->
            showPatientDetails(patient)
        }

        rvPatients.apply {
            layoutManager = LinearLayoutManager(this@DoctorDashboardActivity)
            adapter = patientsAdapter
        }
    }


    private fun loadPatients() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .getDoctorPatients(token)

                if (response.isSuccessful && response.body() != null) {
                    val patients = response.body()!!

                    runOnUiThread {
                        if (patients.isEmpty()) {
                            rvPatients.visibility = View.GONE
                            llEmptyPatients.visibility = View.VISIBLE
                            tvPatientsCount.text = "0 patients"
                        } else {
                            rvPatients.visibility = View.VISIBLE
                            llEmptyPatients.visibility = View.GONE
                            patientsAdapter.updatePatients(patients)
                            tvPatientsCount.text = "${patients.size} patients"
                        }
                    }
                } else {
                    runOnUiThread {
                        rvPatients.visibility = View.GONE
                        llEmptyPatients.visibility = View.VISIBLE
                        tvPatientsCount.text = "0 patients"
                    }
                }
            } catch (e: Exception) {
                Log.e("DoctorDashboard", "Error loading patients: ${e.message}", e)
                runOnUiThread {
                    rvPatients.visibility = View.GONE
                    llEmptyPatients.visibility = View.VISIBLE
                    tvPatientsCount.text = "0 patients"
                }
            }
        }
    }

    private fun showPatientDetails(patient: PatientInfoResponse) {
        val message = """
        👤 ${patient.patientName}
        📧 ${patient.patientEmail}
        📞 ${patient.patientPhone ?: "Non disponible"}
        
        📊 Statistiques des rendez-vous:
        • Total: ${patient.totalAppointments}
        • Complétés: ${patient.completedAppointments}
        • Annulés: ${patient.cancelledAppointments}
        
        📅 Dates importantes:
        • Première visite: ${patient.firstVisitDate?.substringBefore("T") ?: "Non disponible"}
        • Dernier RDV: ${patient.lastAppointmentDate?.substringBefore("T") ?: "Aucun"}
        • Prochain RDV: ${patient.nextAppointmentDate?.substringBefore("T") ?: "Aucun"}
    """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Détails du patient")
            .setMessage(message)
            .setPositiveButton("Fermer") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }





    private fun loadDashboardStats() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .getDoctorStats(token)

                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!

                    runOnUiThread {
                        tvTodayAppointments.text = stats.todayAppointments.toString()
                        tvTotalPatients.text = stats.totalPatients.toString()
                        tvPendingAppointments.text = stats.todayPending.toString()
                    }
                }
            } catch (e: Exception) {
                Log.e("DoctorDashboard", "Error loading stats: ${e.message}", e)
            }
        }
    }

    private fun loadDoctorProfile() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .getDoctorProfile(token)

                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!

                    runOnUiThread {
                        tvDoctorName.text = profile.fullName
                        tvDoctorEmail.text = profile.email
                        tvSpecialization.text = profile.specialization
                        tvActivationStatus.text = if (profile.isActivated) {
                            "✅ Activated"
                        } else {
                            "⏳ Pending Activation"
                        }

                        // Store profile data for edit dialog
                        currentFirstName = profile.firstName
                        currentLastName = profile.lastName
                        currentPhoneNumber = profile.phoneNumber ?: ""
                        currentSpecialization = profile.specialization
                        currentHospital = profile.hospitalAffiliation
                        currentYearsOfExperience = profile.yearsOfExperience
                        currentOfficeAddress = profile.officeAddress ?: ""
                        currentConsultationHours = profile.consultationHours ?: ""

                        currentProfileImageUrl = profile.profilePictureUrl
                        if (!currentProfileImageUrl.isNullOrEmpty()) {
                            loadProfileImage(currentProfileImageUrl!!)
                        }
                    }

                    Log.d("DoctorDashboard", "✅ Profile loaded: ${profile.email}")
                } else {
                    val error = response.errorBody()?.string() ?: "Error loading profile"
                    Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("DoctorDashboard", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this@DoctorDashboardActivity)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .into(ivDoctorProfile)

        Log.d("DoctorDashboard", "✅ Profile image loaded: $imageUrl")
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_doctor_edit_profile, null)
        
        // Get dialog views
        val etFirstName = dialogView.findViewById<EditText>(R.id.etFirstName)
        val etLastName = dialogView.findViewById<EditText>(R.id.etLastName)
        val etPhoneNumber = dialogView.findViewById<EditText>(R.id.etPhoneNumber)
        val etSpecialization = dialogView.findViewById<EditText>(R.id.etSpecialization)
        val etHospital = dialogView.findViewById<EditText>(R.id.etHospital)
        val etYearsOfExperience = dialogView.findViewById<EditText>(R.id.etYearsOfExperience)
        val etOfficeAddress = dialogView.findViewById<EditText>(R.id.etOfficeAddress)
        val etConsultationHours = dialogView.findViewById<EditText>(R.id.etConsultationHours)
        val btnCheckActivation = dialogView.findViewById<Button>(R.id.btnCheckActivation)
        val btnChangePassword = dialogView.findViewById<Button>(R.id.btnChangePassword)

        // Pre-fill with current data
        etFirstName.setText(currentFirstName)
        etLastName.setText(currentLastName)
        etPhoneNumber.setText(currentPhoneNumber)
        etSpecialization.setText(currentSpecialization)
        etHospital.setText(currentHospital)
        etYearsOfExperience.setText(currentYearsOfExperience.toString())
        etOfficeAddress.setText(currentOfficeAddress)
        etConsultationHours.setText(currentConsultationHours)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Enregistrer", null) // Set to null initially
            .setNegativeButton("Annuler", null)
            .create()

        // Set up inner button listeners
        btnCheckActivation.setOnClickListener {
            checkActivationStatus()
        }

        btnChangePassword.setOnClickListener {
            dialog.dismiss()
            showChangePasswordDialog()
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val firstName = etFirstName.text.toString().trim()
                val lastName = etLastName.text.toString().trim()
                val phoneNumber = etPhoneNumber.text.toString().trim()
                val specialization = etSpecialization.text.toString().trim()
                val hospital = etHospital.text.toString().trim()
                val yearsOfExperience = etYearsOfExperience.text.toString().trim().toIntOrNull()
                val officeAddress = etOfficeAddress.text.toString().trim()
                val consultationHours = etConsultationHours.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty() || specialization.isEmpty()) {
                    Toast.makeText(this, "⚠️ Champs requis manquants", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                updateDoctorProfile(
                    firstName, lastName, phoneNumber, specialization,
                    hospital, yearsOfExperience, officeAddress, consultationHours,
                    dialog
                )
            }
        }

        dialog.show()
    }

    private fun updateDoctorProfile(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        specialization: String,
        hospital: String,
        yearsOfExperience: Int?,
        officeAddress: String,
        consultationHours: String,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                var imageUrl = currentProfileImageUrl

                if (selectedImageBitmap != null) {
                    Toast.makeText(
                        this@DoctorDashboardActivity,
                        "📤 Uploading image...",
                        Toast.LENGTH_SHORT
                    ).show()

                    imageUrl = ImageUploadHelper.uploadImage(selectedImageBitmap!!, "doctors")

                    if (imageUrl != null) {
                        Log.d("DoctorDashboard", "✅ Image uploaded: $imageUrl")
                        runOnUiThread {
                            loadProfileImage(imageUrl)
                        }
                    }
                }

                val finalImageUrl = imageUrl ?: currentProfileImageUrl

                val request = UpdateDoctorProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber.ifEmpty { null },
                    specialization = specialization,
                    hospitalAffiliation = hospital,
                    yearsOfExperience = yearsOfExperience,
                    officeAddress = officeAddress.ifEmpty { null },
                    consultationHours = consultationHours.ifEmpty { null },
                    profilePictureUrl = finalImageUrl
                )

                Log.d("DoctorDashboard", "📤 Updating profile...")

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .updateDoctorProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val updatedProfile = response.body()!!

                    runOnUiThread {
                        tvDoctorName.text = updatedProfile.fullName
                        tvSpecialization.text = updatedProfile.specialization

                        // Update stored values
                        currentFirstName = firstName
                        currentLastName = lastName
                        currentPhoneNumber = phoneNumber
                        currentSpecialization = specialization
                        currentHospital = hospital
                        currentYearsOfExperience = yearsOfExperience ?: 0
                        currentOfficeAddress = officeAddress
                        currentConsultationHours = consultationHours

                        currentProfileImageUrl = updatedProfile.profilePictureUrl
                        selectedImageBitmap = null

                        if (!currentProfileImageUrl.isNullOrEmpty()) {
                            loadProfileImage(currentProfileImageUrl!!)
                        }

                        Toast.makeText(
                            this@DoctorDashboardActivity,
                            "✅ Profil mis à jour!",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    }

                    Log.d("DoctorDashboard", "✅ Profile updated: ${updatedProfile.email}")
                } else {
                    val error = response.errorBody()?.string() ?: "Update failed"
                    runOnUiThread {
                        Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("DoctorDashboard", "❌ Exception: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@DoctorDashboardActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("Changer le mot de passe")
            .setView(dialogView)
            .setPositiveButton("Changer") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(this, "⚠️ Tous les champs requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "⚠️ Mots de passe non identiques", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                if (newPassword.length < 8) {
                    Toast.makeText(this, "⚠️ Minimum 8 caractères", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changeDoctorPassword(currentPassword, newPassword)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changeDoctorPassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = ChangePasswordRequest(currentPassword, newPassword)

                Log.d("DoctorDashboard", "🔐 Changing doctor password...")

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .changeDoctorPassword(token, request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DoctorDashboardActivity,
                        "✅ Mot de passe changé!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("DoctorDashboard", "✅ Password changed successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DoctorDashboard", "❌ Password change error: $errorBody")

                    Toast.makeText(
                        this@DoctorDashboardActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("DoctorDashboard", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkActivationStatus() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .getDoctorActivationStatus(token)

                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    val isActivated = status["isActivated"] as? Boolean ?: false
                    val message = status["message"] as? String ?: "Unknown"

                    runOnUiThread {
                        tvActivationStatus.text = if (isActivated) {
                            "✅ Activated"
                        } else {
                            "⏳ $message"
                        }

                        Toast.makeText(this@DoctorDashboardActivity, message, Toast.LENGTH_LONG)
                            .show()
                    }

                    Log.d("DoctorDashboard", "✅ Activation status: $isActivated")
                } else {
                    val error = response.errorBody()?.string() ?: "Failed to check status"
                    Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("DoctorDashboard", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Galerie", "Appareil photo", "Annuler")

        MaterialAlertDialogBuilder(this)
            .setTitle("Changer la photo de profil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkPermissionAndPickImage()
                    1 -> Toast.makeText(
                        this,
                        "📷 Appareil photo (à implémenter)",
                        Toast.LENGTH_SHORT
                    ).show()

                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkPermissionAndPickImage() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                pickImage()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                tokenManager.clearTokens()
                Log.d("DoctorDashboard", "✅ Logout successful")
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "👋 Déconnecté avec succès",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@DoctorDashboardActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e("DoctorDashboard", "❌ Logout error: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Erreur lors de la déconnexion",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPatients()
        loadDashboardStats()
    }
}
