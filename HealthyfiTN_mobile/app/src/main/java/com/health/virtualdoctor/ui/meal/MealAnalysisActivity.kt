package com.health.virtualdoctor.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MealAnalysisActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var ivMealImage: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var cardNutritionalBreakdown: CardView
    private lateinit var cardHealthierAlternatives: CardView
    private lateinit var tvFoodName: TextView
    private lateinit var tvConfidence: TextView
    private lateinit var tvProteins: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvFiber: TextView
    private lateinit var tvCalories: TextView
    private lateinit var progressProteins: ProgressBar
    private lateinit var progressCarbs: ProgressBar
    private lateinit var progressFats: ProgressBar
    private lateinit var progressFiber: ProgressBar
    private lateinit var containerAlternatives: LinearLayout
    private lateinit var btnScanNewMeal: MaterialButton

    private var currentImageBitmap: Bitmap? = null
    private var currentPhotoPath: String? = null

    // ========================================
    // CAMERA LAUNCHERS
    // ========================================

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoPath != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                currentImageBitmap = bitmap
                displayImage(bitmap)
                analyzeMeal(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur lecture photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Photo annulée", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                currentImageBitmap = bitmap
                displayImage(bitmap)
                analyzeMeal(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur chargement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            takePicture()
        } else {
            Toast.makeText(this, "⚠️ Permission caméra requise", Toast.LENGTH_SHORT).show()
        }
    }

    // ========================================
    // LIFECYCLE
    // ========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_analysis)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        ivMealImage = findViewById(R.id.ivMealImage)
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder)
        layoutLoading = findViewById(R.id.layoutLoading)
        cardNutritionalBreakdown = findViewById(R.id.cardNutritionalBreakdown)
        cardHealthierAlternatives = findViewById(R.id.cardHealthierAlternatives)
        tvFoodName = findViewById(R.id.tvFoodName)
        tvConfidence = findViewById(R.id.tvConfidence)
        tvProteins = findViewById(R.id.tvProteins)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvFiber = findViewById(R.id.tvFiber)
        tvCalories = findViewById(R.id.tvCalories)
        progressProteins = findViewById(R.id.progressProteins)
        progressCarbs = findViewById(R.id.progressCarbs)
        progressFats = findViewById(R.id.progressFats)
        progressFiber = findViewById(R.id.progressFiber)
        containerAlternatives = findViewById(R.id.containerAlternatives)
        btnScanNewMeal = findViewById(R.id.btnScanNewMeal)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnCamera.setOnClickListener {
            checkCameraPermission()
        }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnScanNewMeal.setOnClickListener {
            resetView()
        }
    }

    // ========================================
    // CAMERA FUNCTIONS
    // ========================================

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePicture()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePicture() {
        try {
            // Créer un fichier temporaire
            val photoFile = File.createTempFile(
                "meal_${System.currentTimeMillis()}",
                ".jpg",
                cacheDir
            )

            currentPhotoPath = photoFile.absolutePath

            // Créer URI avec FileProvider
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            // Lancer la caméra
            cameraLauncher.launch(photoUri)

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Erreur caméra: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MealAnalysis", "Erreur caméra", e)
        }
    }

    // ========================================
    // UI FUNCTIONS
    // ========================================

    private fun displayImage(bitmap: Bitmap) {
        ivMealImage.setImageBitmap(bitmap)
        ivMealImage.visibility = View.VISIBLE
        layoutPlaceholder.visibility = View.GONE
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        btnCamera.isEnabled = !show
        btnGallery.isEnabled = !show
    }

    private fun resetView() {
        currentImageBitmap = null
        currentPhotoPath = null
        ivMealImage.visibility = View.GONE
        layoutPlaceholder.visibility = View.VISIBLE
        cardNutritionalBreakdown.visibility = View.GONE
        cardHealthierAlternatives.visibility = View.GONE
        btnScanNewMeal.visibility = View.GONE
    }

    // ========================================
    // ANALYSE FUNCTIONS
    // ========================================

    private fun analyzeMeal(bitmap: Bitmap) {
        showLoading(true)
        cardNutritionalBreakdown.visibility = View.GONE
        cardHealthierAlternatives.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Convertir bitmap en file
                val file = bitmapToFile(bitmap)

                // Envoyer au serveur
                val result = sendImageToNutritionService(file)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (result != null) {
                        displayNutritionalInfo(result)
                        btnScanNewMeal.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this@MealAnalysisActivity,
                            "❌ Erreur d'analyse",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MealAnalysisActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("MealAnalysis", "Erreur analyse", e)
                }
            }
        }
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val file = File(cacheDir, "meal_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private suspend fun sendImageToNutritionService(imageFile: File): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()

            if (token == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MealAnalysisActivity,
                        "❌ Token manquant",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            // ✅ URL Cloudflare au lieu de localhost
            val serverUrl = "https://spending-elderly-change-fin.trycloudflare.com/api/v1/nutrition/analyze"
            // Ou si vous avez un tunnel ngrok:
            // val serverUrl = "https://your-ngrok-url.ngrok-free.app/api/v1/nutrition/analyze"

            Log.d("MealAnalysis", "🔄 Envoi vers: $serverUrl")

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            // Créer le body multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("use_ai", "true")
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("MealAnalysis", "📡 Response code: ${response.code}")
            Log.d("MealAnalysis", "📡 Response: $responseBody")

            response.close()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getBoolean("success")) {
                    json.getJSONObject("data")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MealAnalysisActivity,
                            json.optString("message", "Erreur"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MealAnalysisActivity,
                        "❌ Erreur HTTP ${response.code}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                null
            }
        } catch (e: Exception) {
            Log.e("MealAnalysis", "❌ Erreur envoi", e)
            null
        }
    }
    private fun displayNutritionalInfo(data: JSONObject) {
        try {
            // Extraire les infos
            val detectedFoods = data.getJSONArray("detected_foods")
            val totalNutrition = data.getJSONObject("total_nutrition")

            if (detectedFoods.length() > 0) {
                val mainFood = detectedFoods.getJSONObject(0)

                // Nom et confiance
                tvFoodName.text = mainFood.getString("food_name")
                tvConfidence.text = "${mainFood.getDouble("confidence").toInt()}%"

                // Nutrition
                val proteins = totalNutrition.getDouble("proteins")
                val carbs = totalNutrition.getDouble("carbohydrates")
                val fats = totalNutrition.getDouble("fats")
                val fiber = totalNutrition.getDouble("fiber")
                val calories = totalNutrition.getDouble("calories")

                tvProteins.text = "${proteins}g"
                tvCarbs.text = "${carbs}g"
                tvFats.text = "${fats}g"
                tvFiber.text = "${fiber}g"
                tvCalories.text = "${calories.toInt()} kcal"

                // Progress bars
                progressProteins.progress = ((proteins / 50.0) * 100).toInt().coerceAtMost(100)
                progressCarbs.progress = ((carbs / 60.0) * 100).toInt().coerceAtMost(100)
                progressFats.progress = ((fats / 35.0) * 100).toInt().coerceAtMost(100)
                progressFiber.progress = ((fiber / 15.0) * 100).toInt().coerceAtMost(100)

                cardNutritionalBreakdown.visibility = View.VISIBLE
            }

            // Alternatives
            if (data.has("alternatives")) {
                val alternatives = data.getJSONArray("alternatives")
                containerAlternatives.removeAllViews()

                for (i in 0 until alternatives.length().coerceAtMost(3)) {
                    val alt = alternatives.getJSONObject(i)
                    val altView = createAlternativeView(alt)
                    containerAlternatives.addView(altView)
                }

                if (alternatives.length() > 0) {
                    cardHealthierAlternatives.visibility = View.VISIBLE
                }
            }

        } catch (e: Exception) {
            Log.e("MealAnalysis", "Erreur affichage", e)
            Toast.makeText(this, "❌ Erreur affichage: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAlternativeView(alternative: JSONObject): CardView {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(getColor(android.R.color.white))
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameTextView = TextView(this).apply {
            text = alternative.optString("name", "Alternative")
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val confidenceTextView = TextView(this).apply {
            text = "${alternative.optDouble("confidence", 0.0).toInt()}% confiance"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dpToPx(4), 0, 0)
        }

        textLayout.addView(nameTextView)
        textLayout.addView(confidenceTextView)
        contentLayout.addView(textLayout)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}