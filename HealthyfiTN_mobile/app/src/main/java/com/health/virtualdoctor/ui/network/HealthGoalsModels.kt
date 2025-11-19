package com.health.virtualdoctor.network

// 📊 Modèles pour Health Goals API

// 📤 Requête de préférences
data class GoalPreferencesRequest(
    val preferred_goals: List<String>,
    val timeframe_days: Int,
    val difficulty: String
)

// 📥 Réponse d'objectifs personnalisés
data class PersonalizedGoalsResponse(
    val email: String,
    val total_goals: Int,
    val high_priority_count: Int,
    val timeframe_days: Int,
    val difficulty: String,
    val estimated_improvement: Double,
    val average_current_health_score: String,
    val projected_health_score: String,
    val goals: List<HealthGoal>
)

data class HealthGoal(
    val category: String,
    val title: String,
    val current: Any, // Peut être Int ou Double
    val target: Any,  // Peut être Int ou Double
    val timeframe: String,
    val priority: String,
    val tips: List<String>,
    val milestones: List<Milestone>,
    val expected_improvement: String
)

data class Milestone(
    val day: Int,
    val target: Double,
    val description: String
)