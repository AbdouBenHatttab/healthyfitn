package com.health.virtualdoctor.ui.user

import android.app.DatePickerDialog
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
import com.google.android.material.textfield.TextInputEditText
import com.health.virtualdoctor.R
import com.health.virtualdoctor.network.*
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MedicalAlertsActivity : ComponentActivity() {

    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsContainer: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chip1Day: Chip
    private lateinit var chip3Days: Chip
    private lateinit var chip7Days: Chip
    private lateinit var chip30Days: Chip
    private lateinit var etSpecificDate: TextInputEditText
    private lateinit var btnClearDate: MaterialButton
    private lateinit var btnAnalyze: MaterialButton

    // Results views
    private lateinit var alertLevelCard: LinearLayout
    private lateinit var tvAlertLevel: TextView
    private lateinit var tvAnalysisPeriod: TextView
    private lateinit var tvAnalysisType: TextView
    private lateinit var averagesContainer: LinearLayout
    private lateinit var cardAlerts: CardView
    private lateinit var alertsListContainer: LinearLayout
    private lateinit var riskFactorsContainer: LinearLayout
    private lateinit var actionPrioritiesContainer: LinearLayout
    private lateinit var tvNextCheckup: TextView

    // API
    private val apiHealthService = RetrofitHealthClient.apiHealthService
    private lateinit var tokenManager: TokenManager
    private var userEmail: String = ""

    // Sélection
    private var selectedPeriodDays: Int? = 7
    private var selectedSpecificDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_alerts)

        tokenManager = TokenManager(this)
        userEmail = tokenManager.getUserEmail() ?: "unknown@example.com"

        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Toolbar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Loading & Error
        progressBar = findViewById(R.id.progressBar)
        resultsContainer = findViewById(R.id.resultsContainer)
        tvError = findViewById(R.id.tvError)

        // Sélection période
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod)
        chip1Day = findViewById(R.id.chip1Day)
        chip3Days = findViewById(R.id.chip3Days)
        chip7Days = findViewById(R.id.chip7Days)
        chip30Days = findViewById(R.id.chip30Days)
        etSpecificDate = findViewById(R.id.etSpecificDate)
        btnClearDate = findViewById(R.id.btnClearDate)
        btnAnalyze = findViewById(R.id.btnAnalyze)

        // Results
        alertLevelCard = findViewById(R.id.alertLevelCard)
        tvAlertLevel = findViewById(R.id.tvAlertLevel)
        tvAnalysisPeriod = findViewById(R.id.tvAnalysisPeriod)
        tvAnalysisType = findViewById(R.id.tvAnalysisType)
        averagesContainer = findViewById(R.id.averagesContainer)
        cardAlerts = findViewById(R.id.cardAlerts)
        alertsListContainer = findViewById(R.id.alertsListContainer)
        riskFactorsContainer = findViewById(R.id.riskFactorsContainer)
        actionPrioritiesContainer = findViewById(R.id.actionPrioritiesContainer)
        tvNextCheckup = findViewById(R.id.tvNextCheckup)
    }

    private fun setupListeners() {
        // Chips période
        chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip1Day -> {
                    selectedPeriodDays = 1
                    selectedSpecificDate = null
                    etSpecificDate.setText("")
                }
                R.id.chip3Days -> {
                    selectedPeriodDays = 3
                    selectedSpecificDate = null
                    etSpecificDate.setText("")
                }
                R.id.chip7Days -> {
                    selectedPeriodDays = 7
                    selectedSpecificDate = null
                    etSpecificDate.setText("")
                }
                R.id.chip30Days -> {
                    selectedPeriodDays = 30
                    selectedSpecificDate = null
                    etSpecificDate.setText("")
                }
            }
        }

        // Date picker
        etSpecificDate.setOnClickListener {
            showDatePicker()
        }

        // Clear date
        btnClearDate.setOnClickListener {
            selectedSpecificDate = null
            etSpecificDate.setText("")
            chip7Days.isChecked = true
            selectedPeriodDays = 7
        }

        // Analyser
        btnAnalyze.setOnClickListener {
            if (selectedSpecificDate != null || selectedPeriodDays != null) {
                fetchRiskAlerts()
            } else {
                Toast.makeText(this, "Sélectionnez une période ou une date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etSpecificDate.setText(selectedDate)
                selectedSpecificDate = selectedDate
                selectedPeriodDays = null
                chipGroupPeriod.clearCheck()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun fetchRiskAlerts() {
        showLoading()

        lifecycleScope.launch {
            try {
                Log.d("MedicalAlerts", "📤 Requête: email=$userEmail, period=$selectedPeriodDays, date=$selectedSpecificDate")

                val response = withContext(Dispatchers.IO) {
                    apiHealthService.getRiskAlerts(
                        email = userEmail,
                        periodDays = selectedPeriodDays ?: 7, // <-- valeur par défaut
                        specificDate = selectedSpecificDate
                    )
                }


                if (response.isSuccessful && response.body() != null) {
                    val alertsResult = response.body()!!
                    Log.d("MedicalAlerts", "✅ Alertes reçues: ${alertsResult.alert_level}")

                    withContext(Dispatchers.Main) {
                        displayResults(alertsResult)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Erreur inconnue"
                    Log.e("MedicalAlerts", "❌ Erreur API: ${response.code()} - $errorBody")
                    showError("Erreur serveur: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("MedicalAlerts", "❌ Exception: ${e.message}")
                e.printStackTrace()
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }

    private fun displayResults(result: RiskAlertsResponse) {
        hideLoading()
        resultsContainer.visibility = View.VISIBLE

        // 1. Niveau d'alerte
        tvAlertLevel.text = result.alert_level
        tvAnalysisPeriod.text = result.analysis_period
        tvAnalysisType.text = "${result.analysis_type} (${result.data_points_analyzed} point(s))"

        // Couleur selon niveau
        val alertColor = when (result.alert_level.lowercase()) {
            "faible" -> Color.parseColor("#4CAF50")
            "modéré" -> Color.parseColor("#FF9800")
            "élevé" -> Color.parseColor("#FF5722")
            "critique" -> Color.parseColor("#D32F2F")
            else -> Color.parseColor("#9E9E9E")
        }
        alertLevelCard.setBackgroundColor(alertColor)

        // 2. Moyennes
        displayAverages(result.averages_computed)

        // 3. Alertes
        if (result.alerts.isNotEmpty()) {
            cardAlerts.visibility = View.VISIBLE
            displayAlerts(result.alerts)
        } else {
            cardAlerts.visibility = View.GONE
        }

        // 4. Facteurs de risque
        displayRiskFactors(result.risk_factors)

        // 5. Actions prioritaires
        displayActionPriorities(result.action_priorities)

        // 6. Prochain checkup
        tvNextCheckup.text = formatDate(result.next_checkup_recommended)
    }

    private fun displayAverages(averages: AveragesComputed) {
        averagesContainer.removeAllViews()

        val items = listOf(
            "🚶 Pas" to "${averages.steps} pas/jour",
            "💤 Sommeil" to "${String.format("%.1f", averages.sleep_hours)}h/nuit",
            "❤️ FC moyenne" to "${averages.heart_rate} bpm",
            "🧠 Stress" to "${averages.stress_score}/100",
            "💧 Hydratation" to "${String.format("%.1f", averages.hydration_liters)}L/jour"
        )

        for ((label, value) in items) {
            averagesContainer.addView(createAverageItem(label, value))
        }
    }

    private fun createAverageItem(label: String, value: String): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            setBackgroundResource(R.drawable.bg_rounded_light)
        }

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.parseColor("#37474F"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvValue = TextView(this).apply {
            text = value
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A237E"))
        }

        layout.addView(tvLabel)
        layout.addView(tvValue)

        return layout
    }

    private fun displayAlerts(alerts: List<String>) {
        alertsListContainer.removeAllViews()

        for (alert in alerts) {
            val tvAlert = TextView(this).apply {
                text = alert
                textSize = 15f
                setTextColor(Color.parseColor("#C62828"))
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                setLineSpacing(4f, 1f)
                setBackgroundResource(R.drawable.bg_rounded_alert)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
            }
            alertsListContainer.addView(tvAlert)
        }
    }

    private fun displayRiskFactors(riskFactors: List<RiskFactor>) {
        riskFactorsContainer.removeAllViews()

        for (factor in riskFactors) {
            riskFactorsContainer.addView(createRiskFactorCard(factor))
        }
    }

    private fun createRiskFactorCard(factor: RiskFactor): View {
        val card = androidx.cardview.widget.CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(getSeverityColor(factor.severity))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // En-tête
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvType = TextView(this).apply {
            text = getTypeEmoji(factor.type) + " " + formatType(factor.type)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A237E"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvProbability = TextView(this).apply {
            text = "${factor.probability.toInt()}%"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getSeverityTextColor(factor.severity))
        }

        header.addView(tvType)
        header.addView(tvProbability)
        layout.addView(header)

        // Description
        val tvDescription = TextView(this).apply {
            text = factor.description
            textSize = 14f
            setTextColor(Color.parseColor("#546E7A"))
            setPadding(0, dpToPx(8), 0, dpToPx(12))
        }
        layout.addView(tvDescription)

        // Actions
        if (factor.actions.isNotEmpty()) {
            val tvActionsLabel = TextView(this).apply {
                text = "💡 Actions recommandées :"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#1A237E"))
                setPadding(0, 0, 0, dpToPx(8))
            }
            layout.addView(tvActionsLabel)

            for (action in factor.actions) {
                val tvAction = TextView(this).apply {
                    text = "• $action"
                    textSize = 13f
                    setTextColor(Color.parseColor("#37474F"))
                    setPadding(dpToPx(8), dpToPx(4), 0, dpToPx(4))
                }
                layout.addView(tvAction)
            }
        }

        card.addView(layout)
        return card
    }

    private fun displayActionPriorities(actions: List<ActionPriority>) {
        actionPrioritiesContainer.removeAllViews()

        for (action in actions) {
            actionPrioritiesContainer.addView(createActionPriorityCard(action))
        }
    }

    private fun createActionPriorityCard(action: ActionPriority): View {
        val card = androidx.cardview.widget.CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // Badge urgence
        val badge = com.google.android.material.chip.Chip(this).apply {
            text = getUrgencyText(action.urgency)
            setChipBackgroundColorResource(android.R.color.transparent)
            setTextColor(getUrgencyColor(action.urgency))
            textSize = 11f
            chipStrokeWidth = dpToPx(1).toFloat()
            chipStrokeColor = android.content.res.ColorStateList.valueOf(getUrgencyColor(action.urgency))
        }
        layout.addView(badge)

        // Action
        val tvAction = TextView(this).apply {
            text = action.action
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1A237E"))
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        layout.addView(tvAction)

        // Impact
        val tvImpact = TextView(this).apply {
            text = "📈 ${action.impact}"
            textSize = 13f
            setTextColor(Color.parseColor("#4CAF50"))
        }
        layout.addView(tvImpact)

        card.addView(layout)
        return card
    }

    private fun getSeverityColor(severity: String): Int {
        return when (severity.lowercase()) {
            "critical" -> Color.parseColor("#FFEBEE")
            "high" -> Color.parseColor("#FFF3E0")
            "medium" -> Color.parseColor("#FFF9C4")
            "low" -> Color.parseColor("#E8F5E9")
            else -> Color.parseColor("#F5F5F5")
        }
    }

    private fun getSeverityTextColor(severity: String): Int {
        return when (severity.lowercase()) {
            "critical" -> Color.parseColor("#C62828")
            "high" -> Color.parseColor("#E65100")
            "medium" -> Color.parseColor("#F57C00")
            "low" -> Color.parseColor("#2E7D32")
            else -> Color.parseColor("#757575")
        }
    }

    private fun getUrgencyColor(urgency: String): Int {
        return when (urgency.lowercase()) {
            "critical" -> Color.parseColor("#D32F2F")
            "high" -> Color.parseColor("#F57C00")
            "medium" -> Color.parseColor("#FFA726")
            "low" -> Color.parseColor("#66BB6A")
            else -> Color.parseColor("#9E9E9E")
        }
    }

    private fun getUrgencyText(urgency: String): String {
        return when (urgency.lowercase()) {
            "critical" -> "🚨 CRITIQUE"
            "high" -> "⚠️ HAUTE"
            "medium" -> "📌 MOYENNE"
            "low" -> "✓ BASSE"
            else -> urgency.uppercase()
        }
    }

    private fun getTypeEmoji(type: String): String {
        return when (type) {
            "sleep_deprivation", "sleep_insufficient" -> "💤"
            "low_activity", "severe_inactivity" -> "🚶"
            "moderate_stress", "high_stress", "stress_increasing" -> "🧠"
            "dehydration" -> "💧"
            "elevated_heart_rate" -> "❤️"
            else -> "⚠️"
        }
    }

    private fun formatType(type: String): String {
        return type.replace("_", " ").split(" ")
            .joinToString(" ") { it.capitalize() }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(isoDate.substring(0, 19))
            date?.let { outputFormat.format(it) } ?: isoDate
        } catch (e: Exception) {
            isoDate.substring(0, 10)
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