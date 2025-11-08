package com.health.virtualdoctor.ui.data.models

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==========================================
    // AUTH SERVICE (port 8082)
    // ==========================================
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body refreshToken: String): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body refreshToken: String): Response<Unit>

    // ==========================================
    // USER SERVICE (port 8085)
    // ==========================================
    @GET("api/v1/users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>

    @PUT("api/v1/users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateUserProfileRequest
    ): Response<UserProfileResponse>

    @PUT("api/v1/users/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    // ✅ NOUVEAU: Forgot Password pour USER
    @POST("api/v1/users/forgot-password")
    suspend fun forgotUserPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // ==========================================
    // DOCTOR SERVICE (port 8083)
    // ==========================================
    @POST("api/doctors/register")
    suspend fun registerDoctor(@Body request: DoctorRegisterRequest): Response<DoctorResponse>

    @POST("api/doctors/login")
    suspend fun loginDoctor(@Body request: LoginRequest): Response<Map<String, Any>>

    @GET("api/doctors/profile")
    suspend fun getDoctorProfile(
        @Header("Authorization") token: String
    ): Response<DoctorResponse>

    @PUT("api/doctors/profile")
    suspend fun updateDoctorProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateDoctorProfileRequest
    ): Response<DoctorResponse>

    @GET("api/doctors/activation-status")
    suspend fun getDoctorActivationStatus(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @GET("api/doctors/debug/all-emails")
    suspend fun getAllDoctorEmails(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    // ✅ NOUVEAU: Change Password pour DOCTOR
    @POST("api/doctors/change-password")
    suspend fun changeDoctorPassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Map<String, Any>>

    // ✅ NOUVEAU: Forgot Password pour DOCTOR
    @POST("api/doctors/forgot-password")
    suspend fun forgotDoctorPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    // ==========================================
    // NOTIFICATION SERVICE
    // ==========================================
    @POST("api/notifications/fcm/token")
    suspend fun saveFcmToken(
        @Header("Authorization") token: String,
        @Body request: FCMTokenRequest
    ): Response<Map<String, String>>

    // ==========================================
    // NUTRITION SERVICE (Cloudflare Worker)
    // ==========================================
    @Multipart
    @POST("api/nutrition/analyze")
    suspend fun analyzeNutrition(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("use_ai") useAi: RequestBody
    ): Response<NutritionAnalysisResponse>
}

// ==========================================
// DATA CLASSES
// ==========================================

// User Profile
data class UserProfileResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val roles: Set<String>,
    val isActivated: Boolean,
    val createdAt: String
)

data class UpdateUserProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val email: String?,
    val profilePictureUrl: String?
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// Doctor Profile
data class UpdateDoctorProfileRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val specialization: String,
    val hospitalAffiliation: String,
    val yearsOfExperience: Int?,
    val officeAddress: String?,
    val consultationHours: String?,
    val profilePictureUrl: String? = null
)

// Nutrition Analysis
data class NutritionAnalysisResponse(
    val success: Boolean,
    val data: NutritionData?,
    val message: String?
)

data class NutritionData(
    val detected_foods: List<DetectedFood>,
    val total_nutrition: TotalNutrition,
    val alternatives: List<Alternative>?
)

data class DetectedFood(
    val food_name: String,
    val confidence: Double,
    val nutrition: NutritionInfo
)

data class TotalNutrition(
    val calories: Double,
    val proteins: Double,
    val carbohydrates: Double,
    val fats: Double,
    val fiber: Double
)

data class NutritionInfo(
    val calories: Double,
    val proteins: Double,
    val carbohydrates: Double,
    val fats: Double,
    val fiber: Double
)

data class Alternative(
    val name: String,
    val confidence: Double
)