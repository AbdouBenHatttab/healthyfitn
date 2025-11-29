package com.health.virtualdoctor.ui.meal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.health.virtualdoctor.BuildConfig
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MealHistoryActivity : ComponentActivity() {
    private val cloudflared = BuildConfig.CLOUDFLARED_URL

    private lateinit var tokenManager: TokenManager

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvTotalMeals: TextView
    private lateinit var tvAvgCalories: TextView

    private lateinit var historyAdapter: MealHistoryAdapter
    private val historyList = mutableListOf<MealHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_history)

        tokenManager = TokenManager(this)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadHistory()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rvHistory = findViewById(R.id.rvHistory)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvTotalMeals = findViewById(R.id.tvTotalMeals)
        tvAvgCalories = findViewById(R.id.tvAvgCalories)
    }

    private fun setupRecyclerView() {
        historyAdapter = MealHistoryAdapter(historyList) { item ->
            // Click handler - afficher les détails
            showMealDetails(item)
        }

        rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MealHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadHistory() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = fetchHistoryFromServer()

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (result != null) {
                        displayHistory(result)
                    } else {
                        Toast.makeText(
                            this@MealHistoryActivity,
                            "❌ Erreur chargement historique",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MealHistoryActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("MealHistory", "Erreur chargement", e)
                }
            }
        }
    }

    private suspend fun fetchHistoryFromServer(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()

            if (token == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MealHistoryActivity,
                        "❌ Token manquant",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            val serverUrl = "$cloudflared/nutrition-service/api/v1/nutrition/history?limit=50&skip=0"

            Log.d("MealHistory", "🔄 Requête vers: $serverUrl")

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("MealHistory", "📡 Response code: ${response.code}")
            Log.d("MealHistory", "📡 Response: $responseBody")

            response.close()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getBoolean("success")) {
                    json.getJSONObject("data")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MealHistoryActivity,
                            json.optString("message", "Erreur"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MealHistoryActivity,
                        "❌ Erreur HTTP ${response.code}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                null
            }
        } catch (e: Exception) {
            Log.e("MealHistory", "❌ Erreur requête", e)
            null
        }
    }

    private fun displayHistory(data: JSONObject) {
        try {
            val analyses = data.getJSONArray("analyses")
            val count = data.getInt("count")

            historyList.clear()

            if (count == 0) {
                layoutEmpty.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
                return
            }

            var totalCalories = 0.0

            for (i in 0 until analyses.length()) {
                val analysis = analyses.getJSONObject(i)

                val detectedFoods = analysis.getJSONArray("detected_foods")
                val totalNutrition = analysis.getJSONObject("total_nutrition")

                val foodName = if (detectedFoods.length() > 0) {
                    detectedFoods.getJSONObject(0).getString("food_name")
                } else {
                    "Repas"
                }

                val calories = totalNutrition.getDouble("calories")
                totalCalories += calories

                val createdAt = analysis.getString("created_at")
                val formattedDate = formatDate(createdAt)

                val imageUrl = analysis.getString("image_url")

                // DEBUG: Afficher l'URL de l'image
                Log.d("MealHistory", "Image URL #$i: $imageUrl")

                historyList.add(
                    MealHistoryItem(
                        analysisId = analysis.getString("analysis_id"),
                        foodName = foodName,
                        calories = calories,
                        proteins = totalNutrition.getDouble("proteins"),
                        carbs = totalNutrition.getDouble("carbohydrates"),
                        fats = totalNutrition.getDouble("fats"),
                        imageUrl = imageUrl,
                        date = formattedDate,
                        detectedFoodsJson = detectedFoods.toString(),
                        totalNutritionJson = totalNutrition.toString()
                    )
                )
            }

            // Calculer les statistiques
            val avgCalories = if (count > 0) totalCalories / count else 0.0

            tvTotalMeals.text = "$count repas analysés"
            tvAvgCalories.text = "${avgCalories.toInt()} kcal/repas"

            historyAdapter.notifyDataSetChanged()

            layoutEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE

        } catch (e: Exception) {
            Log.e("MealHistory", "Erreur affichage", e)
            Toast.makeText(this, "❌ Erreur affichage: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMealDetails(item: MealHistoryItem) {
        val intent = Intent(this, MealDetailsActivity::class.java).apply {
            putExtra("foodName", item.foodName)
            putExtra("date", item.date)
            putExtra("imageUrl", item.imageUrl)
            putExtra("totalNutritionJson", item.totalNutritionJson)
            putExtra("detectedFoodsJson", item.detectedFoodsJson)
        }
        startActivity(intent)
    }
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvHistory.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH)
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            isoDate
        }
    }
}

// Data class pour représenter un élément de l'historique
data class MealHistoryItem(
    val analysisId: String,
    val foodName: String,
    val calories: Double,
    val proteins: Double,
    val carbs: Double,
    val fats: Double,
    val imageUrl: String,
    val date: String,
    val detectedFoodsJson: String,
    val totalNutritionJson: String
)