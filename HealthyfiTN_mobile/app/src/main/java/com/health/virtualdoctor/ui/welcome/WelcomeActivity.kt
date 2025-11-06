package com.health.virtualdoctor.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.auth.RegisterActivity
import com.health.virtualdoctor.ui.user.UserMetricsActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnGetStarted: MaterialButton
    private lateinit var btnExploreDashboard: MaterialButton
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Rendre la barre de statut transparente
        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnGetStarted = findViewById(R.id.btnGetStarted)
        btnExploreDashboard = findViewById(R.id.btnExploreDashboard)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }

    private fun setupListeners() {
        // Navigate to Register
        btnGetStarted.setOnClickListener {
            navigateToRegister()
        }

        // Navigate to Dashboard (pour plus tard)
        btnExploreDashboard.setOnClickListener {
            // TODO: Naviguer vers le Dashboard
            // Pour le moment, aller à UserMetrics
            navigateToUserMetrics()
        }

        // Navigate to Login
        tvLoginLink.setOnClickListener {
            navigateToLogin()
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
}