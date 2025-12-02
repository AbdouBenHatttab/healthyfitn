package com.health.virtualdoctor.ui.consultation

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import com.health.virtualdoctor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

data class Doctor(
    val id: Int,
    val name: String,
    val specialty: String,
    val specialtyEmoji: String,
    val description: String,
    val specializations: List<String>,
    val rating: Float,
    val experience: Int,
    val availability: Boolean
)

class ConsultationActivity : ComponentActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etSearch: EditText
    private lateinit var containerDoctors: LinearLayout
    private lateinit var chipAll: Chip
    private lateinit var chipCardiology: Chip
    private lateinit var chipNutrition: Chip
    private lateinit var chipPsychology: Chip
    private lateinit var chipGeneral: Chip

    private var selectedSpecialty = "all"

    private val doctors = listOf(
        Doctor(
            1,
            "Dr. Michael Chen",
            "Cardiologist",
            "❤️",
            "Dr. Michael Chen is a board-certified cardiologist with over 15 years of experience. He specializes in preventive cardiology, heart disease management, and cardiac rehabilitation.",
            listOf("Heart Disease", "Hypertension", "Preventive Care", "Cardiac Rehab"),
            4.8f,
            15,
            true
        ),
        Doctor(
            2,
            "Dr. Sarah Johnson",
            "Nutritionist",
            "🥗",
            "Dr. Sarah Johnson is a registered nutritionist specializing in weight management, sports nutrition, and dietary planning for chronic conditions.",
            listOf("Weight Management", "Sports Nutrition", "Meal Planning", "Diabetes Care"),
            4.9f,
            12,
            true
        ),
        Doctor(
            3,
            "Dr. Ahmed Hassan",
            "Psychologist",
            "🧠",
            "Dr. Ahmed Hassan is a clinical psychologist with expertise in stress management, anxiety disorders, and cognitive behavioral therapy.",
            listOf("Stress Management", "Anxiety", "Depression", "CBT"),
            4.7f,
            10,
            false
        ),
        Doctor(
            4,
            "Dr. Emily Rodriguez",
            "General Practitioner",
            "👨‍⚕️",
            "Dr. Emily Rodriguez is a general practitioner with a holistic approach to healthcare, focusing on preventive medicine and family health.",
            listOf("Preventive Care", "Family Health", "Chronic Disease", "Health Checkups"),
            4.6f,
            8,
            true
        ),
        Doctor(
            5,
            "Dr. David Kim",
            "Cardiologist",
            "❤️",
            "Dr. David Kim specializes in interventional cardiology and has extensive experience in treating complex heart conditions.",
            listOf("Interventional Cardiology", "Heart Surgery", "Arrhythmia", "Heart Failure"),
            4.9f,
            18,
            true
        )
    )

    private var filteredDoctors = doctors.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation)

        initViews()
        setupListeners()
        displayDoctors()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)
        containerDoctors = findViewById(R.id.containerDoctors)
        chipAll = findViewById(R.id.chipAll)
        chipCardiology = findViewById(R.id.chipCardiology)
        chipNutrition = findViewById(R.id.chipNutrition)
        chipPsychology = findViewById(R.id.chipPsychology)
        chipGeneral = findViewById(R.id.chipGeneral)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDoctors(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipAll.setOnClickListener { selectSpecialty("all", chipAll) }
        chipCardiology.setOnClickListener { selectSpecialty("Cardiologist", chipCardiology) }
        chipNutrition.setOnClickListener { selectSpecialty("Nutritionist", chipNutrition) }
        chipPsychology.setOnClickListener { selectSpecialty("Psychologist", chipPsychology) }
        chipGeneral.setOnClickListener { selectSpecialty("General Practitioner", chipGeneral) }
    }

    private fun selectSpecialty(specialty: String, selectedChip: Chip) {
        selectedSpecialty = specialty

        // Reset all chips
        listOf(chipAll, chipCardiology, chipNutrition, chipPsychology, chipGeneral).forEach { chip ->
            chip.setChipBackgroundColorResource(android.R.color.transparent)
            chip.setChipBackgroundColorResource(R.color.background)
        }

        // Highlight selected chip
        selectedChip.setChipBackgroundColorResource(R.color.primary)
        selectedChip.setTextColor(getColor(R.color.white))

        filterDoctors(etSearch.text.toString())
    }

    private fun filterDoctors(searchQuery: String) {
        filteredDoctors = doctors.filter { doctor ->
            val matchesSearch = searchQuery.isEmpty() ||
                    doctor.name.contains(searchQuery, ignoreCase = true) ||
                    doctor.specialty.contains(searchQuery, ignoreCase = true)

            val matchesSpecialty = selectedSpecialty == "all" ||
                    doctor.specialty == selectedSpecialty

            matchesSearch && matchesSpecialty
        }

        displayDoctors()
    }

    private fun displayDoctors() {
        containerDoctors.removeAllViews()

        if (filteredDoctors.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "Aucun médecin trouvé"
                textSize = 16f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, dpToPx(24), 0, dpToPx(24))
            }
            containerDoctors.addView(emptyView)
            return
        }

        filteredDoctors.forEach { doctor ->
            val doctorCard = createDoctorCard(doctor)
            containerDoctors.addView(doctorCard)
        }
    }

    private fun createDoctorCard(doctor: Doctor): CardView {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(getColor(R.color.white))
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // Header: Avatar + Name + Specialty
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Avatar
        val avatarView = TextView(this).apply {
            text = doctor.specialtyEmoji
            textSize = 48f
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(64),
                dpToPx(64)
            ).apply {
                setMargins(0, 0, dpToPx(12), 0)
            }
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_gradient_primary)
        }

        // Info Layout
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(this).apply {
            text = doctor.name
            textSize = 18f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val specialtyText = TextView(this).apply {
            text = doctor.specialty
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dpToPx(2), 0, 0)
        }

        val experienceText = TextView(this).apply {
            text = "⭐ ${doctor.rating} • ${doctor.experience} ans d'expérience"
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dpToPx(4), 0, 0)
        }

        infoLayout.addView(nameText)
        infoLayout.addView(specialtyText)
        infoLayout.addView(experienceText)

        // Availability Badge
        val availabilityBadge = TextView(this).apply {
            text = if (doctor.availability) "🟢 Disponible" else "🔴 Occupé"
            textSize = 11f
            setTextColor(if (doctor.availability) getColor(R.color.primary) else getColor(android.R.color.holo_red_dark))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setBackgroundResource(if (doctor.availability) R.color.background else android.R.color.holo_red_light)
        }

        headerLayout.addView(avatarView)
        headerLayout.addView(infoLayout)
        headerLayout.addView(availabilityBadge)

        // Description
        val descriptionText = TextView(this).apply {
            text = doctor.description
            textSize = 13f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Specializations
        val specializationsLabel = TextView(this).apply {
            text = "Spécialisations"
            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(8))
        }

        val specializationsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val scrollView = android.widget.HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }

        val chipsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        doctor.specializations.forEach { spec ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = spec
                isClickable = false
                setChipBackgroundColorResource(R.color.background)
                setTextColor(getColor(R.color.text_primary))
            }
            chipsLayout.addView(chip)
        }

        scrollView.addView(chipsLayout)
        specializationsLayout.addView(scrollView)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, dpToPx(12), 0, dpToPx(12))
            }
            setBackgroundColor(getColor(android.R.color.darker_gray))
            alpha = 0.2f
        }

        // Action Buttons
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weightSum = 3f
        }

        val btnCall = MaterialButton(this).apply {
            text = "📞 Call"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(getColor(R.color.primary))
            strokeWidth = dpToPx(1)
            strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
            cornerRadius = dpToPx(8)
            setOnClickListener {
                // Open call activity or dialer
            }
        }

        val btnVideo = MaterialButton(this).apply {
            text = "📹 Video"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            setBackgroundColor(getColor(R.color.primary))
            setTextColor(getColor(R.color.white))
            cornerRadius = dpToPx(8)
            setOnClickListener {
                openVideoCall(doctor)
            }
        }

        val btnChat = MaterialButton(this).apply {
            text = "💬 Chat"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dpToPx(4), 0, 0, 0)
            }
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(getColor(R.color.primary))
            strokeWidth = dpToPx(1)
            strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
            cornerRadius = dpToPx(8)
            setOnClickListener {
                // Open chat activity
            }
        }

        buttonsLayout.addView(btnCall)
        buttonsLayout.addView(btnVideo)
        buttonsLayout.addView(btnChat)

        // Add all views to main layout
        mainLayout.addView(headerLayout)
        mainLayout.addView(descriptionText)
        mainLayout.addView(specializationsLabel)
        mainLayout.addView(specializationsLayout)
        mainLayout.addView(divider)
        mainLayout.addView(buttonsLayout)

        cardView.addView(mainLayout)

        return cardView
    }

    private fun openVideoCall(doctor: Doctor) {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("DOCTOR_NAME", doctor.name)
        intent.putExtra("DOCTOR_SPECIALTY", doctor.specialty)
        intent.putExtra("DOCTOR_ID", doctor.id)
        startActivity(intent)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}