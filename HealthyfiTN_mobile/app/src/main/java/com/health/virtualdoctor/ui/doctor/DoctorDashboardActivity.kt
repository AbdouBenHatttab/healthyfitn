package com.health.virtualdoctor.ui.doctor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
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
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etSpecialization: EditText
    private lateinit var etHospital: EditText
    private lateinit var etYearsOfExperience: EditText
    private lateinit var etOfficeAddress: EditText
    private lateinit var etConsultationHours: EditText
    private lateinit var btnUpdateProfile: Button
    private lateinit var btnCheckActivation: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnEditProfile: com.google.android.material.button.MaterialButton
    private lateinit var cardEditProfile: androidx.cardview.widget.CardView

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
        setupToolbar()
        setupListeners()
        setupRecyclerView()

        loadDoctorProfile()
        loadDashboardStats()
        loadPatients()
    }

    private fun initViews() {


        // Back button
        //

        // ➕ Add this (Manage Appointments button)
        btnManageAppointments = findViewById(R.id.btnManageAppointments)

        // Profile views

        ivDoctorProfile = findViewById(R.id.ivDoctorProfile)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDoctorEmail = findViewById(R.id.tvDoctorEmail)
        tvActivationStatus = findViewById(R.id.tvActivationStatus)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etSpecialization = findViewById(R.id.etSpecialization)
        etHospital = findViewById(R.id.etHospital)
        etYearsOfExperience = findViewById(R.id.etYearsOfExperience)
        etOfficeAddress = findViewById(R.id.etOfficeAddress)
        etConsultationHours = findViewById(R.id.etConsultationHours)
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        btnCheckActivation = findViewById(R.id.btnCheckActivation)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        cardEditProfile = findViewById(R.id.cardEditProfile)

        // Statistics views
        tvTodayAppointments = findViewById(R.id.tvTodayAppointments)
        tvTotalPatients = findViewById(R.id.tvTotalPatients)
        tvPendingAppointments = findViewById(R.id.tvPendingAppointments)

        // Appointments views
        rvPatients = findViewById(R.id.rvPatients)
        llEmptyPatients = findViewById(R.id.llEmptyPatients)
        tvPatientsCount = findViewById(R.id.tvPatientsCount)

    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, "🔔 Notifications", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_settings -> {
                Toast.makeText(this, "⚙️ Paramètres", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_logout -> {
                showLogoutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupListeners() {

//        // Back button
//        btnBack.setOnClickListener {
//            finish()
//        }

        // Manage Appointments
        btnManageAppointments.setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
        }

        // Update profile
        btnUpdateProfile.setOnClickListener {
            updateDoctorProfile()
        }

        // Check activation
        btnCheckActivation.setOnClickListener {
            checkActivationStatus()
        }

        // Change password
        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Edit profile section toggle
        btnEditProfile.setOnClickListener {
            toggleEditProfileVisibility()
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

                        etFirstName.setText(profile.firstName)
                        etLastName.setText(profile.lastName)
                        etPhoneNumber.setText(profile.phoneNumber ?: "")
                        etSpecialization.setText(profile.specialization)
                        etHospital.setText(profile.hospitalAffiliation)
                        etYearsOfExperience.setText(profile.yearsOfExperience.toString())
                        etOfficeAddress.setText(profile.officeAddress ?: "")
                        etConsultationHours.setText(profile.consultationHours ?: "")

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

    private fun updateDoctorProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val specialization = etSpecialization.text.toString().trim()
        val hospital = etHospital.text.toString().trim()
        val yearsOfExperience = etYearsOfExperience.text.toString().trim().toIntOrNull()
        val officeAddress = etOfficeAddress.text.toString().trim()
        val consultationHours = etConsultationHours.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || specialization.isEmpty()) {
            Toast.makeText(this, "⚠️ Required fields are missing", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                btnUpdateProfile.isEnabled = false
                btnUpdateProfile.text = "Updating..."

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
                        Toast.makeText(
                            this@DoctorDashboardActivity,
                            "✅ Image uploaded!",
                            Toast.LENGTH_SHORT
                        ).show()

                        runOnUiThread {
                            loadProfileImage(imageUrl)
                        }
                    } else {
                        Log.e("DoctorDashboard", "❌ Image upload failed")
                        Toast.makeText(
                            this@DoctorDashboardActivity,
                            "⚠️ Image upload failed",
                            Toast.LENGTH_SHORT
                        ).show()
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

                Log.d("DoctorDashboard", "📤 Updating profile with imageUrl: $finalImageUrl")

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .updateDoctorProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val updatedProfile = response.body()!!

                    runOnUiThread {
                        tvDoctorName.text = updatedProfile.fullName
                        tvSpecialization.text = updatedProfile.specialization

                        currentProfileImageUrl = updatedProfile.profilePictureUrl
                        selectedImageBitmap = null

                        if (!currentProfileImageUrl.isNullOrEmpty()) {
                            loadProfileImage(currentProfileImageUrl!!)
                        }

                        Toast.makeText(
                            this@DoctorDashboardActivity,
                            "✅ Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        toggleEditProfileVisibility()
                    }

                    Log.d("DoctorDashboard", "✅ Profile updated: ${updatedProfile.email}")
                } else {
                    val error = response.errorBody()?.string() ?: "Update failed"
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
            } finally {
                runOnUiThread {
                    btnUpdateProfile.isEnabled = true
                    btnUpdateProfile.text = "Enregistrer"
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

    private fun toggleEditProfileVisibility() {
        if (cardEditProfile.visibility == View.VISIBLE) {
            cardEditProfile.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    cardEditProfile.visibility = View.GONE
                }
        } else {
            cardEditProfile.visibility = View.VISIBLE
            cardEditProfile.alpha = 0f
            cardEditProfile.animate()
                .alpha(1f)
                .setDuration(300)
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
