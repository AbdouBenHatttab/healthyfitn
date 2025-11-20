// Complete ApiService.kt with wrapper
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
    // USER SERVICE (port 8085) - WITH WRAPPER
    // ==========================================
    @GET("api/v1/users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfileResponse>>

    @PUT("api/v1/users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateUserProfileRequest
    ): Response<ApiResponse<UserProfileResponse>>

    @PUT("api/v1/users/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<String>>

    @POST("api/v1/users/forgot-password")
    suspend fun forgotUserPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("api/v1/admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<UserApiResponse<List<UserManagementResponse>>>

    @POST("api/v1/admin/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Body request: UserSearchRequest
    ): Response<UserApiResponse<UserPageResponse>>

    @GET("api/v1/admin/users/{userId}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<UserApiResponse<UserManagementResponse>>

    @DELETE("api/v1/admin/users/{userId}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<UserApiResponse<Void>>

    @GET("api/v1/admin/users/role/{role}")
    suspend fun getUsersByRole(
        @Header("Authorization") token: String,
        @Path("role") role: String
    ): Response<UserApiResponse<List<UserManagementResponse>>>

    @GET("api/v1/admin/users/statistics")
    suspend fun getUserStatistics(
        @Header("Authorization") token: String
    ): Response<UserApiResponse<UserStatistics>>

    /**
     * Get all appointments for authenticated patient
     */
    @GET("api/v1/appointments")
    suspend fun getPatientAppointments(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<AppointmentResponse>>>

    /**
     * Create new appointment
     */
    @POST("api/v1/appointments")
    suspend fun createAppointment(
        @Header("Authorization") token: String,
        @Body request: AppointmentRequest
    ): Response<ApiResponse<AppointmentResponse>>

    /**
     * Cancel appointment
     */
    @POST("api/v1/appointments/{appointmentId}/cancel")
    suspend fun cancelAppointmentByUser(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<String>>

    /**
     * Get available doctors for appointments
     */
    @GET("api/v1/appointments/doctors")
    suspend fun getAvailableDoctors(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<DoctorAvailableResponse>>>
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

    @PUT("api/doctors/change-password")
    suspend fun changeDoctorPassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Map<String, Any>>

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


    /**
     * Get pending appointments (need doctor response)
     */
    @GET("api/doctors/appointments/pending")
    suspend fun getPendingAppointments(
        @Header("Authorization") token: String
    ): Response<List<AppointmentResponse>>

    /**
     * Accept a pending appointment
     */
    @POST("api/doctors/appointments/{appointmentId}/accept")
    suspend fun acceptAppointment(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String
    ): Response<AppointmentResponse>

    /**
     * Reject a pending appointment with reason
     */
    @POST("api/doctors/appointments/{appointmentId}/reject")
    suspend fun rejectAppointment(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String
    ): Response<AppointmentResponse>

    // ==========================================
    // APPOINTMENT SERVICE
    // ==========================================
    /**
     * Get all appointments for authenticated doctor
     */
    @GET("api/doctors/appointments")
    suspend fun getDoctorAppointments(
        @Header("Authorization") token: String
    ): Response<List<AppointmentResponse>>

    /**
     * Get upcoming appointments only
     */
    @GET("api/doctors/appointments/upcoming")
    suspend fun getUpcomingAppointments(
        @Header("Authorization") token: String
    ): Response<List<AppointmentResponse>>

    /**
     * Get doctor's patient list
     */
    @GET("api/doctors/appointments/patients")
    suspend fun getDoctorPatients(
        @Header("Authorization") token: String
    ): Response<List<PatientInfoResponse>>

    /**
     * Get dashboard statistics
     */
    @GET("api/doctors/appointments/dashboard/stats")
    suspend fun getDoctorStats(
        @Header("Authorization") token: String
    ): Response<DoctorStatsResponse>

    /**
     * Complete an appointment
     */
    @POST("api/doctors/appointments/{appointmentId}/complete")
    suspend fun completeAppointment(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String,
        @Body request: Map<String, String>
    ): Response<AppointmentResponse>

    /**
     * Cancel appointment (Doctor side)
     */
    @POST("api/doctors/appointments/{appointmentId}/cancel")
    suspend fun cancelAppointmentByDoctor(
        @Header("Authorization") token: String,
        @Path("appointmentId") appointmentId: String,
        @Body request: Map<String, String>
    ): Response<AppointmentResponse> // change Map to AppointmentResponse
    // ==========================================
    // NUTRITION SERVICE
    // ==========================================
    @Multipart
    @POST("api/nutrition/analyze")
    suspend fun analyzeNutrition(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("use_ai") useAi: RequestBody
    ): Response<NutritionAnalysisResponse>

    // ==========================================
    // ADMIN SERVICE ENDPOINTS
    // ==========================================
    @GET("api/admin/doctors/pending")
    suspend fun getPendingDoctors(
        @Header("Authorization") token: String
    ): Response<List<DoctorPendingResponse>>

    @GET("api/admin/doctors/pending/count")
    suspend fun getPendingDoctorsCount(
        @Header("Authorization") token: String
    ): Response<Map<String, Long>>

    @GET("api/admin/doctors/activated")
    suspend fun getActivatedDoctors(
        @Header("Authorization") token: String
    ): Response<List<DoctorPendingResponse>>

    @POST("api/admin/doctors/activate")
    suspend fun activateDoctor(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    // Set score for patient
    @PUT("api/v1/users/{email}/score")
    suspend fun setScorePatient(
        @Header("Authorization") token: String,
        @Path("email") email: String,
        @Body scoreRequest: ScoreRequest
    ): Response<ApiResponse<UserProfileResponse>>

}

// ==========================================
// WRAPPER FOR USER SERVICE RESPONSES
// ==========================================
data class ScoreRequest(
    val score: Double
)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// ==========================================
// DATA CLASSES
// ==========================================

// User Profile (for profile endpoints)
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

//data class PatientInfoResponse(
//    val id: String,
//    val email: String,
//    val firstName: String,
//    val lastName: String,
//    val fullName: String,
//    val phoneNumber: String?,
//    val profilePictureUrl: String?
//)

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
