package com.health.virtualdoctor.ui.user.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.HealthDataViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ExercisesFragment : Fragment() {

    private val viewModel: HealthDataViewModel by activityViewModels()

    private lateinit var tvExerciseCount: TextView
    private lateinit var exerciseContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercises, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiser les vues
        tvExerciseCount = view.findViewById(R.id.tvExerciseCount)
        exerciseContainer = view.findViewById(R.id.exerciseContainer)

        // Observer les données
        observeHealthData()
    }

    private fun observeHealthData() {
        viewModel.healthData.observe(viewLifecycleOwner) { dayJson ->
            dayJson?.let { updateExercises(it) }
        }
    }

    private fun updateExercises(dayJson: JSONObject) {
        val exercises = dayJson.optJSONArray("exercise") ?: JSONArray()

        if (exercises.length() > 0) {
            tvExerciseCount.text = "${exercises.length()} activité(s) aujourd'hui"
        } else {
            tvExerciseCount.text = "Aucun exercice aujourd'hui"
        }

        exerciseContainer.removeAllViews()

        for (i in 0 until exercises.length()) {
            val ex = exercises.getJSONObject(i)
            val exerciseCard = createExerciseCard(ex)
            exerciseContainer.addView(exerciseCard)
        }
    }

    private fun createExerciseCard(exercise: JSONObject): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(resources.getColor(android.R.color.white, null))
        }

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // En-tête : Nom de l'exercice + Durée
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val exerciseName = exercise.optString("exerciseTypeName", "Exercice")
        val duration = exercise.optLong("durationMinutes", 0)

        val tvName = TextView(requireContext()).apply {
            text = "🏃 $exerciseName"
            textSize = 18f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvDuration = TextView(requireContext()).apply {
            text = "$duration min"
            textSize = 16f
            setTextColor(resources.getColor(R.color.primary, null))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        headerLayout.addView(tvName)
        headerLayout.addView(tvDuration)
        mainLayout.addView(headerLayout)

        // Ligne de séparation
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, dpToPx(12), 0, dpToPx(12))
            }
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }
        mainLayout.addView(divider)

        // Grille de statistiques
        val statsGrid = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weightSum = 3f
        }

        // Cadence
        val avgCadence = exercise.optInt("avgCadence", 0)
        val cadenceText = if (avgCadence > 0) "$avgCadence spm" else "-- spm"
        statsGrid.addView(createStatView("🦶 Cadence", cadenceText))

        // BPM moyen
        val avgBpm = exercise.optInt("avgHeartRate", 0)
        val bpmText = if (avgBpm > 0) "$avgBpm bpm" else "-- bpm"
        statsGrid.addView(createStatView("❤️ BPM Moy", bpmText))

        // Distance
        val distanceKm = exercise.optString("distanceKm", "0.00")
        val distanceMeters = exercise.optDouble("distanceMeters", 0.0)
        val distanceText = if (distanceMeters >= 1000) {
            "$distanceKm km"
        } else if (distanceMeters > 0) {
            "${String.format(Locale.US, "%.0f", distanceMeters)} m"
        } else {
            "-- m"
        }
        statsGrid.addView(createStatView("📏 Distance", distanceText))

        mainLayout.addView(statsGrid)

        // Statistiques supplémentaires
        val hasExtraStats = exercise.optInt("steps", 0) > 0 ||
                exercise.optInt("activeCalories", 0) > 0 ||
                exercise.optString("avgSpeedKmh", "").isNotEmpty()

        if (hasExtraStats) {
            val extraStatsLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dpToPx(8), 0, 0)
            }

            val steps = exercise.optInt("steps", 0)
            if (steps > 0) {
                val stepsText = TextView(requireContext()).apply {
                    text = "• $steps pas pendant l'exercice"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, dpToPx(2), 0, dpToPx(2))
                }
                extraStatsLayout.addView(stepsText)
            }

            val calories = exercise.optInt("activeCalories", 0)
            if (calories > 0) {
                val caloriesText = TextView(requireContext()).apply {
                    text = "• $calories kcal brûlées"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, dpToPx(2), 0, dpToPx(2))
                }
                extraStatsLayout.addView(caloriesText)
            }

            val avgSpeed = exercise.optString("avgSpeedKmh", "")
            if (avgSpeed.isNotEmpty()) {
                val speedText = TextView(requireContext()).apply {
                    text = "• Vitesse moy: $avgSpeed km/h"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, dpToPx(2), 0, dpToPx(2))
                }
                extraStatsLayout.addView(speedText)
            }

            mainLayout.addView(extraStatsLayout)
        }

        cardView.addView(mainLayout)
        return cardView
    }

    private fun createStatView(label: String, value: String): LinearLayout {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        val tvLabel = TextView(requireContext()).apply {
            text = label
            textSize = 11f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            gravity = android.view.Gravity.CENTER
        }

        val tvValue = TextView(requireContext()).apply {
            text = value
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dpToPx(4), 0, 0)
        }

        layout.addView(tvLabel)
        layout.addView(tvValue)

        return layout
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}