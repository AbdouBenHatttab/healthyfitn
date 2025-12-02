package com.health.virtualdoctor.ui.user.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.HealthDataViewModel
import com.health.virtualdoctor.ui.user.HealthAnalysisActivity
import com.health.virtualdoctor.ui.user.HealthGoalsActivity
import com.health.virtualdoctor.ui.user.HealthTrendsActivity
import com.health.virtualdoctor.ui.user.MedicalAlertsActivity
import org.json.JSONArray
import org.json.JSONObject

class AdvancedFeaturesFragment : Fragment() {

    private val viewModel: HealthDataViewModel by activityViewModels()

    private lateinit var dropdownHeader: LinearLayout
    private lateinit var dropdownContent: LinearLayout
    private lateinit var dropdownIcon: ImageView
    private var isDropdownExpanded = false

    private lateinit var btnHealthAnalysis: MaterialButton
    private lateinit var btnAlerts: MaterialButton
    private lateinit var btnTrends: MaterialButton
    private lateinit var btnGoals: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_advanced_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupDropdown()
        setupButtons()
    }

    private fun initViews(view: View) {
        dropdownHeader = view.findViewById(R.id.dropdownHeader)
        dropdownContent = view.findViewById(R.id.dropdownContent)
        dropdownIcon = view.findViewById(R.id.dropdownIcon)

        btnHealthAnalysis = view.findViewById(R.id.btnHealthAnalysis)
        btnAlerts = view.findViewById(R.id.btnAlerts)
        btnTrends = view.findViewById(R.id.btnTrends)
        btnGoals = view.findViewById(R.id.btnGoals)
    }

    private fun setupDropdown() {
        dropdownHeader.setOnClickListener {
            toggleDropdown()
        }
    }

    private fun toggleDropdown() {
        isDropdownExpanded = !isDropdownExpanded

        if (isDropdownExpanded) {
            dropdownContent.visibility = View.VISIBLE
            animateDropdownIcon(0f, 180f)
        } else {
            dropdownContent.visibility = View.GONE
            animateDropdownIcon(180f, 0f)
        }
    }

    private fun animateDropdownIcon(fromDegrees: Float, toDegrees: Float) {
        val rotate = RotateAnimation(
            fromDegrees, toDegrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 300
        rotate.fillAfter = true
        dropdownIcon.startAnimation(rotate)
    }

    private fun setupButtons() {
        btnHealthAnalysis.setOnClickListener {
            navigateToHealthAnalysis()
        }

        btnAlerts.setOnClickListener {
            navigateToAlerts()
        }

        btnTrends.setOnClickListener {
            navigateToTrends()
        }

        btnGoals.setOnClickListener {
            navigateToGoals()
        }
    }

    private fun navigateToHealthAnalysis() {
        try {
            val healthData = viewModel.getCurrentHealthData()
            if (healthData != null) {
                val biometricJson = createBiometricJson(healthData)
                val intent = Intent(requireContext(), HealthAnalysisActivity::class.java)
                intent.putExtra("biometric_data", biometricJson.toString())
                startActivity(intent)
                requireActivity().overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
            } else {
                Toast.makeText(requireContext(), "Données non disponibles", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun createBiometricJson(healthData: JSONObject): JSONObject {
        return JSONObject().apply {
            val steps = healthData.optInt("totalSteps", 0)
            val heartRate = healthData.optInt("avgHeartRate", 70)
            val sleepHours = healthData.optString("totalSleepHours", "7.0").toDoubleOrNull() ?: 7.0
            val hydration = healthData.optString("totalHydrationLiters", "2.0").toDoubleOrNull() ?: 2.0
            val distance = healthData.optString("totalDistanceKm", "0.0").toDoubleOrNull() ?: 0.0
            val stressLevel = healthData.optString("stressLevel", "Modéré")

            put("totalSteps", steps)
            put("avgHeartRate", heartRate)
            put("minHeartRate", heartRate - 10)
            put("maxHeartRate", heartRate + 20)
            put("totalDistanceKm", distance)
            put("totalSleepHours", sleepHours)
            put("totalHydrationLiters", hydration)
            put("stressLevel", stressLevel)
            put("stressScore", calculateStressScoreFromLevel(stressLevel))

            put("oxygenSaturation", JSONArray())
            put("bodyTemperature", JSONArray())
            put("bloodPressure", JSONArray())
            put("weight", JSONArray())
            put("height", JSONArray())
            put("exercise", JSONArray())
        }
    }

    private fun calculateStressScoreFromLevel(level: String): Int {
        return when (level.lowercase()) {
            "très élevé" -> 80
            "élevé" -> 60
            "modéré" -> 40
            "faible" -> 20
            else -> 50
        }
    }

    private fun navigateToAlerts() {
        try {
            val intent = Intent(requireContext(), MedicalAlertsActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Page Alertes Médicales en développement", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToTrends() {
        try {
            val intent = Intent(requireContext(), HealthTrendsActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Page Tendances en développement", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToGoals() {
        try {
            val intent = Intent(requireContext(), HealthGoalsActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Page Objectifs en développement", Toast.LENGTH_SHORT).show()
        }
    }
}