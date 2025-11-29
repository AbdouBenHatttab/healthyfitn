package com.health.virtualdoctor.ui.user.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.HealthDataViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class VitalSignsFragment : Fragment() {

    private val viewModel: HealthDataViewModel by activityViewModels()

    // Vues
    private lateinit var tvSpO2: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvBloodPressure: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvHeight: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vital_signs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiser les vues
        initViews(view)

        // Observer les données
        observeHealthData()
    }

    private fun initViews(view: View) {
        tvSpO2 = view.findViewById(R.id.tvSpO2)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvBloodPressure = view.findViewById(R.id.tvBloodPressure)
        tvWeight = view.findViewById(R.id.tvWeight)
        tvHeight = view.findViewById(R.id.tvHeight)
    }

    private fun observeHealthData() {
        viewModel.healthData.observe(viewLifecycleOwner) { dayJson ->
            dayJson?.let { updateVitalSigns(it) }
        }
    }

    private fun updateVitalSigns(dayJson: JSONObject) {
        // SpO2
        val oxygenArray = dayJson.optJSONArray("oxygenSaturation") ?: JSONArray()
        if (oxygenArray.length() > 0) {
            val lastO2 = oxygenArray.getJSONObject(oxygenArray.length() - 1)
            tvSpO2.text = "${String.format(Locale.US, "%.1f", lastO2.optDouble("percentage", 0.0))}%"
        } else {
            tvSpO2.text = "--"
        }

        // Température
        val tempArray = dayJson.optJSONArray("bodyTemperature") ?: JSONArray()
        if (tempArray.length() > 0) {
            val lastTemp = tempArray.getJSONObject(tempArray.length() - 1)
            tvTemperature.text = "${String.format(Locale.US, "%.1f", lastTemp.optDouble("temperature", 0.0))}°C"
        } else {
            tvTemperature.text = "--"
        }

        // Pression artérielle
        val bpArray = dayJson.optJSONArray("bloodPressure") ?: JSONArray()
        if (bpArray.length() > 0) {
            val lastBP = bpArray.getJSONObject(bpArray.length() - 1)
            tvBloodPressure.text = "${lastBP.optInt("systolic", 0)}/${lastBP.optInt("diastolic", 0)}"
        } else {
            tvBloodPressure.text = "--/--"
        }

        // Poids
        val weightArray = dayJson.optJSONArray("weight") ?: JSONArray()
        if (weightArray.length() > 0) {
            val lastWeight = weightArray.getJSONObject(weightArray.length() - 1)
            tvWeight.text = "${String.format(Locale.US, "%.1f", lastWeight.optDouble("weight", 0.0))} kg"
        } else {
            tvWeight.text = "--"
        }

        // Taille
        val heightArray = dayJson.optJSONArray("height") ?: JSONArray()
        if (heightArray.length() > 0) {
            val lastHeight = heightArray.getJSONObject(heightArray.length() - 1)
            val heightInCm = lastHeight.optDouble("height", 0.0) * 100
            tvHeight.text = "${String.format(Locale.US, "%.0f", heightInCm)} cm"
        } else {
            tvHeight.text = "--"
        }
    }
}