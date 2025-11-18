package com.health.virtualdoctor.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.auth.RegisterActivity
import com.health.virtualdoctor.ui.user.UserMetricsActivity
import com.health.virtualdoctor.ui.doctor.DoctorDashboardActivity
import com.health.virtualdoctor.ui.admin.AdminDashboardActivity
import com.health.virtualdoctor.ui.utils.TokenManager

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnGetStarted: MaterialButton
    private lateinit var btnExploreDashboard: MaterialButton
    private lateinit var tvLoginLink: TextView
    private lateinit var loginSection: LinearLayout
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Rendre la barre de statut transparente
        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        // Initialiser TokenManager
        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
        updateUIBasedOnAuthState()
    }

    private fun initViews() {
        btnGetStarted = findViewById(R.id.btnGetStarted)
        btnExploreDashboard = findViewById(R.id.btnExploreDashboard)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        loginSection = findViewById(R.id.loginSection) // Le LinearLayout contenant "Already member?"
    }

    private fun setupListeners() {
        // Navigate to Register
        btnGetStarted.setOnClickListener {
            navigateToRegister()
        }

        // Navigate to Dashboard selon le rôle
        btnExploreDashboard.setOnClickListener {
            navigateToAppropriateScreen()
        }

        // Navigate to Login
        tvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    /**
     * 🎯 Navigation intelligente selon l'authentification et le rôle
     */
    private fun navigateToAppropriateScreen() {
        // Vérifier si l'utilisateur est connecté
        if (!tokenManager.isLoggedIn()) {
            // Pas de token → Aller à Login
            navigateToLogin()
            return
        }

        // L'utilisateur est connecté → Router selon le rôle
        val userRole = tokenManager.getUserRole()?.uppercase()

        when (userRole) {
            "USER" -> {
                // Utilisateur normal → UserMetricsActivity
                navigateToUserMetrics()
            }
            "DOCTOR" -> {
                // Médecin → DoctorDashboardActivity
                navigateToDoctorDashboard()
            }
            "ADMIN" -> {
                // Admin → AdminDashboardActivity
                navigateToAdminDashboard()
            }
            else -> {
                // Rôle inconnu ou null → Par défaut vers Login
                navigateToLogin()
            }
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToUserMetrics() {
        val intent = Intent(this, UserMetricsActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToDoctorDashboard() {
        val intent = Intent(this, DoctorDashboardActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToAdminDashboard() {
        val intent = Intent(this, AdminDashboardActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    /**
     * 🎨 Met à jour l'interface selon l'état de connexion
     */
    private fun updateUIBasedOnAuthState() {
        if (tokenManager.isLoggedIn()) {
            // Utilisateur connecté → Masquer la section login
            loginSection.visibility = android.view.View.GONE

            // Optionnel : Changer le texte du bouton "Get Started"
            btnGetStarted.text = "Mon Profil"

            // Optionnel : Mettre en avant le bouton Explore
            btnExploreDashboard.visibility = android.view.View.VISIBLE
        } else {
            // Utilisateur non connecté → Afficher la section login
            loginSection.visibility = android.view.View.VISIBLE
        }
    }
}