package com.health.virtualdoctor.network

// 📊 Modèles pour Risk Alerts API

data class RiskAlertsResponse(
    val email: String,
    val alert_level: String,
    val analysis_period: String,
    val analysis_type: String,
    val data_points_analyzed: Int,
    val averages_computed: AveragesComputed,
    val alerts: List<String>,
    val risk_factors: List<RiskFactor>,
    val action_priorities: List<ActionPriority>,
    val next_checkup_recommended: String
)

data class AveragesComputed(
    val steps: Int,
    val sleep_hours: Double,
    val heart_rate: Int,
    val stress_score: Int,
    val hydration_liters: Double
)

data class RiskFactor(
    val type: String,
    val severity: String,
    val description: String,
    val probability: Double,
    val actions: List<String>
)

data class ActionPriority(
    val action: String,
    val category: String,
    val urgency: String,
    val impact: String
)