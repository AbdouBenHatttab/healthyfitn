package com.health.virtualdoctor.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.UserMetricsActivity

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔑 Nouveau token FCM : $token")

        // ✅ Envoyer le token au backend
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "📩 Message reçu de : ${message.from}")

        // Vérifier si le message contient une notification
        message.notification?.let {
            Log.d("FCM", "📬 Titre de la notification : ${it.title}")
            Log.d("FCM", "📬 Contenu de la notification : ${it.body}")

            showNotification(it.title ?: "", it.body ?: "")
        }

        // Vérifier si le message contient des données
        message.data.isNotEmpty().let {
            Log.d("FCM", "📦 Données : ${message.data}")

            val type = message.data["type"]
            val action = message.data["action"]

            handleDataPayload(type, action, message.data)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, UserMetricsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "health_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // ✅ Créez cette icône
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Créer le canal de notification pour Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications Santé",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleDataPayload(type: String?, action: String?, data: Map<String, String>) {
        when (type) {
            "DOCTOR_REGISTRATION" -> {
                Log.d("FCM", "🩺 Nouvelle inscription de médecin")
            }
            "DOCTOR_APPROVED" -> {
                Log.d("FCM", "✅ Médecin approuvé")
            }
            else -> {
                Log.d("FCM", "📦 Type de notification inconnu : $type")
            }
        }
    }

    private fun sendTokenToBackend(token: String) {
        // ✅ TODO : Envoyer le token FCM à votre backend
        Log.d("FCM", "📤 TODO : Envoyer le token au backend : $token")

        // Exemple :
        // lifecycleScope.launch {
        //     val request = FCMTokenRequest(token, "ANDROID", Build.MODEL)
        //     RetrofitClient.getNotificationService(this).saveFcmToken(request)
        // }
    }
}
