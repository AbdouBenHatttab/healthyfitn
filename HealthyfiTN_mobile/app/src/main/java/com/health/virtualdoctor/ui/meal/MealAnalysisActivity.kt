package com.health.virtualdoctor.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import androidx.lifecycle.lifecycleScope
import com.health.virtualdoctor.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class MealAnalysisActivity : ComponentActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var ivMealImage: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var cardNutritionalBreakdown: CardView
    private lateinit var cardHealthierAlternatives: CardView
    private lateinit var tvProteins: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvFiber: TextView
    private lateinit var progressProteins: ProgressBar
    private lateinit var progressCarbs: ProgressBar
    private lateinit var progressFats: ProgressBar
    private lateinit var progressFiber: ProgressBar
    private lateinit var containerAlternatives: LinearLayout
    private lateinit var btnScanNewMeal: MaterialButton

    private var currentImageBitmap: Bitmap? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            currentImageBitmap = it
            displayImage(it)
            analyzeMeal(it)
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
                Toast.makeText(this, "Erreur de chargement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_analysis)

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
        tvProteins = findViewById(R.id.tvProteins)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvFiber = findViewById(R.id.tvFiber)
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnScanNewMeal.setOnClickListener {
            resetView()
        }
    }

    private fun displayImage(bitmap: Bitmap) {
        ivMealImage.setImageBitmap(bitmap)
        ivMealImage.visibility = View.VISIBLE
        layoutPlaceholder.visibility = View.GONE
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun analyzeMeal(bitmap: Bitmap) {
        showLoading(true)
        cardNutritionalBreakdown.visibility = View.GONE
        cardHealthierAlternatives.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val result = sendImageToServer(base64Image)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (result != null) {
                        displayNutritionalInfo(result)
                        btnScanNewMeal.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this@MealAnalysisActivity, "Erreur d'analyse", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@MealAnalysisActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private suspend fun sendImageToServer(base64Image: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val serverUrl = "https://your-api-endpoint.com/analyze-meal"

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val jsonBody = JSONObject().apply {
                put("image", base64Image)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            response.close()

            if (response.isSuccessful && responseBody != null) {
                JSONObject(responseBody)
            } else {
                // Données de démo pour tester l'interface
                createDemoData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Données de démo en cas d'erreur
            createDemoData()
        }
    }

    private fun createDemoData(): JSONObject {
        return JSONObject().apply {
            put("proteins", 32)
            put("carbs", 48)
            put("fats", 15)
            put("fiber", 8)

            val alternatives = JSONArray()
            alternatives.put(JSONObject().apply {
                put("name", "Mediterranean Bowl")
                put("calories", 380)
                put("image", "")
            })
            alternatives.put(JSONObject().apply {
                put("name", "Berry Smoothie Bowl")
                put("calories", 320)
                put("image", "")
            })

            put("alternatives", alternatives)
        }
    }

    private fun displayNutritionalInfo(data: JSONObject) {
        // Afficher les valeurs nutritionnelles
        val proteins = data.optInt("proteins", 0)
        val carbs = data.optInt("carbs", 0)
        val fats = data.optInt("fats", 0)
        val fiber = data.optInt("fiber", 0)

        tvProteins.text = "${proteins}g"
        tvCarbs.text = "${carbs}g"
        tvFats.text = "${fats}g"
        tvFiber.text = "${fiber}g"

        // Calculer les pourcentages (basé sur des valeurs quotidiennes recommandées)
        progressProteins.progress = (proteins * 100 / 50).coerceAtMost(100)
        progressCarbs.progress = (carbs * 100 / 60).coerceAtMost(100)
        progressFats.progress = (fats * 100 / 35).coerceAtMost(100)
        progressFiber.progress = (fiber * 100 / 15).coerceAtMost(100)

        cardNutritionalBreakdown.visibility = View.VISIBLE

        // Afficher les alternatives
        val alternatives = data.optJSONArray("alternatives")
        if (alternatives != null && alternatives.length() > 0) {
            containerAlternatives.removeAllViews()

            for (i in 0 until alternatives.length()) {
                val alternative = alternatives.getJSONObject(i)
                val alternativeView = createAlternativeView(alternative)
                containerAlternatives.addView(alternativeView)
            }

            cardHealthierAlternatives.visibility = View.VISIBLE
        }
    }

    private fun createAlternativeView(alternative: JSONObject): CardView {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(120)
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
        }

        // Image placeholder
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(96))
            setBackgroundColor(getColor(R.color.background))
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_restaurant) // Placeholder icon
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(dpToPx(12), 0, 0, 0)
        }

        val nameTextView = TextView(this).apply {
            text = alternative.optString("name", "Alternative")
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val caloriesTextView = TextView(this).apply {
            text = "${alternative.optInt("calories", 0)} kcal"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dpToPx(4), 0, 0)
        }

        textLayout.addView(nameTextView)
        textLayout.addView(caloriesTextView)

        contentLayout.addView(imageView)
        contentLayout.addView(textLayout)

        cardView.addView(contentLayout)

        return cardView
    }

    private fun resetView() {
        currentImageBitmap = null
        ivMealImage.visibility = View.GONE
        layoutPlaceholder.visibility = View.VISIBLE
        cardNutritionalBreakdown.visibility = View.GONE
        cardHealthierAlternatives.visibility = View.GONE
        btnScanNewMeal.visibility = View.GONE
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}