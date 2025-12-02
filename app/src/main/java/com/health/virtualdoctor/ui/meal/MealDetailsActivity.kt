package com.health.virtualdoctor.ui.meal

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import com.health.virtualdoctor.R
import org.json.JSONObject

class MealDetailsActivity : ComponentActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var ivMealImage: ImageView
    private lateinit var tvFoodName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvProteins: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvFiber: TextView
    private lateinit var tvSugars: TextView
    private lateinit var tvSodium: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_details)

        initViews()
        setupListeners()
        loadDetails()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivMealImage = findViewById(R.id.ivMealImage)
        tvFoodName = findViewById(R.id.tvFoodName)
        tvDate = findViewById(R.id.tvDate)
        tvCalories = findViewById(R.id.tvCalories)
        tvProteins = findViewById(R.id.tvProteins)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvFiber = findViewById(R.id.tvFiber)
        tvSugars = findViewById(R.id.tvSugars)
        tvSodium = findViewById(R.id.tvSodium)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadDetails() {
        // Récupérer les données de l'intent
        val foodName = intent.getStringExtra("foodName") ?: "Repas"
        val date = intent.getStringExtra("date") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val nutritionJson = intent.getStringExtra("totalNutritionJson") ?: "{}"

        try {
            val nutrition = JSONObject(nutritionJson)

            tvFoodName.text = foodName
            tvDate.text = date

            tvCalories.text = "${nutrition.getDouble("calories").toInt()} kcal"
            tvProteins.text = "${nutrition.getDouble("proteins")}g"
            tvCarbs.text = "${nutrition.getDouble("carbohydrates")}g"
            tvFats.text = "${nutrition.getDouble("fats")}g"
            tvFiber.text = "${nutrition.getDouble("fiber")}g"
            tvSugars.text = "${nutrition.optDouble("sugars", 0.0)}g"
            tvSodium.text = "${nutrition.optDouble("sodium", 0.0)}mg"

            // Charger l'image
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .centerCrop()
                .into(ivMealImage)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}