package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.admin.AdminDashboardActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.LoginRequest
import com.health.virtualdoctor.ui.doctor.DoctorDashboardActivity
import com.health.virtualdoctor.ui.user.UserMetricsActivity
import com.health.virtualdoctor.ui.utils.FCMHelper
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: android.view.View
    private lateinit var tvForgotPassword: android.view.View // ✅ NOUVEAU

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)
        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)
        tvForgotPassword = findViewById(R.id.tvForgotPassword) // ✅ NOUVEAU
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        tvRegisterLink.setOnClickListener {
            navigateToRegister()
        }

        // ✅ NOUVEAU: Forgot Password
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun setupValidation() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilEmail.error = null
            }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilPassword.error = null
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "L'email est requis"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email invalide"
            isValid = false
        } else {
            tilEmail.error = null
        }

        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "Le mot de passe est requis"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Au moins 6 caractères"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        btnLogin.isEnabled = false
        btnLogin.text = "Connexion..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)

                val isDoctor = email.contains("@doctor.")

                Log.d("LoginActivity", "🔍 Email: $email | isDoctor: $isDoctor")

                val response = if (isDoctor) {
                    Log.d("LoginActivity", "🩺 Calling Doctor Login API")
                    RetrofitClient.getDoctorService(this@LoginActivity).loginDoctor(request)
                } else {
                    Log.d("LoginActivity", "👤 Calling User Login API")
                    RetrofitClient.getAuthService(this@LoginActivity).login(request)
                }

                Log.d("LoginActivity", "📡 Response Code: ${response.code()}")

                if (response.isSuccessful) {
                    if (isDoctor) {
                        handleDoctorLogin(response.body() as Map<String, Any>)
                    } else {
                        handleUserLogin(response.body() as? com.health.virtualdoctor.ui.data.models.AuthResponse)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Erreur inconnue"

                    try {
                        val errorJson = JSONObject(errorBody)
                        val errorMessage = when {
                            errorJson.has("message") -> errorJson.getString("message")
                            errorJson.has("error") -> errorJson.getString("error")
                            else -> "Erreur ${response.code()}"
                        }

                        Log.e("LoginActivity", "❌ Error: $errorMessage")

                        runOnUiThread {
                            when {
                                errorMessage.contains("pending", ignoreCase = true) -> {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "⏳ Votre compte est en attente d'activation par l'admin",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                errorMessage.contains("not activated", ignoreCase = true) -> {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "⏳ Compte en attente d'activation",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                else -> {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "❌ $errorMessage",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "❌ Raw Error: $errorBody")
                        Toast.makeText(
                            this@LoginActivity,
                            "❌ Erreur ${response.code()}: Vérifiez vos identifiants",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@LoginActivity,
                    "❌ Erreur de connexion: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = getString(R.string.login_button)
                }
            }
        }
    }

    private fun handleUserLogin(authResponse: com.health.virtualdoctor.ui.data.models.AuthResponse?) {
        if (authResponse == null) {
            Toast.makeText(this, "❌ Erreur de parsing", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("LoginActivity", "✅ User Login Success: ${authResponse.user.email}")

        tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

        val userId = authResponse.userId ?: authResponse.user.email
        val role = authResponse.user.roles.firstOrNull() ?: "USER"

        tokenManager.saveUserInfo(
            userId = userId,
            email = authResponse.user.email,
            name = authResponse.user.fullName,
            role = role
        )

        FCMHelper.saveFCMToken(this)

        Toast.makeText(this, "✅ Connexion réussie!", Toast.LENGTH_SHORT).show()
        navigateByRole(role)
    }

    private fun handleDoctorLogin(response: Map<String, Any>) {
        Log.d("LoginActivity", "✅ Doctor Login Success")

        val accessToken = response["accessToken"] as? String ?: ""
        val refreshToken = response["refreshToken"] as? String ?: ""
        val userId = response["userId"] as? String ?: ""
        val email = response["email"] as? String ?: ""
        val fullName = response["fullName"] as? String ?: ""

        if (accessToken.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "❌ Données manquantes", Toast.LENGTH_SHORT).show()
            return
        }

        tokenManager.saveTokens(accessToken, refreshToken)
        tokenManager.saveUserInfo(
            userId = userId.ifEmpty { email },
            email = email,
            name = fullName,
            role = "DOCTOR"
        )

        FCMHelper.saveFCMToken(this)

        Toast.makeText(this, "✅ Bienvenue Dr. $fullName!", Toast.LENGTH_SHORT).show()
        navigateByRole("DOCTOR")
    }


    private fun navigateByRole(role: String) {
        val intent = when (role) {
            "USER" -> Intent(this, UserMetricsActivity::class.java)
            "DOCTOR" -> Intent(this, DoctorDashboardActivity::class.java)
            "ADMIN" -> Intent(this, AdminDashboardActivity::class.java) // ✅ NOUVEAU
            else -> Intent(this, com.health.virtualdoctor.ui.welcome.WelcomeActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // ✅ NOUVEAU: Dialog "Mot de passe oublié"
    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etEmailForgot = dialogView.findViewById<EditText>(R.id.etEmailForgot)
        val rgUserTypeForgot = dialogView.findViewById<RadioGroup>(R.id.rgUserTypeForgot)

        MaterialAlertDialogBuilder(this)
            .setTitle("Mot de passe oublié")
            .setMessage("Entrez votre email pour réinitialiser votre mot de passe")
            .setView(dialogView)
            .setPositiveButton("Envoyer") { _, _ ->
                val email = etEmailForgot.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(this, "⚠️ L'email est requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "⚠️ Email invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // ✅ Détecter le type selon le RadioButton sélectionné
                val selectedId = rgUserTypeForgot.checkedRadioButtonId
                val isDoctor = (selectedId == R.id.rbDoctorForgot)

                Log.d("LoginActivity", "🔐 Forgot password for: $email (isDoctor: $isDoctor)")

                performForgotPassword(email, isDoctor)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ✅ NOUVEAU: Envoyer demande de réinitialisation
    private fun performForgotPassword(email: String, isDoctor: Boolean) {
        lifecycleScope.launch {
            try {
                val request = mapOf("email" to email)

                Log.d("LoginActivity", "📤 Sending forgot password request...")
                Log.d("LoginActivity", "   Email: $email")
                Log.d("LoginActivity", "   Type: ${if (isDoctor) "DOCTOR" else "USER"}")

                val response = if (isDoctor) {
                    // Call DOCTOR SERVICE (port 8083)
                    RetrofitClient.getDoctorService(this@LoginActivity)
                        .forgotDoctorPassword(request)
                } else {
                    // Call USER SERVICE (port 8085)
                    RetrofitClient.getUserService(this@LoginActivity)
                        .forgotUserPassword(request)
                }

                if (response.isSuccessful) {
                    Log.d("LoginActivity", "✅ Forgot password email sent")

                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Un email de réinitialisation a été envoyé à $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LoginActivity", "❌ Forgot password error: $errorBody")

                    Toast.makeText(
                        this@LoginActivity,
                        "❌ Erreur ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@LoginActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}