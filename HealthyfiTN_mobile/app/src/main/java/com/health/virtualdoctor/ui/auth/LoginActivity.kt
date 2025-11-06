package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.doctor.DoctorDashboardActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var rgUserType: RadioGroup
    private lateinit var rbPatient: RadioButton
    private lateinit var rbDoctor: RadioButton
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: View
    private lateinit var tvRegisterLink: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        rgUserType = findViewById(R.id.rgUserTypeLogin)
        rbPatient = findViewById(R.id.rbPatientLogin)
        rbDoctor = findViewById(R.id.rbDoctorLogin)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
        }

        tvRegisterLink.setOnClickListener {
            navigateToRegister()
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
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val isDoctor = rbDoctor.isChecked

        btnLogin.isEnabled = false
        btnLogin.text = "Connexion..."

        // Simuler une connexion API
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            btnLogin.isEnabled = true
            btnLogin.text = getString(R.string.login_button)

            Toast.makeText(this, "Connexion réussie!", Toast.LENGTH_SHORT).show()

            // Navigation selon le type d'utilisateur
            if (isDoctor) {
                navigateToDoctorDashboard()
            } else {
                navigateToPatientHome()
            }
        }, 1500)
    }

    private fun navigateToDoctorDashboard() {
        val intent = Intent(this, DoctorDashboardActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToPatientHome() {
        // TODO: Créer PatientHomeActivity
        Toast.makeText(this, "Navigation vers Patient Home", Toast.LENGTH_SHORT).show()
        // val intent = Intent(this, PatientHomeActivity::class.java)
        // startActivity(intent)
        // finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}