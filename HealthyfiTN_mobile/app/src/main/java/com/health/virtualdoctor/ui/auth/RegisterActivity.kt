package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.DoctorRegisterRequest
import com.health.virtualdoctor.ui.data.models.RegisterRequest
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    // Basic Fields
    private lateinit var tilFirstName: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var tilLastName: TextInputLayout
    private lateinit var etLastName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText

    // ✅ NOUVEAU: Contact Email pour doctors
    private lateinit var tilContactEmail: TextInputLayout
    private lateinit var etContactEmail: TextInputEditText

    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText

    // User Type
    private lateinit var rgUserType: RadioGroup
    private lateinit var rbPatient: RadioButton
    private lateinit var rbDoctor: RadioButton

    // Doctor-Specific Fields Container
    private lateinit var llDoctorFields: LinearLayout

    // Doctor-Specific Fields
    private lateinit var tilSpecialization: TextInputLayout
    private lateinit var etSpecialization: TextInputEditText
    private lateinit var tilLicenseNumber: TextInputLayout
    private lateinit var etLicenseNumber: TextInputEditText
    private lateinit var tilYearsOfExperience: TextInputLayout
    private lateinit var etYearsOfExperience: TextInputEditText
    private lateinit var tilEducation: TextInputLayout
    private lateinit var etEducation: TextInputEditText
    private lateinit var tilClinicName: TextInputLayout
    private lateinit var etClinicName: TextInputEditText
    private lateinit var tilClinicAddress: TextInputLayout
    private lateinit var etClinicAddress: TextInputEditText

    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLoginLink: TextView

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        // Basic fields
        tilFirstName = findViewById(R.id.tilFirstName)
        etFirstName = findViewById(R.id.etFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        etLastName = findViewById(R.id.etLastName)
        tilEmail = findViewById(R.id.tilEmailRegister)
        etEmail = findViewById(R.id.etEmailRegister)

        // ✅ NOUVEAU: Contact email
        tilContactEmail = findViewById(R.id.tilContactEmail)
        etContactEmail = findViewById(R.id.etContactEmail)

        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        tilPassword = findViewById(R.id.tilPasswordRegister)
        etPassword = findViewById(R.id.etPasswordRegister)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        // User type
        rgUserType = findViewById(R.id.rgUserType)
        rbPatient = findViewById(R.id.rbPatient)
        rbDoctor = findViewById(R.id.rbDoctor)

        // Doctor fields container
        llDoctorFields = findViewById(R.id.llDoctorFields)

        // Doctor-specific fields
        tilSpecialization = findViewById(R.id.tilSpecialization)
        etSpecialization = findViewById(R.id.etSpecialization)
        tilLicenseNumber = findViewById(R.id.tilLicenseNumber)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        tilYearsOfExperience = findViewById(R.id.tilYearsOfExperience)
        etYearsOfExperience = findViewById(R.id.etYearsOfExperience)
        tilEducation = findViewById(R.id.tilEducation)
        etEducation = findViewById(R.id.etEducation)
        tilClinicName = findViewById(R.id.tilClinicName)
        etClinicName = findViewById(R.id.etClinicName)
        tilClinicAddress = findViewById(R.id.tilClinicAddress)
        etClinicAddress = findViewById(R.id.etClinicAddress)

        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }

    private fun setupListeners() {
        // User type selection
        rgUserType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPatient -> {
                    showDoctorFields(false)
                    // ✅ Cacher le contact email pour les patients
                    tilContactEmail.visibility = View.GONE
                    // ✅ Email normal pour patients
                    tilEmail.hint = "Email"
                }
                R.id.rbDoctor -> {
                    showDoctorFields(true)
                    // ✅ Afficher le contact email pour les doctors
                    tilContactEmail.visibility = View.VISIBLE
                    // ✅ Préciser que c'est l'email système
                    tilEmail.hint = "Email système (@doctor.com)"
                }
            }
        }

        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }

        tvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun showDoctorFields(show: Boolean) {
        llDoctorFields.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupValidation() {
        // Basic fields
        etFirstName.addTextChangedListener(createTextWatcher(tilFirstName))
        etLastName.addTextChangedListener(createTextWatcher(tilLastName))
        etEmail.addTextChangedListener(createTextWatcher(tilEmail))
        etContactEmail.addTextChangedListener(createTextWatcher(tilContactEmail))
        etPhone.addTextChangedListener(createTextWatcher(tilPhone))
        etPassword.addTextChangedListener(createTextWatcher(tilPassword))
        etConfirmPassword.addTextChangedListener(createTextWatcher(tilConfirmPassword))

        // Doctor fields
        etSpecialization.addTextChangedListener(createTextWatcher(tilSpecialization))
        etLicenseNumber.addTextChangedListener(createTextWatcher(tilLicenseNumber))
        etYearsOfExperience.addTextChangedListener(createTextWatcher(tilYearsOfExperience))
        etEducation.addTextChangedListener(createTextWatcher(tilEducation))
        etClinicName.addTextChangedListener(createTextWatcher(tilClinicName))
        etClinicAddress.addTextChangedListener(createTextWatcher(tilClinicAddress))
    }

    private fun createTextWatcher(textInputLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.error = null
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Basic validation
        val firstName = etFirstName.text.toString().trim()
        if (firstName.isEmpty()) {
            tilFirstName.error = "Le prénom est requis"
            isValid = false
        }

        val lastName = etLastName.text.toString().trim()
        if (lastName.isEmpty()) {
            tilLastName.error = "Le nom est requis"
            isValid = false
        }

        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "L'email est requis"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email invalide"
            isValid = false
        }

        // ✅ NOUVEAU: Valider contactEmail pour doctors
        if (rbDoctor.isChecked) {
            val contactEmail = etContactEmail.text.toString().trim()
            if (contactEmail.isEmpty()) {
                tilContactEmail.error = "L'email de contact est requis"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
                tilContactEmail.error = "Email invalide"
                isValid = false
            }

            // ✅ Vérifier que l'email système se termine par @doctor
            if (!email.contains("@doctor.")) {
                tilEmail.error = "L'email doit se terminer par @doctor (ex: dr.smith@doctor.com)"
                isValid = false
            }
        }

        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Le téléphone est requis"
            isValid = false
        } else if (phone.length < 8) {
            tilPhone.error = "Numéro invalide"
            isValid = false
        }

        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "Le mot de passe est requis"
            isValid = false
        } else if (password.length < 8) {
            tilPassword.error = "Au moins 8 caractères"
            isValid = false
        }

        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirmation requise"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Les mots de passe ne correspondent pas"
            isValid = false
        }

        // Doctor-specific validation
        if (rbDoctor.isChecked) {
            val specialization = etSpecialization.text.toString().trim()
            if (specialization.isEmpty()) {
                tilSpecialization.error = "La spécialisation est requise"
                isValid = false
            }

            val licenseNumber = etLicenseNumber.text.toString().trim()
            if (licenseNumber.isEmpty()) {
                tilLicenseNumber.error = "Le numéro de licence est requis"
                isValid = false
            }

            val yearsOfExperience = etYearsOfExperience.text.toString().trim()
            if (yearsOfExperience.isEmpty()) {
                tilYearsOfExperience.error = "L'expérience est requise"
                isValid = false
            }

            val education = etEducation.text.toString().trim()
            if (education.isEmpty()) {
                tilEducation.error = "La formation est requise"
                isValid = false
            }

            val clinicName = etClinicName.text.toString().trim()
            if (clinicName.isEmpty()) {
                tilClinicName.error = "Le nom de la clinique est requis"
                isValid = false
            }

            val clinicAddress = etClinicAddress.text.toString().trim()
            if (clinicAddress.isEmpty()) {
                tilClinicAddress.error = "L'adresse est requise"
                isValid = false
            }
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Veuillez accepter les conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun performRegister() {
        btnRegister.isEnabled = false
        btnRegister.text = "Inscription..."

        lifecycleScope.launch {
            try {
                if (rbDoctor.isChecked) {
                    registerAsDoctor()
                } else {
                    registerAsUser()
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "❌ Exception: ${e.message}", e)
                Toast.makeText(
                    this@RegisterActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnRegister.isEnabled = true
                btnRegister.text = getString(R.string.register_button)
            }
        }
    }

    // ✅ Register Doctor avec contactEmail
    private suspend fun registerAsDoctor() {
        val request = DoctorRegisterRequest(
            email = etEmail.text.toString().trim(),
            contactEmail = etContactEmail.text.toString().trim(), // ✅ NOUVEAU
            password = etPassword.text.toString(),
            firstName = etFirstName.text.toString().trim(),
            lastName = etLastName.text.toString().trim(),
            birthDate = null,
            gender = null,
            phoneNumber = etPhone.text.toString().trim(),
            medicalLicenseNumber = etLicenseNumber.text.toString().trim(),
            specialization = etSpecialization.text.toString().trim(),
            hospitalAffiliation = etClinicName.text.toString().trim(),
            yearsOfExperience = etYearsOfExperience.text.toString().trim().toIntOrNull() ?: 0,
            officeAddress = etClinicAddress.text.toString().trim().ifEmpty { null },
            consultationHours = null
        )

        Log.d("RegisterActivity", "🩺 Registering doctor")
        Log.d("RegisterActivity", "   System email: ${request.email}")
        Log.d("RegisterActivity", "   Contact email: ${request.contactEmail}")

        val response = RetrofitClient.getDoctorService(this@RegisterActivity)
            .registerDoctor(request)

        if (response.isSuccessful && response.body() != null) {
            val doctorResponse = response.body()!!

            Log.d("RegisterActivity", "✅ Doctor registered: ${doctorResponse.email}")

            Toast.makeText(
                this@RegisterActivity,
                "✅ Inscription réussie!\n📧 Vérifiez votre email: ${request.contactEmail}\n⏳ En attente d'activation",
                Toast.LENGTH_LONG
            ).show()

            navigateToLogin()
        } else {
            val errorBody = response.errorBody()?.string() ?: response.message()
            Log.e("RegisterActivity", "❌ Doctor registration error: $errorBody")
            Toast.makeText(
                this@RegisterActivity,
                "❌ Erreur: $errorBody",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Register User (inchangé)
    private suspend fun registerAsUser() {
        val request = RegisterRequest(
            email = etEmail.text.toString().trim(),
            password = etPassword.text.toString(),
            firstName = etFirstName.text.toString().trim(),
            lastName = etLastName.text.toString().trim(),
            birthDate = null,
            gender = null,
            phoneNumber = etPhone.text.toString().trim(),
            role = "USER"
        )

        Log.d("RegisterActivity", "👤 Registering user: ${request.email}")

        val response = RetrofitClient.getAuthService(this@RegisterActivity)
            .register(request)

        if (response.isSuccessful && response.body() != null) {
            val authResponse = response.body()!!

            Log.d("RegisterActivity", "✅ User registered: ${authResponse.user.email}")

            tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

            val userId = authResponse.userId ?: authResponse.user.email
            tokenManager.saveUserInfo(
                userId = userId,
                email = authResponse.user.email,
                name = authResponse.user.fullName,
                role = "USER"
            )

            Toast.makeText(
                this@RegisterActivity,
                "✅ Inscription réussie!",
                Toast.LENGTH_SHORT
            ).show()

            navigateByRole("USER")
        } else {
            val errorBody = response.errorBody()?.string() ?: response.message()
            Log.e("RegisterActivity", "❌ User registration error: $errorBody")
            Toast.makeText(
                this@RegisterActivity,
                "❌ Erreur: $errorBody",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun navigateByRole(role: String) {
        when (role) {
            "USER" -> {
                Toast.makeText(this, "🏠 Bienvenue User!", Toast.LENGTH_SHORT).show()
            }
            "DOCTOR" -> {
                Toast.makeText(this, "👨‍⚕️ Bienvenue Docteur!", Toast.LENGTH_SHORT).show()
            }
            "ADMIN" -> {
                Toast.makeText(this, "⚙️ Bienvenue Admin!", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}