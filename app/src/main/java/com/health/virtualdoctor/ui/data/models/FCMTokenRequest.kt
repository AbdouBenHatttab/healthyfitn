package com.health.virtualdoctor.ui.data.models

data class FCMTokenRequest(
    val fcmToken: String,
    val deviceType: String = "ANDROID",  // Valeur par défaut
    val deviceModel: String? = null      // Optionnel
)