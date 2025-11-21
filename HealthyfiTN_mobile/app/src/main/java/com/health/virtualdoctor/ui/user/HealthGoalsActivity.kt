package com.health.virtualdoctor.ui.user

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.health.virtualdoctor.R
import com.health.virtualdoctor.network.*
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthGoalsActivity : ComponentActivity() {

    // Views - Configuration
    private lateinit var chipGroupGoals: ChipGroup
    private lateinit var chipActivity: Chip
    private lateinit var chipSleep: Chip
    private lateinit var chipHydration: Chip
    private lateinit var chipStress: Chip
    private lateinit var chipCardio: Chip
    private lateinit var sliderTimeframe: Slider
    private lateinit var tvTimeframeValue: TextView
    private lateinit var chipGroupDifficulty: ChipGroup
    private lateinit var chipEasy: Chip
    private lateinit var chipModerate: Chip
    private lateinit var chipChallenging: Chip
    private lateinit var btnGenerateGoals: MaterialButton

    // Views - Résultats
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsContainer: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var tvCurrentScore: TextView
    private lateinit var tvProjectedScore: TextView
    private lateinit var tvTotalGoals: TextView
    private lateinit var tvTimeframe: TextView
    private lateinit var tvImprovement: TextView
    private lateinit var goalsListContainer: LinearLayout

    // API
    private val apiService = RetrofitHealthClient.apiHealthService
    private lateinit var tokenManager: TokenManager
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_goals)

        tokenManager = TokenManager(this)
        userEmail = tokenManager.getUserEmail() ?: "unknown@example.com"

        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Toolbar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Configuration
        chipGroupGoals = findViewById(R.id.chipGroupGoals)
        chipActivity = findViewById(R.id.chipActivity)
        chipSleep = findViewById(R.id.chipSleep)
        chipHydration = findViewById(R.id.chipHydration)
        chipStress = findViewById(R.id.chipStress)
        chipCardio = findViewById(R.id.chipCardio)
        sliderTimeframe = findViewById(R.id.sliderTimeframe)
        tvTimeframeValue = findViewById(R.id.tvTimeframeValue)
        chipGroupDifficulty = findViewById(R.id.chipGroupDifficulty)
        chipEasy = findViewById(R.id.chipEasy)
        chipModerate = findViewById(R.id.chipModerate)
        chipChallenging = findViewById(R.id.chipChallenging)
        btnGenerateGoals = findViewById(R.id.btnGenerateGoals)

        // Résultats
        progressBar = findViewById(R.id.progressBar)
        resultsContainer = findViewById(R.id.resultsContainer)
        tvError = findViewById(R.id.tvError)
        tvCurrentScore = findViewById(R.id.tvCurrentScore)
        tvProjectedScore = findViewById(R.id.tvProjectedScore)
        tvTotalGoals = findViewById(R.id.tvTotalGoals)
        tvTimeframe = findViewById(R.id.tvTimeframe)
        tvImprovement = findViewById(R.id.tvImprovement)
        goalsListContainer = findViewById(R.id.goalsListContainer)
    }

    private fun setupListeners() {
        // Slider timeframe
        sliderTimeframe.addOnChangeListener { _, value, _ ->
            tvTimeframeValue.text = "${value.toInt()} jours"
        }

        // Bouton générer
        btnGenerateGoals.setOnClickListener {
            val selectedGoals = getSelectedGoals()
            if (selectedGoals.isEmpty()) {
                Toast.makeText(this, "Sélectionnez au moins un objectif", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeframe = sliderTimeframe.value.toInt()
            val difficulty = getSelectedDifficulty()

            generatePersonalizedGoals(selectedGoals, timeframe, difficulty)
        }
    }

    private fun getSelectedGoals(): List<String> {
        val goals = mutableListOf<String>()
        if (chipActivity.isChecked) goals.add("activity")
        if (chipSleep.isChecked) goals.add("sleep")
        if (chipHydration.isChecked) goals.add("hydration")
        if (chipStress.isChecked) goals.add("stress")
        if (chipCardio.isChecked) goals.add("cardiovascular")
        return goals
    }

    private fun getSelectedDifficulty(): String {
        return when (chipGroupDifficulty.checkedChipId) {
            R.id.chipEasy -> "easy"
            R.id.chipModerate -> "moderate"
            R.id.chipChallenging -> "challenging"
            else -> "moderate"
        }
    }

    private fun generatePersonalizedGoals(
        selectedGoals: List<String>,
        timeframe: Int,
        difficulty: String
    ) {
        showLoading()

        val preferences = GoalPreferencesRequest(
            preferred_goals = selectedGoals,
            timeframe_days = timeframe,
            difficulty = difficulty
        )

        lifecycleScope.launch {
            try {
                Log.d("HealthGoals", "📤 Requête: email=$userEmail, goals=$selectedGoals, timeframe=$timeframe, difficulty=$difficulty")

                val response = withContext(Dispatchers.IO) {
                    apiService.getPersonalizedGoals(userEmail, preferences)
                }

                if (response.isSuccessful && response.body() != null) {
                    val goalsResult = response.body()!!
                    Log.d("HealthGoals", "✅ Objectifs reçus: ${goalsResult.total_goals} objectifs")

                    withContext(Dispatchers.Main) {
                        displayResults(goalsResult)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Erreur inconnue"
                    Log.e("HealthGoals", "❌ Erreur API: ${response.code()} - $errorBody")
                    showError("Erreur serveur: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("HealthGoals", "❌ Exception: ${e.message}")
                e.printStackTrace()
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }

    private fun displayResults(result: PersonalizedGoalsResponse) {
        hideLoading()
        resultsContainer.visibility = View.VISIBLE

        // 1. Scores
        val currentScore = result.average_current_health_score.split(" ")[0]
        val projectedScore = result.projected_health_score.split(" ")[0]

        tvCurrentScore.text = currentScore
        tvProjectedScore.text = projectedScore

        // 2. Stats
        tvTotalGoals.text = result.total_goals.toString()
        tvTimeframe.text = result.timeframe_days.toString()
        tvImprovement.text = "+${result.estimated_improvement.toInt()}"

        // 3. Liste des objectifs
        goalsListContainer.removeAllViews()
        for (goal in result.goals) {
            goalsListContainer.addView(createGoalCard(goal, result.timeframe_days))
        }
    }

    private fun createGoalCard(goal: HealthGoal, totalDays: Int): View {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(6).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        // En-tête avec badge priorité
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(this).apply {
            text = getCategoryEmoji(goal.category) + " " + goal.title
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getCategoryColor(goal.category))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        headerLayout.addView(tvTitle)

        val priorityBadge = com.google.android.material.chip.Chip(this).apply {
            text = getPriorityText(goal.priority)
            setChipBackgroundColorResource(android.R.color.transparent)
            setTextColor(getPriorityColor(goal.priority))
            textSize = 10f
            chipStrokeWidth = dpToPx(1).toFloat()
            chipStrokeColor = android.content.res.ColorStateList.valueOf(getPriorityColor(goal.priority))
        }
        headerLayout.addView(priorityBadge)

        mainLayout.addView(headerLayout)

        // Progression visuelle
        val progressLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(16), 0, dpToPx(16))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val currentValue = when (goal.current) {
            is Int -> goal.current.toString()
            is Double -> String.format("%.1f", goal.current)
            else -> goal.current.toString()
        }

        val targetValue = when (goal.target) {
            is Int -> goal.target.toString()
            is Double -> String.format("%.1f", goal.target)
            else -> goal.target.toString()
        }

        val tvCurrent = TextView(this).apply {
            text = currentValue
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#FF6F00"))
        }
        progressLayout.addView(tvCurrent)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(8),
                1f
            ).apply {
                setMargins(dpToPx(16), 0, dpToPx(16), 0)
            }
            max = 100
            progress = 30 // Valeur indicative
            progressDrawable = resources.getDrawable(R.drawable.progress_goal, null)
        }
        progressLayout.addView(progressBar)

        val tvTarget = TextView(this).apply {
            text = targetValue
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#4CAF50"))
        }
        progressLayout.addView(tvTarget)

        mainLayout.addView(progressLayout)

        // Amélioration attendue
        val tvImprovement = TextView(this).apply {
            text = "📈 ${goal.expected_improvement}"
            textSize = 13f
            setTextColor(Color.parseColor("#4CAF50"))
            setPadding(0, 0, 0, dpToPx(12))
        }
        mainLayout.addView(tvImprovement)

        // Divider
        mainLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(8))
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        })

        // Milestones
        val tvMilestonesLabel = TextView(this).apply {
            text = "🎯 Étapes clés (${totalDays} jours)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#37474F"))
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        mainLayout.addView(tvMilestonesLabel)

        for (milestone in goal.milestones) {
            val milestoneView = createMilestoneView(milestone)
            mainLayout.addView(milestoneView)
        }

        // Divider
        mainLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, dpToPx(12), 0, dpToPx(12))
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        })

        // Tips
        val tvTipsLabel = TextView(this).apply {
            text = "💡 Conseils pratiques"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#37474F"))
            setPadding(0, 0, 0, dpToPx(8))
        }
        mainLayout.addView(tvTipsLabel)

        for (tip in goal.tips) {
            val tvTip = TextView(this).apply {
                text = "• $tip"
                textSize = 13f
                setTextColor(Color.parseColor("#546E7A"))
                setPadding(dpToPx(8), dpToPx(4), 0, dpToPx(4))
                setLineSpacing(4f, 1f)
            }
            mainLayout.addView(tvTip)
        }

        card.addView(mainLayout)
        return card
    }

    private fun createMilestoneView(milestone: Milestone): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundResource(R.drawable.bg_milestone)
        }

        val tvDay = TextView(this).apply {
            text = "Jour ${milestone.day}"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#6A1B9A"))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(tvDay)

        val tvDescription = TextView(this).apply {
            text = milestone.description
            textSize = 13f
            setTextColor(Color.parseColor("#37474F"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        layout.addView(tvDescription)

        return layout
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category) {
            "activity" -> "🚶"
            "sleep" -> "💤"
            "hydration" -> "💧"
            "stress" -> "🧠"
            "cardiovascular" -> "❤️"
            else -> "🎯"
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "activity" -> Color.parseColor("#FF6F00")
            "sleep" -> Color.parseColor("#5E35B1")
            "hydration" -> Color.parseColor("#0288D1")
            "stress" -> Color.parseColor("#C2185B")
            "cardiovascular" -> Color.parseColor("#D32F2F")
            else -> Color.parseColor("#6A1B9A")
        }
    }

    private fun getPriorityColor(priority: String): Int {
        return when (priority.lowercase()) {
            "high" -> Color.parseColor("#D32F2F")
            "medium" -> Color.parseColor("#F57C00")
            "low" -> Color.parseColor("#388E3C")
            else -> Color.parseColor("#757575")
        }
    }

    private fun getPriorityText(priority: String): String {
        return when (priority.lowercase()) {
            "high" -> "⚠️ HAUTE"
            "medium" -> "📌 MOYENNE"
            "low" -> "✓ BASSE"
            else -> priority.uppercase()
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        resultsContainer.visibility = View.GONE
        tvError.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        resultsContainer.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}