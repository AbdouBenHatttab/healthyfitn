package com.health.virtualdoctor.ui.utils

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.FCMTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FCMHelper {

    private const val TAG = "FCMHelper"

    /**
     * Récupère le FCM token et l'envoie au backend via le Notification Service (Cloudflare)
     */
    fun saveFCMToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "❌ Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "✅ FCM Token obtained: $token")

            // Envoyer le token au backend
            sendTokenToBackend(context, token)
        }
    }

    /**
     * Envoie le FCM token au Notification Service via Cloudflare
     */
    private fun sendTokenToBackend(context: Context, fcmToken: String) {
        val tokenManager = TokenManager(context)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No access token available, skipping FCM token registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = FCMTokenRequest(fcmToken)

                // 🆕 Utiliser le Notification Service (Cloudflare port 8084)
                val response = RetrofitClient.getNotificationService(context)
                    .saveFcmToken("Bearer $accessToken", request)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ FCM token saved successfully to Cloudflare Notification Service")
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "❌ Failed to save FCM token: ${response.code()} - $error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while saving FCM token: ${e.message}", e)
            }
        }
    }

    /**
     * Rafraîchir le token FCM (appelé quand le token change)
     */
    fun refreshFCMToken(context: Context, newToken: String) {
        Log.d(TAG, "🔄 FCM Token refreshed: $newToken")
        sendTokenToBackend(context, newToken)
    }
}