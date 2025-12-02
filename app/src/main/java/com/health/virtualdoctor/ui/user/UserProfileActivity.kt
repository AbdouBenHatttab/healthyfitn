package com.health.virtualdoctor.ui.user

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.ChangePasswordRequest
import com.health.virtualdoctor.ui.data.models.UpdateUserProfileRequest
import com.health.virtualdoctor.ui.utils.ImageUploadHelper
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnUpdateProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var ivProfileImage: ImageView
    private lateinit var profileImageCard: com.google.android.material.card.MaterialCardView
    private lateinit var progressBar: ProgressBar

    // Image picker launcher
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Status bar
        window.statusBarColor = resources.getColor(R.color.primary, theme)

        tokenManager = TokenManager(this)

        // Initialize image picker launcher BEFORE initViews
        setupImagePickerLauncher()

        initViews()
        setupListeners()
        loadUserProfile()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        profileImageCard = findViewById(R.id.profileImageCard)

        // Add progress bar to layout (or find existing one)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnUpdateProfile.setOnClickListener {
            updateUserProfile()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Make profile image clickable
        profileImageCard.setOnClickListener {
            openImagePicker()
        }

        ivProfileImage.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedImage(uri)
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            selectedImageBitmap = bitmap

            // Display selected image immediately
            ivProfileImage.setImageBitmap(bitmap)

            // Upload image to Cloudinary
            uploadProfileImage(bitmap)

        } catch (e: Exception) {
            Log.e("UserProfile", "Error loading image: ${e.message}", e)
            Toast.makeText(
                this,
                "❌ Erreur lors du chargement de l'image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun uploadProfileImage(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                btnUpdateProfile.isEnabled = false

                Toast.makeText(
                    this@UserProfileActivity,
                    "📤 Upload de l'image en cours...",
                    Toast.LENGTH_SHORT
                ).show()

                val imageUrl = ImageUploadHelper.uploadImageWithProgress(
                    bitmap = bitmap,
                    folder = "user_profiles"
                ) { progress ->
                    runOnUiThread {
                        Log.d("UserProfile", "Upload progress: $progress%")
                    }
                }

                progressBar.visibility = View.GONE
                btnUpdateProfile.isEnabled = true

                if (imageUrl != null) {
                    Log.d("UserProfile", "✅ Image uploaded: $imageUrl")

                    // Update profile with new image URL
                    updateProfileImageUrl(imageUrl)

                    Toast.makeText(
                        this@UserProfileActivity,
                        "✅ Photo de profil mise à jour!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Échec de l'upload",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnUpdateProfile.isEnabled = true

                Log.e("UserProfile", "Upload error: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateProfileImageUrl(imageUrl: String) {
        lifecycleScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    logout()
                    return@launch
                }

                val token = "Bearer $accessToken"
                val request = UpdateUserProfileRequest(
                    firstName = null,
                    lastName = null,
                    phoneNumber = null,
                    email = null,
                    profilePictureUrl = imageUrl
                )

                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .updateUserProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val updatedProfile = apiResponse.data

                    if (updatedProfile != null) {
                        Log.d("UserProfile", "✅ Profile image URL updated on server")
                    } else {
                        Log.e("UserProfile", "❌ Updated profile data is null")
                    }
                } else {
                    Log.e("UserProfile", "❌ Failed to update image URL: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("UserProfile", "❌ Error updating image URL: ${e.message}", e)
            }
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    Log.e("UserProfile", "❌ No access token found")
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Session expirée. Reconnectez-vous",
                        Toast.LENGTH_LONG
                    ).show()
                    logout()
                    return@launch
                }

                val token = "Bearer $accessToken"
                Log.d("UserProfile", "🔑 Token: ${accessToken.take(50)}...")
                Log.d("UserProfile", "📍 Calling: ${RetrofitClient.getUserBaseUrl()}/api/v1/users/profile")

                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .getUserProfile(token)

                Log.d("UserProfile", "📡 Response code: ${response.code()}")
                Log.d("UserProfile", "📡 Response message: ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("UserProfile", "📦 Response body: $responseBody")

                    if (responseBody == null) {
                        Log.e("UserProfile", "❌ Response body is null")
                        Toast.makeText(
                            this@UserProfileActivity,
                            "❌ Réponse vide du serveur",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // Check if it's wrapped in ApiResponse
                    try {
                        // Try to access as ApiResponse wrapper first
                        val apiResponse = responseBody as? com.health.virtualdoctor.ui.data.models.ApiResponse<*>

                        if (apiResponse != null) {
                            Log.d("UserProfile", "✅ Response is wrapped in ApiResponse")
                            Log.d("UserProfile", "   Success: ${apiResponse.success}")
                            Log.d("UserProfile", "   Message: ${apiResponse.message}")
                            Log.d("UserProfile", "   Data: ${apiResponse.data}")

                            val profile = apiResponse.data as? com.health.virtualdoctor.ui.data.models.UserProfileResponse

                            if (profile == null) {
                                Log.e("UserProfile", "❌ Profile data is null in wrapped response")
                                Toast.makeText(
                                    this@UserProfileActivity,
                                    "❌ Données de profil manquantes",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            displayProfile(profile)
                        } else {
                            // Try direct UserProfileResponse
                            Log.d("UserProfile", "ℹ️ Response is direct UserProfileResponse")
                            val profile = responseBody as? com.health.virtualdoctor.ui.data.models.UserProfileResponse

                            if (profile == null) {
                                Log.e("UserProfile", "❌ Cannot cast to UserProfileResponse")
                                Toast.makeText(
                                    this@UserProfileActivity,
                                    "❌ Format de réponse incorrect",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            displayProfile(profile)
                        }
                    } catch (e: ClassCastException) {
                        Log.e("UserProfile", "❌ ClassCastException: ${e.message}", e)
                        Toast.makeText(
                            this@UserProfileActivity,
                            "❌ Erreur de format: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserProfile", "❌ Error response code: ${response.code()}")
                    Log.e("UserProfile", "❌ Error message: ${response.message()}")
                    Log.e("UserProfile", "❌ Error body: $errorBody")

                    when (response.code()) {
                        401 -> {
                            Toast.makeText(
                                this@UserProfileActivity,
                                "❌ Session expirée. Reconnectez-vous",
                                Toast.LENGTH_LONG
                            ).show()
                            logout()
                        }
                        404 -> {
                            Toast.makeText(
                                this@UserProfileActivity,
                                "❌ Endpoint introuvable. Vérifiez l'URL",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        500 -> {
                            Toast.makeText(
                                this@UserProfileActivity,
                                "❌ Erreur serveur (500)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        503 -> {
                            Toast.makeText(
                                this@UserProfileActivity,
                                "❌ Service non disponible (503)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this@UserProfileActivity,
                                "❌ Erreur ${response.code()}: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                Log.e("UserProfile", "❌ UnknownHostException: ${e.message}", e)
                Log.e("UserProfile", "   URL tentée: ${RetrofitClient.getUserBaseUrl()}")
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Impossible de joindre le serveur. Vérifiez:\n1. Votre connexion Internet\n2. L'URL du serveur\n3. Cloudflare Tunnel actif",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("UserProfile", "❌ SocketTimeoutException: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Timeout. Le serveur ne répond pas",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: java.net.ConnectException) {
                Log.e("UserProfile", "❌ ConnectException: ${e.message}", e)
                Log.e("UserProfile", "   URL tentée: ${RetrofitClient.getUserBaseUrl()}")
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Connexion refusée. Vérifiez que le serveur est démarré",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: com.google.gson.JsonSyntaxException) {
                Log.e("UserProfile", "❌ JsonSyntaxException: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur de parsing JSON: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: NullPointerException) {
                Log.e("UserProfile", "❌ NullPointerException: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Données manquantes. Reconnectez-vous",
                    Toast.LENGTH_LONG
                ).show()
                logout()
            } catch (e: Exception) {
                Log.e("UserProfile", "❌ Unexpected Exception: ${e.javaClass.simpleName}", e)
                Log.e("UserProfile", "   Message: ${e.message}")
                e.printStackTrace()
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur inattendue: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayProfile(profile: com.health.virtualdoctor.ui.data.models.UserProfileResponse) {
        Log.d("UserProfile", "✅ Profile data received: ${profile.email}")
        Log.d("UserProfile", "   ID: ${profile.id}")
        Log.d("UserProfile", "   FirstName: ${profile.firstName}")
        Log.d("UserProfile", "   LastName: ${profile.lastName}")
        Log.d("UserProfile", "   FullName: ${profile.fullName}")
        Log.d("UserProfile", "   Phone: ${profile.phoneNumber}")
        Log.d("UserProfile", "   ProfilePic: ${profile.profilePictureUrl}")
        Log.d("UserProfile", "   Roles: ${profile.roles}")

        // Display profile info
        tvUserName.text = profile.fullName.ifEmpty { "Utilisateur" }
        tvUserEmail.text = profile.email

        // Handle roles correctly (Set<String>)
        val rolesText = try {
            if (profile.roles.isNotEmpty()) {
                profile.roles.joinToString(", ")
            } else {
                "USER"
            }
        } catch (e: Exception) {
            Log.e("UserProfile", "Error parsing roles: ${e.message}")
            "USER"
        }
        tvUserRole.text = "👤 $rolesText"

        // Pre-fill edit fields
        etFirstName.setText(profile.firstName)
        etLastName.setText(profile.lastName)
        etPhoneNumber.setText(profile.phoneNumber ?: "")

        // Load profile image if exists
        if (!profile.profilePictureUrl.isNullOrEmpty()) {
            loadProfileImage(profile.profilePictureUrl)
        } else {
            Log.d("UserProfile", "ℹ️ No profile picture URL")
        }

        Log.d("UserProfile", "✅ Profile displayed successfully")
    }

    private fun loadProfileImage(imageUrl: String) {
        try {
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(ivProfileImage)

            Log.d("UserProfile", "✅ Profile image loaded: $imageUrl")
        } catch (e: Exception) {
            Log.e("UserProfile", "❌ Error loading profile image: ${e.message}", e)
        }
    }

    private fun updateUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "⚠️ Prénom et nom requis", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                btnUpdateProfile.isEnabled = false
                btnUpdateProfile.text = "Mise à jour..."

                val accessToken = tokenManager.getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Session expirée",
                        Toast.LENGTH_SHORT
                    ).show()
                    logout()
                    return@launch
                }

                val token = "Bearer $accessToken"
                val request = UpdateUserProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber.ifEmpty { null },
                    email = null,
                    profilePictureUrl = null
                )

                Log.d("UserProfile", "📤 Updating profile: $request")

                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .updateUserProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val updatedProfile = apiResponse.data

                    if (updatedProfile == null) {
                        Log.e("UserProfile", "❌ Updated profile data is null")
                        Toast.makeText(
                            this@UserProfileActivity,
                            "❌ Erreur lors de la mise à jour",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // Update UI immediately
                    tvUserName.text = updatedProfile.fullName ?: "Utilisateur"

                    // Handle roles correctly
                    val role = try {
                        updatedProfile.roles.firstOrNull() ?: "USER"
                    } catch (e: Exception) {
                        "USER"
                    }

                    // Update TokenManager with new data
                    tokenManager.saveUserInfo(
                        userId = updatedProfile.id,
                        email = updatedProfile.email,
                        name = updatedProfile.fullName,
                        role = role
                    )

                    Toast.makeText(
                        this@UserProfileActivity,
                        "✅ Profil mis à jour!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("UserProfile", "✅ Profile updated: ${updatedProfile.email}")

                    // Reload profile to ensure UI is in sync
                    loadUserProfile()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserProfile", "❌ Update error: $errorBody")
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("UserProfile", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnUpdateProfile.isEnabled = true
                btnUpdateProfile.text = "Mettre à jour"
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
                    Toast.makeText(this, "⚠️ Mots de passe non identiques", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 8) {
                    Toast.makeText(this, "⚠️ Minimum 8 caractères", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Session expirée",
                        Toast.LENGTH_SHORT
                    ).show()
                    logout()
                    return@launch
                }

                val token = "Bearer $accessToken"
                val request = ChangePasswordRequest(currentPassword, newPassword)

                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .changePassword(token, request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "✅ Mot de passe changé!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("UserProfile", "✅ Password changed")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserProfile", "❌ Password change error: $errorBody")
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("UserProfile", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter?")
            .setPositiveButton("Oui") { _, _ ->
                logout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun logout() {
        tokenManager.clearTokens()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "✅ Déconnexion réussie", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }
}