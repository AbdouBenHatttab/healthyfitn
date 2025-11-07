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
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.UpdateDoctorProfileRequest
import com.health.virtualdoctor.ui.utils.ImageUploadHelper
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    // Views
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
    private lateinit var btnEditProfile: com.google.android.material.button.MaterialButton
    private lateinit var cardEditProfile: androidx.cardview.widget.CardView

    // Image selection
    private var selectedImageBitmap: Bitmap? = null
    private var currentProfileImageUrl: String? = null  // ✅ FIX : Nom correct de la variable

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                ivDoctorProfile.setImageBitmap(selectedImageBitmap)
                Toast.makeText(this, "✅ Image sélectionnée", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("DoctorProfile", "Error loading image: ${e.message}")
                Toast.makeText(this, "❌ Erreur chargement image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission Launcher
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
        setupToolbar()  // 🆕 AJOUTER
        setupListeners()
        loadDoctorProfile()
    }

    // 🆕 Configurer la toolbar
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    // 🆕 Créer le menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_dashboard, menu)
        return true
    }

    // 🆕 Gérer les clics sur le menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, "🔔 Notifications", Toast.LENGTH_SHORT).show()
                // TODO: Ouvrir l'écran des notifications
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "⚙️ Paramètres", Toast.LENGTH_SHORT).show()
                // TODO: Ouvrir l'écran des paramètres
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 🆕 Afficher une boîte de dialogue de confirmation avant logout
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // 🆕 Effectuer la déconnexion
    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // 1. Supprimer le token local
                tokenManager.clearTokens()

                // 2. (Optionnel) Informer le backend de la déconnexion
                // val token = "Bearer ${tokenManager.getAccessToken()}"
                // RetrofitClient.getAuthService().logout(token)

                Log.d("DoctorProfile", "✅ Logout successful")
                Toast.makeText(this@DoctorDashboardActivity, "👋 Déconnecté avec succès", Toast.LENGTH_SHORT).show()

                // 3. Rediriger vers l'écran de connexion
                val intent = Intent(this@DoctorDashboardActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e("DoctorProfile", "❌ Logout error: ${e.message}", e)
                Toast.makeText(this@DoctorDashboardActivity, "❌ Erreur lors de la déconnexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
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
        btnEditProfile = findViewById(R.id.btnEditProfile)
        cardEditProfile = findViewById(R.id.cardEditProfile)
    }

    private fun setupListeners() {
        btnUpdateProfile.setOnClickListener {
            updateDoctorProfile()
        }

        btnCheckActivation.setOnClickListener {
            checkActivationStatus()
        }

        btnEditProfile.setOnClickListener {
            toggleEditProfileVisibility()
        }

        ivDoctorProfile.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Galerie", "Appareil photo", "Annuler")

        MaterialAlertDialogBuilder(this)
            .setTitle("Changer la photo de profil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkPermissionAndPickImage()
                    1 -> Toast.makeText(this, "📷 Appareil photo (à implémenter)", Toast.LENGTH_SHORT).show()
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

    private fun loadDoctorProfile() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .getDoctorProfile(token)

                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!

                    tvDoctorName.text = profile.fullName
                    tvDoctorEmail.text = profile.email
                    tvSpecialization.text = profile.specialization
                    tvActivationStatus.text = if (profile.isActivated) {
                        "✅ Activated"
                    } else {
                        "⏳ Pending Activation"
                    }

                    // Pre-fill edit fields
                    etFirstName.setText(profile.firstName)
                    etLastName.setText(profile.lastName)
                    etPhoneNumber.setText(profile.phoneNumber ?: "")
                    etSpecialization.setText(profile.specialization)
                    etHospital.setText(profile.hospitalAffiliation)
                    etYearsOfExperience.setText(profile.yearsOfExperience.toString())
                    etOfficeAddress.setText(profile.officeAddress ?: "")
                    etConsultationHours.setText(profile.consultationHours ?: "")

                    // ✅ FIX : Utiliser le nom correct de la variable
                    currentProfileImageUrl = profile.profilePictureUrl
                    if (!currentProfileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@DoctorDashboardActivity)
                            .load(currentProfileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivDoctorProfile)

                        Log.d("DoctorProfile", "✅ Profile image loaded: $currentProfileImageUrl")
                    }

                    Log.d("DoctorProfile", "✅ Profile loaded: ${profile.email}")
                } else {
                    val error = response.errorBody()?.string() ?: "Error loading profile"
                    Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("DoctorProfile", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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

                // Upload l'image si une nouvelle a été sélectionnée
                var imageUrl = currentProfileImageUrl  // ✅ FIX : Nom correct

                if (selectedImageBitmap != null) {
                    Toast.makeText(this@DoctorDashboardActivity, "📤 Uploading image...", Toast.LENGTH_SHORT).show()

                    imageUrl = ImageUploadHelper.uploadImage(selectedImageBitmap!!, "doctors")

                    if (imageUrl != null) {
                        Log.d("DoctorProfile", "✅ Image uploaded: $imageUrl")
                        Toast.makeText(this@DoctorDashboardActivity, "✅ Image uploaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("DoctorProfile", "❌ Image upload failed")
                        Toast.makeText(this@DoctorDashboardActivity, "⚠️ Image upload failed, continuing without image", Toast.LENGTH_SHORT).show()
                    }
                }

                val request = UpdateDoctorProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber.ifEmpty { null },
                    specialization = specialization,
                    hospitalAffiliation = hospital,
                    yearsOfExperience = yearsOfExperience,
                    officeAddress = officeAddress.ifEmpty { null },
                    consultationHours = consultationHours.ifEmpty { null },
                    profilePictureUrl = imageUrl
                )

                val response = RetrofitClient.getDoctorService(this@DoctorDashboardActivity)
                    .updateDoctorProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val updatedProfile = response.body()!!

                    tvDoctorName.text = updatedProfile.fullName
                    tvSpecialization.text = updatedProfile.specialization

                    // ✅ FIX : Nom correct
                    currentProfileImageUrl = updatedProfile.profilePictureUrl
                    selectedImageBitmap = null

                    Toast.makeText(
                        this@DoctorDashboardActivity,
                        "✅ Profile updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    toggleEditProfileVisibility()

                    Log.d("DoctorProfile", "✅ Profile updated: ${updatedProfile.email}")
                } else {
                    val error = response.errorBody()?.string() ?: "Update failed"
                    Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("DoctorProfile", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@DoctorDashboardActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnUpdateProfile.isEnabled = true
                btnUpdateProfile.text = "Enregistrer"
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

                    tvActivationStatus.text = if (isActivated) {
                        "✅ Activated"
                    } else {
                        "⏳ $message"
                    }

                    Toast.makeText(this@DoctorDashboardActivity, message, Toast.LENGTH_LONG).show()

                    Log.d("DoctorProfile", "✅ Activation status: $isActivated")
                } else {
                    val error = response.errorBody()?.string() ?: "Failed to check status"
                    Toast.makeText(this@DoctorDashboardActivity, "❌ $error", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("DoctorProfile", "❌ Exception: ${e.message}", e)
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
}