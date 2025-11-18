package com.health.virtualdoctor.ui.user

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.network.*
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.network.RetrofitHealthClient
import com.health.virtualdoctor.ui.data.models.ApiService
import com.health.virtualdoctor.ui.data.models.ScoreRequest
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class HealthAnalysisActivity : ComponentActivity() {

    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var errorContainer: LinearLayout
    private lateinit var tvHealthScore: TextView
    private lateinit var chipRiskLevel: Chip
    private lateinit var tvAiExplanation: TextView
    private lateinit var scoreBreakdownContainer: LinearLayout
    private lateinit var anomaliesContainer: LinearLayout
    private lateinit var recommendationsContainer: LinearLayout
    private lateinit var detailsContainer: LinearLayout
    private lateinit var cardAnomalies: CardView
    private lateinit var tokenManager: TokenManager

    // API Service
    private val apiHealthService = RetrofitHealthClient.apiHealthService
    lateinit var apiService: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_analysis)
        apiService = RetrofitClient.getUserService(this)
        // Initialiser les vues
        initViews()

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Récupérer les données biométriques depuis l'intent
        val biometricJson = intent.getStringExtra("biometric_data")

        if (biometricJson != null) {
            try {
                val jsonObject = JSONObject(biometricJson)
                val biometricData = parseBiometricData(jsonObject)
                analyzeHealth(biometricData)
            } catch (e: Exception) {
                Log.e("HealthAnalysis", "Erreur parsing données: ${e.message}")
                showError("Erreur de données: ${e.message}")
            }
        } else {
            showError("Aucune donnée biométrique reçue")
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        contentContainer = findViewById(R.id.contentContainer)

        errorContainer = findViewById(R.id.errorContainer)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        tvHealthScore = findViewById(R.id.tvHealthScore)
        chipRiskLevel = findViewById(R.id.chipRiskLevel)
        tvAiExplanation = findViewById(R.id.tvAiExplanation)
        scoreBreakdownContainer = findViewById(R.id.scoreBreakdownContainer)
        anomaliesContainer = findViewById(R.id.anomaliesContainer)
        recommendationsContainer = findViewById(R.id.recommendationsContainer)
        detailsContainer = findViewById(R.id.detailsContainer)
        cardAnomalies = findViewById(R.id.cardAnomalies)
        tokenManager = TokenManager(this)
    }

    private fun parseBiometricData(json: JSONObject): BiometricDataRequest {
        // Extraire les données du JSON
        val totalSteps = json.optInt("totalSteps", 0)
        val avgHeartRate = json.optInt("avgHeartRate", 70)
        val minHeartRate = json.optInt("minHeartRate", 60)
        val maxHeartRate = json.optInt("maxHeartRate", 90)
        val totalDistanceKm = json.optString("totalDistanceKm", "0.0").toDouble()
        val totalSleepHours = json.optString("totalSleepHours", "7.0").toDouble()
        val totalHydrationLiters = json.optString("totalHydrationLiters", "2.0").toDouble()
        val stressLevel = json.optString("stressLevel", "Modéré")
        val stressScore = json.optInt("stressScore", 50)

        // Parser les listes
        val oxygenSaturation = parseOxygenData(json.optJSONArray("oxygenSaturation"))
        val bodyTemperature = parseTemperatureData(json.optJSONArray("bodyTemperature"))
        val bloodPressure = parseBloodPressureData(json.optJSONArray("bloodPressure"))
        val weight = parseWeightData(json.optJSONArray("weight"))
        val height = parseHeightData(json.optJSONArray("height"))
        val exercise = parseExerciseData(json.optJSONArray("exercise"))

        return BiometricDataRequest(
            totalSteps = totalSteps,
            avgHeartRate = avgHeartRate,
            minHeartRate = minHeartRate,
            maxHeartRate = maxHeartRate,
            totalDistanceKm = totalDistanceKm,
            totalSleepHours = totalSleepHours,
            totalHydrationLiters = totalHydrationLiters,
            stressLevel = stressLevel,
            stressScore = stressScore,
            oxygenSaturation = oxygenSaturation,
            bodyTemperature = bodyTemperature,
            bloodPressure = bloodPressure,
            weight = weight,
            height = height,
            exercise = exercise
        )
    }

    private fun parseOxygenData(jsonArray: org.json.JSONArray?): List<OxygenData> {
        val list = mutableListOf<OxygenData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(OxygenData(
                    percentage = obj.optDouble("percentage", 0.0),
                    time = obj.optString("time", "")
                ))
            }
        }
        return list
    }

    private fun parseTemperatureData(jsonArray: org.json.JSONArray?): List<TemperatureData> {
        val list = mutableListOf<TemperatureData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(TemperatureData(
                    temperature = obj.optDouble("temperature", 0.0),
                    time = obj.optString("time", "")
                ))
            }
        }
        return list
    }

    private fun parseBloodPressureData(jsonArray: org.json.JSONArray?): List<BloodPressureData> {
        val list = mutableListOf<BloodPressureData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(BloodPressureData(
                    systolic = obj.optInt("systolic", 120),
                    diastolic = obj.optInt("diastolic", 80),
                    time = obj.optString("time", "")
                ))
            }
        }
        return list
    }

    private fun parseWeightData(jsonArray: org.json.JSONArray?): List<WeightData> {
        val list = mutableListOf<WeightData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(WeightData(
                    weight = obj.optDouble("weight", 0.0),
                    time = obj.optString("time", "")
                ))
            }
        }
        return list
    }

    private fun parseHeightData(jsonArray: org.json.JSONArray?): List<HeightData> {
        val list = mutableListOf<HeightData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(HeightData(
                    height = obj.optDouble("height", 0.0),
                    time = obj.optString("time", "")
                ))
            }
        }
        return list
    }

    private fun parseExerciseData(jsonArray: org.json.JSONArray?): List<ExerciseData> {
        val list = mutableListOf<ExerciseData>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                list.add(ExerciseData(
                    title = obj.optString("title", null),
                    exerciseType = obj.optInt("exerciseType", 0),
                    exerciseTypeName = obj.optString("exerciseTypeName", "Exercice"),
                    durationMinutes = obj.optLong("durationMinutes", 0),
                    steps = obj.optLong("steps", 0),
                    distanceMeters = obj.optDouble("distanceMeters", 0.0),
                    activeCalories = obj.optInt("activeCalories", 0),
                    avgHeartRate = obj.optInt("avgHeartRate", 0)
                ))
            }
        }
        return list
    }

    private fun analyzeHealth(biometricData: BiometricDataRequest) {
        showLoading()

        lifecycleScope.launch {
            try {
                Log.d("HealthAnalysis", "📤 Envoi des données au serveur...")

                val response = withContext(Dispatchers.IO) {
                    apiHealthService.analyzeHealth(biometricData)
                }

                if (response.isSuccessful && response.body() != null) {
                    val analysisResult = response.body()!!
                    Log.d("HealthAnalysis", "✅ Analyse reçue: Score ${analysisResult.healthScore}")

                    withContext(Dispatchers.Main) {
                        displayAnalysisResult(analysisResult)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Erreur inconnue"
                    Log.e("HealthAnalysis", "❌ Erreur API: ${response.code()} - $errorBody")
                    showError("Erreur serveur: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("HealthAnalysis", "❌ Exception: ${e.message}")
                e.printStackTrace()
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }
    private fun displayAnalysisResult(result: HealthAnalysisResponse) {
        hideLoading()
        contentContainer.visibility = View.VISIBLE

        // 1. Score de santé
        tvHealthScore.text = String.format("%.1f", result.healthScore)

        // Couleur du score
        val scoreColor = when {
            result.healthScore >= 80 -> Color.parseColor("#4CAF50") // Vert
            result.healthScore >= 60 -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Rouge
        }
        tvHealthScore.setTextColor(scoreColor)

        // 2. Niveau de risque
        chipRiskLevel.text = result.riskLevel
        val riskColor = when (result.riskLevel.lowercase()) {
            "faible" -> Color.parseColor("#4CAF50")
            "modéré" -> Color.parseColor("#FF9800")
            "élevé" -> Color.parseColor("#FF5722")
            "critique" -> Color.parseColor("#D32F2F")
            else -> Color.parseColor("#9E9E9E")
        }
        chipRiskLevel.setChipBackgroundColorResource(android.R.color.transparent)
        chipRiskLevel.setTextColor(riskColor)

        // 3. Explication IA
        tvAiExplanation.text = result.aiExplanation

        // 4. Détails du score
        displayScoreBreakdown(result.insights.score_breakdown)

        // 5. Anomalies
        if (result.anomalies.isNotEmpty()) {
            cardAnomalies.visibility = View.VISIBLE
            displayAnomalies(result.anomalies)
        } else {
            cardAnomalies.visibility = View.GONE
        }

        // 6. Recommandations
        displayRecommendations(result.recommendations)

        // 7. Détails métriques
        displayMetricsDetails(result.insights)

        // 8. Enregistrer le score sur le serveur
        sendScoreToServer(result.healthScore)
    }


    private fun sendScoreToServer(score: Double) {
        val email = tokenManager.getUserEmail() ?: return
        val token = tokenManager.getAccessToken() ?: return

        val request = ScoreRequest(score)

        lifecycleScope.launch {
            try {
                val response = apiService.setScorePatient(
                    token = "Bearer $token",
                    email = email,
                    scoreRequest = request
                )
                if (response.isSuccessful) {
                    Log.d("ScoreUpdate", "Score updated successfully")
                } else {
                    Log.e("ScoreUpdate", "Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ScoreUpdate", "Exception: ${e.message}")
            }
        }
    }


    private fun displayScoreBreakdown(breakdown: ScoreBreakdown) {
        scoreBreakdownContainer.removeAllViews()

        val categories = mapOf(
            "🚶 Activité" to (breakdown.activity to 25.0),
            "❤️ Cardiovasculaire" to (breakdown.cardiovascular to 25.0),
            "💤 Sommeil" to (breakdown.sleep to 20.0),
            "💧 Hydratation" to (breakdown.hydration to 10.0),
            "🧠 Stress" to (breakdown.stress to 10.0),
            "🩺 Signes vitaux" to (breakdown.vitals to 10.0)
        )

        for ((label, scoreMaxPair) in categories) {
            val (score, max) = scoreMaxPair
            val itemView = createScoreItem(label, score, max)
            scoreBreakdownContainer.addView(itemView)
        }
    }

    private fun createScoreItem(label: String, score: Double, max: Double): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
        }

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvScore = TextView(this).apply {
            text = String.format("%.1f / %.0f", score, max)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)

            // Couleur basée sur le pourcentage
            val percentage = (score / max) * 100
            val color = when {
                percentage >= 80 -> Color.parseColor("#4CAF50")
                percentage >= 60 -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#F44336")
            }
            setTextColor(color)
        }

        layout.addView(tvLabel)
        layout.addView(tvScore)

        return layout
    }

    private fun displayAnomalies(anomalies: List<String>) {
        anomaliesContainer.removeAllViews()

        for (anomaly in anomalies) {
            val tvAnomaly = TextView(this).apply {
                text = "• $anomaly"
                textSize = 14f
                setTextColor(Color.parseColor("#D32F2F"))
                setPadding(0, dpToPx(4), 0, dpToPx(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            anomaliesContainer.addView(tvAnomaly)
        }
    }

    private fun displayRecommendations(recommendations: List<String>) {
        recommendationsContainer.removeAllViews()

        for (recommendation in recommendations) {
            val tvRecommendation = TextView(this).apply {
                text = "• $recommendation"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setPadding(0, dpToPx(6), 0, dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setLineSpacing(4f, 1f)
            }
            recommendationsContainer.addView(tvRecommendation)
        }
    }

    private fun displayMetricsDetails(insights: InsightsData) {
        detailsContainer.removeAllViews()

        // Activité
        addDetailItem("🚶 Pas", "${insights.activity_details.steps} pas")
        addDetailItem("📏 Distance", "${insights.activity_details.distance_km} km")
        addDetailItem("🏋️ Exercices", "${insights.activity_details.exercises_count} session(s)")

        addDivider()

        // Cardiovasculaire
        addDetailItem("❤️ FC Moyenne", "${insights.cardiovascular_details.avg_heart_rate} bpm")
        addDetailItem("📊 Variabilité FC", "${insights.cardiovascular_details.hr_variability} bpm")

        addDivider()

        // Sommeil
        addDetailItem("💤 Heures de sommeil", "${insights.sleep_details.hours}h")
        addDetailItem("✨ Qualité", insights.sleep_details.quality)

        addDivider()

        // Stress
        addDetailItem("🧠 Niveau de stress", insights.stress_details.level)
        addDetailItem("📈 Score stress", "${insights.stress_details.score}/100")
    }

    private fun addDetailItem(label: String, value: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
        }

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvValue = TextView(this).apply {
            text = value
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null))
        }

        layout.addView(tvLabel)
        layout.addView(tvValue)

        detailsContainer.addView(layout)
    }

    private fun addDivider() {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(8))
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        detailsContainer.addView(divider)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        contentContainer.visibility = View.GONE

        errorContainer.visibility = View.VISIBLE
        tvErrorMessage.text = message

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}