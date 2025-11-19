package com.health.virtualdoctor.ui.user

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.health.virtualdoctor.R
import com.health.virtualdoctor.network.RetrofitHealthClient
import com.health.virtualdoctor.ui.utils.TokenManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 📊 Modèles de données pour la réponse API
data class HealthTrendsResponse(
    val email: String,
    @SerializedName("period_days") val periodDays: Int,
    @SerializedName("data_points") val dataPoints: Int,
    val trends: TrendsData,
    @SerializedName("moving_averages") val movingAverages: Map<String, List<Double>>?,
    val statistics: Map<String, StatisticData>
)

data class TrendsData(
    val dates: List<String>,
    val steps: List<Int>,
    @SerializedName("heart_rate") val heartRate: List<Int>,
    @SerializedName("sleep_hours") val sleepHours: List<Double>,
    @SerializedName("stress_score") val stressScore: List<Int>,
    val hydration: List<Double>,
    @SerializedName("health_scores") val healthScores: List<Double>,
    val weight: List<Double>
)

data class StatisticData(
    val min: Double,
    val max: Double,
    val mean: Double,
    val std: Double,
    val trend: String
)

class HealthTrendsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView
    private lateinit var errorLayout: LinearLayout
    private lateinit var chipGroup: ChipGroup

    private var currentPeriod = 30
    private var trendsData: HealthTrendsResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_trends)

        tokenManager = TokenManager(this)

        initializeViews()
        setupPeriodSelector()
        loadHealthTrends(currentPeriod)
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)
        errorLayout = findViewById(R.id.errorLayout)
        chipGroup = findViewById(R.id.chipGroupPeriod)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRetry).setOnClickListener {
            loadHealthTrends(currentPeriod)
        }
    }

    private fun setupPeriodSelector() {
        val periods = listOf(7, 14, 30, 60, 90)

        periods.forEach { days ->
            val chip = Chip(this).apply {
                text = when(days) {
                    7 -> "1 semaine"
                    14 -> "2 semaines"
                    30 -> "1 mois"
                    60 -> "2 mois"
                    90 -> "3 mois"
                    else -> "$days jours"
                }
                isCheckable = true
                isChecked = days == currentPeriod
                setOnClickListener {
                    currentPeriod = days
                    loadHealthTrends(days)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun loadHealthTrends(days: Int) {
        val email = tokenManager.getUserEmail()

        if (email.isNullOrEmpty()) {
            showError("Email utilisateur introuvable")
            return
        }

        showLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitHealthClient.apiHealthService.getHealthTrends(email, days)

                if (response.isSuccessful && response.body() != null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(response.body())
                    trendsData = gson.fromJson(jsonString, HealthTrendsResponse::class.java)

                    displayTrends(trendsData!!)
                    showContent()
                } else {
                    showError("Erreur ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }

    private fun displayTrends(data: HealthTrendsResponse) {
        // 📊 Graphique principal - Score de santé
        displayMainChart(data)

        // 📈 Cartes de métriques individuelles
        displayMetricCard(
            R.id.cardSteps,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Pas quotidiens",
            data.trends.steps.map { it.toFloat() },
            data.statistics["steps"],
            "#4CAF50",
            "pas"
        )

        displayMetricCard(
            R.id.cardHeartRate,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Fréquence cardiaque",
            data.trends.heartRate.map { it.toFloat() },
            data.statistics["heart_rate"],
            "#F44336",
            "bpm"
        )

        displayMetricCard(
            R.id.cardSleep,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Sommeil",
            data.trends.sleepHours.map { it.toFloat() },
            data.statistics["sleep_hours"],
            "#2196F3",
            "h"
        )

        displayMetricCard(
            R.id.cardStress,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Stress",
            data.trends.stressScore.map { it.toFloat() },
            data.statistics["stress_score"],
            "#FF9800",
            "/100"
        )

        displayMetricCard(
            R.id.cardHydration,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Hydratation",
            data.trends.hydration.map { it.toFloat() },
            data.statistics["hydration"],
            "#00BCD4",
            "L"
        )

        displayMetricCard(
            R.id.cardWeight,
            R.id.chartMetric,
            R.id.tvMetricTitle,
            R.id.tvMetricAvg,
            R.id.tvMetricTrend,
            R.id.tvMetricRange,
            "Poids",
            data.trends.weight.map { it.toFloat() },
            data.statistics["weight"],
            "#9C27B0",
            "kg"
        )

        // 📅 Info sur la période
        findViewById<TextView>(R.id.tvDataPoints).text =
            "${data.dataPoints} jours de données analysées"
    }


    private fun displayMainChart(data: HealthTrendsResponse) {
        val chart = findViewById<LineChart>(R.id.mainChart)

        val entries = data.trends.healthScores.mapIndexed { index, score ->
            Entry(index.toFloat(), score.toFloat())
        }

        val dataSet = LineDataSet(entries, "Score de santé").apply {
            color = ContextCompat.getColor(this@HealthTrendsActivity, R.color.primary)
            setCircleColor(color)
            lineWidth = 3f
            circleRadius = 4f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            this.data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(formatDates(data.trends.dates))
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }

            axisRight.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            animateX(1000)
            invalidate()
        }

        // Moyenne du score
        val avgScore = data.statistics["health_scores"]?.mean ?: 0.0
        findViewById<TextView>(R.id.tvMainScore).text = "%.1f/100".format(avgScore)

        val trend = data.statistics["health_scores"]?.trend ?: "stable"
        findViewById<TextView>(R.id.tvMainTrend).apply {
            text = when(trend) {
                "increasing" -> "↑ En amélioration"
                "decreasing" -> "↓ En baisse"
                else -> "→ Stable"
            }
            setTextColor(getTrendColor(trend))
        }
    }

    private fun displayMetricCard(
        cardId: Int,
        chartId: Int,
        titleId: Int,
        avgId: Int,
        trendId: Int,
        rangeId: Int,
        title: String,
        values: List<Float>,
        stats: StatisticData?,
        colorHex: String,
        unit: String
    ) {
        if (stats == null) return

        // Récupérer la carte parent
        val card = findViewById<View>(cardId)

        // Trouver les vues à l'intérieur de la carte
        val tvTitle = card.findViewById<TextView>(titleId)
        val tvAvg = card.findViewById<TextView>(avgId)
        val tvTrend = card.findViewById<TextView>(trendId)
        val tvRange = card.findViewById<TextView>(rangeId)
        val chart = card.findViewById<LineChart>(chartId)

        // Mettre à jour le contenu
        tvTitle.text = title
        tvAvg.text = "%.1f$unit".format(stats.mean)

        tvTrend.apply {
            text = when(stats.trend) {
                "increasing" -> "↑"
                "decreasing" -> "↓"
                else -> "→"
            }
            setTextColor(getTrendColor(stats.trend))
        }

        tvRange.text = "%.1f - %.1f$unit".format(stats.min, stats.max)

        // Mini graphique
        val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "").apply {
            color = Color.parseColor(colorHex)
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            setTouchEnabled(false)
            setDrawBorders(false)
            setDrawGridBackground(false)
            animateX(800)
            invalidate()
        }
    }


    private fun formatDates(dates: List<String>): List<String> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        return dates.mapNotNull { dateStr ->
            try {
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getTrendColor(trend: String): Int {
        return when(trend) {
            "increasing" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            "decreasing" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
            else -> ContextCompat.getColor(this, android.R.color.darker_gray)
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
        errorLayout.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvErrorMessage).text = message
    }
}