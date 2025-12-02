package com.health.virtualdoctor.ui.data.api


import android.content.Context
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import kotlin.apply
import kotlin.text.contains

class AuthInterceptor(private val context: Context) : Interceptor {

    private val tokenManager = TokenManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ✅ Ne pas ajouter de token pour les endpoints publics
        if (originalRequest.url.encodedPath.contains("/auth/login") ||
            originalRequest.url.encodedPath.contains("/auth/register") ||
            originalRequest.url.encodedPath.contains("/doctors/register") ||
            originalRequest.url.encodedPath.contains("/doctors/login")) {
            return chain.proceed(originalRequest)
        }

        // ✅ Ajouter le token d'accès s'il existe
        val accessToken = tokenManager.getAccessToken()
        val requestWithToken = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(requestWithToken)

        // ✅ Si 401 => essayer de rafraîchir le token
        if (response.code == 401 && accessToken != null) {
            response.close()

            synchronized(this) {
                val newAccessToken = refreshToken()

                if (newAccessToken != null) {
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()

                    response = chain.proceed(newRequest)
                } else {
                    tokenManager.clearTokens()
                }
            }
        }

        return response
    }

    private fun refreshToken(): String? = runBlocking {
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null

            val client = OkHttpClient.Builder().build()

            // ✅ FIX: Format JSON correct
            val jsonBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            // ✅ Utiliser l'URL Auth correcte
            val request = Request.Builder()
                .url("${RetrofitClient.getAuthBaseUrl()}api/v1/auth/refresh")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "")
                val newAccessToken = jsonResponse.getString("accessToken")
                val newRefreshToken = jsonResponse.getString("refreshToken")

                tokenManager.saveTokens(newAccessToken, newRefreshToken)
                return@runBlocking newAccessToken
            }

            return@runBlocking null

        } catch (e: Exception) {
            e.printStackTrace()
            return@runBlocking null
        }
    }
}