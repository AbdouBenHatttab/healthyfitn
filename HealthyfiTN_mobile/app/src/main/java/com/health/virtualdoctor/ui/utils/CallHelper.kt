//package com.health.virtualdoctor.utils
//
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import com.health.virtualdoctor.ui.consultation.VideoCallActivity
//import com.health.virtualdoctor.ui.data.api.RetrofitClient
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONObject
//
//object CallHelper {
//    private const val TAG = "CallHelper"
//
//    /**
//     * Démarre un appel vidéo
//     *
//     * @param context Context Android
//     * @param appointmentId ID du rendez-vous
//     * @param userId Email/ID de l'utilisateur actuel
//     * @param userRole "DOCTOR" ou "USER"
//     * @param signalingServerUrl URL du serveur ngrok (wss://xxx.ngrok-free.app)
//     */
//    suspend fun startVideoCall(
//        context: Context,
//        appointmentId: String,
//        userId: String,
//        userRole: String,
//        signalingServerUrl: String
//    ): Result<String> = withContext(Dispatchers.IO) {
//        try {
//            Log.d(TAG, "═══════════════════════════════════════")
//            Log.d(TAG, "📞 STARTING VIDEO CALL")
//            Log.d(TAG, "   Appointment: $appointmentId")
//            Log.d(TAG, "   User: $userId")
//            Log.d(TAG, "   Role: $userRole")
//            Log.d(TAG, "═══════════════════════════════════════")
//
//            // 1. Initier l'appel via l'API
//            val apiService = RetrofitClient.getDoctorService(context)
//            val response = apiService.initiateCall(
//                mapOf(
//                    "appointmentId" to appointmentId,
//                    "callType" to "VIDEO"
//                )
//            )
//
//            if (!response.isSuccessful) {
//                val error = "API Error: ${response.code()} - ${response.errorBody()?.string()}"
//                Log.e(TAG, "❌ $error")
//                return@withContext Result.failure(Exception(error))
//            }
//
//            val body = response.body()
//            val callId = body?.callId ?: throw Exception("No callId in response")
//
//            Log.d(TAG, "✅ Call initiated, ID: $callId")
//
//            // 2. Lancer VideoCallActivity
//            withContext(Dispatchers.Main) {
//                val intent = Intent(context, VideoCallActivity::class.java).apply {
//                    putExtra("CALL_ID", callId)
//                    putExtra("USER_ID", userId)
//                    putExtra("USER_ROLE", userRole)
//                    putExtra("IS_INITIATOR", true)
//                    putExtra("SIGNALING_URL", signalingServerUrl)
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                context.startActivity(intent)
//            }
//
//            Result.success(callId)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error starting call", e)
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Rejoindre un appel existant (quand on reçoit une notification)
//     */
//    fun joinVideoCall(
//        context: Context,
//        callId: String,
//        userId: String,
//        userRole: String,
//        signalingServerUrl: String
//    ) {
//        Log.d(TAG, "📞 Joining call: $callId")
//
//        val intent = Intent(context, VideoCallActivity::class.java).apply {
//            putExtra("CALL_ID", callId)
//            putExtra("USER_ID", userId)
//            putExtra("USER_ROLE", userRole)
//            putExtra("IS_INITIATOR", false) // On rejoint, on n'initie pas
//            putExtra("SIGNALING_URL", signalingServerUrl)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        }
//        context.startActivity(intent)
//    }
//}
//
//// ════════════════════════════════════════════════════════════════
//// EXEMPLE D'UTILISATION dans votre Activity/Fragment
//// ════════════════════════════════════════════════════════════════
//
///*
//// Dans DoctorAppointmentsActivity ou UserAppointmentsActivity:
//
//// Quand le docteur/user clique sur "Appeler"
//private fun onCallButtonClicked(appointment: Appointment) {
//    lifecycleScope.launch {
//        val tokenManager = TokenManager(this@YourActivity)
//        val userEmail = tokenManager.getUserEmail() ?: return@launch
//        val userRole = if (isDoctor) "DOCTOR" else "USER"
//
//        // ✅ CHANGEZ CETTE URL avec votre URL ngrok
//        val signalingUrl = "wss://VOTRE-URL.ngrok-free.app"
//
//        val result = CallHelper.startVideoCall(
//            context = this@YourActivity,
//            appointmentId = appointment.id,
//            userId = userEmail,
//            userRole = userRole,
//            signalingServerUrl = signalingUrl
//        )
//
//        result.onFailure { error ->
//            Toast.makeText(this@YourActivity, "Erreur: ${error.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//}
//*/