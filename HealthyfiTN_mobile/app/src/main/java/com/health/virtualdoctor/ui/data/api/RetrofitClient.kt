package com.health.virtualdoctor.ui.data.api

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.health.virtualdoctor.ui.data.models.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.health.virtualdoctor.BuildConfig

object RetrofitClient {

    // ✅ URLs des services via Cloudflare Tunnels
<<<<<<< HEAD
    val cloudflared = BuildConfig.CLOUDFLARED_URL
    private val AUTH_BASE_URL = "$cloudflared/auth-service/" // Port 8082
    private  val DOCTOR_BASE_URL = "$cloudflared/doctor-activation-service/" // Port 8083
    private  val NOTIFICATION_BASE_URL = "$cloudflared/notification-service/" // Port 8084
    private  val USER_BASE_URL = "$cloudflared/user-service/" // Port 8085
    //private const val DOCTOR_SERVICE_BASE_URL = "https://macie-unprognosticative-kylan.ngrok-free.dev"
=======
    private const val AUTH_BASE_URL = "https://mini-cap-impose-hobby.trycloudflare.com/auth-service/" // Port 8082
    private const val DOCTOR_BASE_URL = "https://mini-cap-impose-hobby.trycloudflare.com/doctor-activation-service/" // Port 8083
    private const val NOTIFICATION_BASE_URL = "https://mini-cap-impose-hobby.trycloudflare.com/notification-service/" // Port 8084
    private const val USER_BASE_URL = "https://mini-cap-impose-hobby.trycloudflare.com/user-service/" // Port 8085
>>>>>>> b9f9b4a (working)

    private var authRetrofit: Retrofit? = null
    private var doctorRetrofit: Retrofit? = null
    private var notificationRetrofit: Retrofit? = null
    private var userRetrofit: Retrofit? = null

    private var videoCallRetrofit: Retrofit? = null
    private var authApiService: ApiService? = null
    private var doctorApiService: ApiService? = null
    private var notificationApiService: ApiService? = null
    private var userApiService: ApiService? = null

    private var appContext: Context? = null

    // ✅ Init function for compatibility
    fun init(context: Context) {
        appContext = context.applicationContext
        getAuthService(appContext!!)
    }

    // ✅ AUTH Service (port 8082)
    fun getAuthService(context: Context): ApiService {
        if (authApiService == null) {
            authRetrofit = createRetrofit(AUTH_BASE_URL, context)
            authApiService = authRetrofit!!.create(ApiService::class.java)
        }
        return authApiService!!
    }

    // ✅ DOCTOR Service (port 8083)
    fun getDoctorService(context: Context): ApiService {
        if (doctorApiService == null) {
            doctorRetrofit = createRetrofit(DOCTOR_BASE_URL, context)
            doctorApiService = doctorRetrofit!!.create(ApiService::class.java)
        }
        return doctorApiService!!
    }

    // ✅ NOTIFICATION Service (port 8084) - 🆕 VIA CLOUDFLARE
    fun getNotificationService(context: Context): ApiService {
        if (notificationApiService == null) {
            notificationRetrofit = createRetrofit(NOTIFICATION_BASE_URL, context)
            notificationApiService = notificationRetrofit!!.create(ApiService::class.java)
        }
        return notificationApiService!!
    }

    // ✅ USER Service (port 8085)
    fun getUserService(context: Context): ApiService {
        if (userApiService == null) {
            userRetrofit = createRetrofit(USER_BASE_URL, context)
            userApiService = userRetrofit!!.create(ApiService::class.java)
        }
        return userApiService!!
    }

    // ✅ Default service (for backward compatibility)
    @Deprecated("Use getAuthService(), getDoctorService(), getUserService(), or getNotificationService() instead")
    fun getApiService(context: Context): ApiService {
        return getAuthService(context)
    }

    private fun createRetrofit(baseUrl: String, context: Context): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(context.applicationContext)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    // ✅ Service WebRTC
    fun getWebRTCService(context: Context): WebRTCApiService {
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "🔧 Creating WebRTC Service")
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "   Base URL: $DOCTOR_BASE_URL")
        Log.d(TAG, "═══════════════════════════════════════════")

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(DOCTOR_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(WebRTCApiService::class.java)
    }

// ✅ CRITICAL: Générer URL WebSocket AVEC TOKEN

    fun getWebSocketUrl(callId: String, userId: String, context: Context): String {
        // Clean base URL (remove protocol)
        val cleanBaseUrl = DOCTOR_BASE_URL
            .replace("https://", "")
            .replace("http://", "")
            .trim()

        // Get JWT token from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("access_token", "") ?: ""

        // ✅ FIXED: Proper URL encoding for token
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8")

        // ✅ Build WebSocket URL with proper format
        val wsUrl = "wss://$cleanBaseUrl/ws/webrtc/$callId?userId=$encodedUserId&token=$encodedToken"

        Log.d(TAG, "═══════════════════════════════════════════════════════════════")
        Log.d(TAG, "🔌 GENERATING WEBSOCKET URL")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════")
        Log.d(TAG, "   Clean base: $cleanBaseUrl")
        Log.d(TAG, "   Call ID: $callId")
        Log.d(TAG, "   User ID: $userId")
        Log.d(TAG, "   Token present: ${token.isNotEmpty()}")
        Log.d(TAG, "   Token length: ${token.length}")
        Log.d(TAG, "   Final URL: $wsUrl")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════")

        // Validation
        if (!wsUrl.startsWith("wss://") && !wsUrl.startsWith("ws://")) {
            throw IllegalArgumentException("Invalid WebSocket URL: $wsUrl")
        }

        if (token.isEmpty()) {
            Log.e(TAG, "⚠️ WARNING: Token is empty! Authentication will fail!")
        }

        return wsUrl
    }

    /**
     * Alternative: Obtenir juste la base URL WebSocket
     */
    fun getWebSocketBaseUrl(): String {
        return DOCTOR_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
    }

    // ✅ Getters pour les URLs (utiles pour debug)
    fun getAuthBaseUrl(): String = AUTH_BASE_URL
    fun getDoctorBaseUrl(): String = DOCTOR_BASE_URL
    fun getNotificationBaseUrl(): String = NOTIFICATION_BASE_URL
    fun getUserBaseUrl(): String = USER_BASE_URL


    /**
     * Clear all cached instances (useful for logout)
     */
    fun clearAll() {
        authApiService = null
        doctorApiService = null
        notificationApiService = null
        userApiService = null

        authRetrofit = null
        doctorRetrofit = null
        notificationRetrofit = null
        userRetrofit = null
        videoCallRetrofit = null
    }

}