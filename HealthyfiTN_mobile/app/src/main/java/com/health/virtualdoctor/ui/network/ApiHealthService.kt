package com.health.virtualdoctor.network

import retrofit2.Response
import retrofit2.http.*

// 📊 Modèles de données pour l'API

// 📤 Requête d'analyse de santé
data class BiometricDataRequest(
    val totalSteps: Int,
    val avgHeartRate: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val totalDistanceKm: Double,
    val totalSleepHours: Double,
    val totalHydrationLiters: Double,
    val stressLevel: String,
    val stressScore: Int,
    val dailyTotalCalories: Int = 0,
    val oxygenSaturation: List<OxygenData> = emptyList(),
    val bodyTemperature: List<TemperatureData> = emptyList(),
    val bloodPressure: List<BloodPressureData> = emptyList(),
    val weight: List<WeightData> = emptyList(),
    val height: List<HeightData> = emptyList(),
    val exercise: List<ExerciseData> = emptyList()
)

data class OxygenData(val percentage: Double, val time: String)
data class TemperatureData(val temperature: Double, val time: String)
data class BloodPressureData(val systolic: Int, val diastolic: Int, val time: String)
data class WeightData(val weight: Double, val time: String)
data class HeightData(val height: Double, val time: String)
data class ExerciseData(
    val title: String?,
    val exerciseType: Int,
    val exerciseTypeName: String,
    val durationMinutes: Long,
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val activeCalories: Int = 0,
    val avgHeartRate: Int = 0
)

// 📥 Réponse d'analyse de santé
data class HealthAnalysisResponse(
    val healthScore: Double,
    val riskLevel: String,
    val anomalies: List<String>,
    val recommendations: List<String>,
    val insights: InsightsData,
    val aiExplanation: String
)

data class InsightsData(
    val score_breakdown: ScoreBreakdown,
    val activity_details: ActivityDetails,
    val cardiovascular_details: CardiovascularDetails,
    val sleep_details: SleepDetails,
    val stress_details: StressDetails
)

data class ScoreBreakdown(
    val activity: Double,
    val cardiovascular: Double,
    val sleep: Double,
    val hydration: Double,
    val stress: Double,
    val vitals: Double
)

data class ActivityDetails(
    val steps: Int,
    val distance_km: Double,
    val exercises_count: Int
)

data class CardiovascularDetails(
    val avg_heart_rate: Int,
    val hr_variability: Int
)

data class SleepDetails(
    val hours: Double,
    val quality: String
)

data class StressDetails(
    val level: String,
    val score: Int
)

// 🌐 Interface API
interface ApiHealthService {

    /**
     * Analyse complète de santé
     * POST /analyze-health
     */
    @POST("analyze-health")
    suspend fun analyzeHealth(
        @Body data: BiometricDataRequest
    ): Response<HealthAnalysisResponse>

    /**
     * Tendances de santé
     * GET /health-trends/{email}?days=30
     */
    @GET("health-trends/{email}")
    suspend fun getHealthTrends(
        @Path("email") email: String,
        @Query("days") days: Int = 30
    ): Response<Any>

    /**
     * Alertes de risque
     * GET /risk-alerts/{email}?period_days=7
     */
    @GET("risk-alerts/{email}")
    suspend fun getRiskAlerts(
        @Path("email") email: String,
        @Query("period_days") periodDays: Int = 7,
        @Query("specific_date") specificDate: String? = null
    ): Response<RiskAlertsResponse>

    /**
     * Objectifs personnalisés
     * POST /personalized-goals/{email}
     */
    @POST("personalized-goals/{user_id}")
    suspend fun getPersonalizedGoals(
        @Path("user_id") email: String,
        @Body preferences: Any
    ): Response<Any>

    /**
     * Résumé complet
     * GET /health-summary/{email}
     */
    @GET("health-summary/{email}")
    suspend fun getHealthSummary(
        @Path("email") email: String
    ): Response<Any>

    /**
     * Health check
     * GET /health
     */
    @GET("health")
    suspend fun healthCheck(): Response<Any>
}