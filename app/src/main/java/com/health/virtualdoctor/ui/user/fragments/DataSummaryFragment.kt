package com.health.virtualdoctor.ui.user.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.HealthDataViewModel
import org.json.JSONArray
import org.json.JSONObject

class DataSummaryFragment : Fragment() {

    private val viewModel: HealthDataViewModel by activityViewModels()

    private lateinit var dataSummaryContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_data_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataSummaryContainer = view.findViewById(R.id.dataSummaryContainer)

        observeHealthData()
    }

    private fun observeHealthData() {
        viewModel.healthData.observe(viewLifecycleOwner) { dayJson ->
            dayJson?.let { updateDataSummary(it) }
        }
    }

    private fun updateDataSummary(dayJson: JSONObject) {
        dataSummaryContainer.removeAllViews()

        val summaryItems = mutableListOf<Pair<String, String>>()

        // Activité physique
        val totalSteps = dayJson.optLong("totalSteps", 0)
        val totalDistance = dayJson.optString("totalDistanceKm", "0.00")
        summaryItems.add("👣 Activité" to "$totalSteps pas • $totalDistance km parcourus")

        // Sommeil
        val sleepHours = dayJson.optString("totalSleepHours", "0.0")
        val sleepArray = dayJson.optJSONArray("sleep")
        val sleepCount = sleepArray?.length() ?: 0
        summaryItems.add("💤 Sommeil" to "$sleepHours heures • $sleepCount session(s)")

        // Cardio
        val avgHR = dayJson.optInt("avgHeartRate", 0)
        val minHR = dayJson.optInt("minHeartRate", 0)
        val maxHR = dayJson.optInt("maxHeartRate", 0)
        if (avgHR > 0) {
            summaryItems.add("❤️ Fréquence cardiaque" to "Moy: $avgHR bpm • Min: $minHR • Max: $maxHR")
        }

        // Hydratation
        val hydration = dayJson.optString("totalHydrationLiters", "0.00")
        if (hydration.toDouble() > 0) {
            summaryItems.add("💧 Hydratation" to "$hydration litres consommés")
        }

        // Exercices
        val exercises = dayJson.optJSONArray("exercise")
        if (exercises != null && exercises.length() > 0) {
            val exerciseNames = mutableListOf<String>()
            for (i in 0 until exercises.length()) {
                val ex = exercises.getJSONObject(i)
                exerciseNames.add(ex.optString("exerciseTypeName"))
            }
            summaryItems.add("🏋️ Exercices" to exerciseNames.joinToString(", "))
        }

        // Signes vitaux
        val oxygenArray = dayJson.optJSONArray("oxygenSaturation")
        if (oxygenArray != null && oxygenArray.length() > 0) {
            val lastO2 = oxygenArray.getJSONObject(oxygenArray.length() - 1)
            summaryItems.add("🫁 Oxygénation" to "${String.format(java.util.Locale.US, "%.1f", lastO2.optDouble("percentage"))}% SpO₂")
        }

        val tempArray = dayJson.optJSONArray("bodyTemperature")
        if (tempArray != null && tempArray.length() > 0) {
            val lastTemp = tempArray.getJSONObject(tempArray.length() - 1)
            summaryItems.add("🌡️ Température" to "${String.format(java.util.Locale.US, "%.1f", lastTemp.optDouble("temperature"))}°C")
        }

        val bpArray = dayJson.optJSONArray("bloodPressure")
        if (bpArray != null && bpArray.length() > 0) {
            val lastBP = bpArray.getJSONObject(bpArray.length() - 1)
            summaryItems.add("💉 Tension artérielle" to "${lastBP.optInt("systolic")}/${lastBP.optInt("diastolic")} mmHg")
        }

        val weightArray = dayJson.optJSONArray("weight")
        if (weightArray != null && weightArray.length() > 0) {
            val lastWeight = weightArray.getJSONObject(weightArray.length() - 1)
            summaryItems.add("⚖️ Poids" to "${String.format(java.util.Locale.US, "%.1f", lastWeight.optDouble("weight"))} kg")
        }

        val heightArray = dayJson.optJSONArray("height")
        if (heightArray != null && heightArray.length() > 0) {
            val lastHeight = heightArray.getJSONObject(heightArray.length() - 1)
            val heightInCm = lastHeight.optDouble("height") * 100
            summaryItems.add("📐 Taille" to "${String.format(java.util.Locale.US, "%.0f", heightInCm)} cm")
        }

        // Stress
        val stressLevel = dayJson.optString("stressLevel", "Inconnu")
        val stressScore = dayJson.optInt("stressScore", 0)
        summaryItems.add("🧠 Niveau de stress" to "$stressLevel (score: $stressScore/100)")

        // Créer les vues
        for ((label, value) in summaryItems) {
            val itemLayout = LinearLayout(requireContext())
            itemLayout.orientation = LinearLayout.VERTICAL
            itemLayout.setPadding(0, 12, 0, 12)

            val labelView = TextView(requireContext())
            labelView.text = label
            labelView.textSize = 14f
            labelView.setTypeface(null, android.graphics.Typeface.BOLD)
            labelView.setTextColor(resources.getColor(R.color.text_primary, null))

            val valueView = TextView(requireContext())
            valueView.text = value
            valueView.textSize = 13f
            valueView.setTextColor(resources.getColor(R.color.text_secondary, null))
            valueView.setPadding(0, 4, 0, 0)

            itemLayout.addView(labelView)
            itemLayout.addView(valueView)

            dataSummaryContainer.addView(itemLayout)

            // Ajouter un séparateur
            if (summaryItems.indexOf(label to value) < summaryItems.size - 1) {
                val divider = View(requireContext())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                params.setMargins(0, 8, 0, 8)
                divider.layoutParams = params
                divider.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                divider.alpha = 0.2f
                dataSummaryContainer.addView(divider)
            }
        }
    }
}