package com.health.virtualdoctor.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.ChangePasswordRequest
import com.health.virtualdoctor.ui.data.models.UpdateUserProfileRequest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Status bar
        window.statusBarColor = resources.getColor(R.color.primary, theme)

        tokenManager = TokenManager(this)

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
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                // ✅ Vérifier d'abord si le token existe
                val accessToken = tokenManager.getAccessToken()
                if (accessToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "❌ Session expirée. Reconnectez-vous",
                        Toast.LENGTH_LONG
                    ).show()
                    logout()
                    return@launch
                }

                val token = "Bearer $accessToken"
                Log.d("UserProfile", "🔑 Token: ${accessToken.take(20)}...")

                // Call USER SERVICE (port 8085)
                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .getUserProfile(token)

                Log.d("UserProfile", "📡 Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!

                    // Display profile info
                    tvUserName.text = profile.fullName ?: "Utilisateur"
                    tvUserEmail.text = profile.email ?: ""

                    // ✅ FIX PRINCIPAL : Gérer correctement les rôles (Set<String>)
                    val rolesText = try {
                        if (!profile.roles.isNullOrEmpty()) {
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
                    etFirstName.setText(profile.firstName ?: "")
                    etLastName.setText(profile.lastName ?: "")
                    etPhoneNumber.setText(profile.phoneNumber ?: "")

                    Log.d("UserProfile", "✅ Profile loaded: ${profile.email}")
                    Log.d("UserProfile", "📊 Roles: $rolesText")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserProfile", "❌ Error response: $errorBody")

                    if (response.code() == 401) {
                        Toast.makeText(
                            this@UserProfileActivity,
                            "❌ Session expirée. Reconnectez-vous",
                            Toast.LENGTH_LONG
                        ).show()
                        logout()
                    } else {
                        Toast.makeText(
                            this@UserProfileActivity,
                            "❌ Erreur ${response.code()}: ${errorBody ?: "Inconnu"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                Log.e("UserProfile", "❌ Network error: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Erreur réseau. Vérifiez votre connexion",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("UserProfile", "❌ Timeout: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Timeout. Le serveur ne répond pas",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: NullPointerException) {
                Log.e("UserProfile", "❌ Null pointer: ${e.message}", e)
                Toast.makeText(
                    this@UserProfileActivity,
                    "❌ Données manquantes. Veuillez vous reconnecter",
                    Toast.LENGTH_LONG
                ).show()
                logout()
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

                // Call USER SERVICE (port 8085)
                val response = RetrofitClient.getUserService(this@UserProfileActivity)
                    .updateUserProfile(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val updatedProfile = response.body()!!

                    tvUserName.text = updatedProfile.fullName ?: "Utilisateur"

                    // ✅ FIX : Gérer correctement les rôles lors de la mise à jour
                    val role = try {
                        updatedProfile.roles?.firstOrNull() ?: "USER"
                    } catch (e: Exception) {
                        "USER"
                    }

                    // Update TokenManager
                    tokenManager.saveUserInfo(
                        userId = updatedProfile.id ?: "",
                        email = updatedProfile.email ?: "",
                        name = updatedProfile.fullName ?: "",
                        role = role
                    )

                    Toast.makeText(
                        this@UserProfileActivity,
                        "✅ Profil mis à jour!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("UserProfile", "✅ Profile updated: ${updatedProfile.email}")
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

                // Call USER SERVICE (port 8085)
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
        // Clear tokens
        tokenManager.clearTokens()

        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "✅ Déconnexion réussie", Toast.LENGTH_SHORT).show()
    }
}