package com.health.virtualdoctor.ui.utils


import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import kotlinx.coroutines.launch

object LogoutHelper {

    fun logout(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        val tokenManager = TokenManager(context)
        val refreshToken = tokenManager.getRefreshToken()

        if (refreshToken != null) {
            lifecycleScope.launch {
                try {
                    // Call logout API
                    RetrofitClient.getApiService(context).logout(refreshToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Clear tokens locally
                tokenManager.clearTokens()

                // Navigate to login
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)

                Toast.makeText(context, "✅ Déconnexion réussie", Toast.LENGTH_SHORT).show()
            }
        } else {
            // No token, just navigate
            tokenManager.clearTokens()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
