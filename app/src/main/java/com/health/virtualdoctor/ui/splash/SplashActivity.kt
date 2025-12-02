package com.health.virtualdoctor.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.welcome.WelcomeActivity  // ← CHANGEMENT ICI

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 3000 // 3 secondes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Rendre la barre de statut transparente
        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        // Navigation après le délai
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToWelcome()  // ← CHANGEMENT ICI
        }, splashTimeOut)
    }

    private fun navigateToWelcome() {  // ← CHANGEMENT ICI
        val intent = Intent(this, WelcomeActivity::class.java)  // ← CHANGEMENT ICI
        startActivity(intent)
        finish()

        // Animation de transition fluide
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
