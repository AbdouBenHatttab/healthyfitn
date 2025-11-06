package com.health.virtualdoctor.ui.consultation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.health.virtualdoctor.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class VideoCallActivity : ComponentActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnMicrophone: FloatingActionButton
    private lateinit var btnEndCall: FloatingActionButton
    private lateinit var btnVideo: FloatingActionButton
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorSpecialty: TextView
    private lateinit var tvCallDuration: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var doctorPlaceholder: LinearLayout
    private lateinit var selfPlaceholder: LinearLayout

    private var isMicrophoneOn = true
    private var isVideoOn = true
    private var callStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var durationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        // Get doctor info from intent
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Médecin"
        val doctorSpecialty = intent.getStringExtra("DOCTOR_SPECIALTY") ?: ""

        initViews()
        setupDoctorInfo(doctorName, doctorSpecialty)
        setupListeners()
        simulateCallConnection()
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnMicrophone = findViewById(R.id.btnMicrophone)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnVideo = findViewById(R.id.btnVideo)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty)
        tvCallDuration = findViewById(R.id.tvCallDuration)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        doctorPlaceholder = findViewById(R.id.doctorPlaceholder)
        selfPlaceholder = findViewById(R.id.selfPlaceholder)
    }

    private fun setupDoctorInfo(name: String, specialty: String) {
        tvDoctorName.text = name
        tvDoctorSpecialty.text = specialty
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            endCall()
        }

        btnSwitchCamera.setOnClickListener {
            Toast.makeText(this, "Changement de caméra", Toast.LENGTH_SHORT).show()
            // Implement camera switch logic
        }

        btnMicrophone.setOnClickListener {
            toggleMicrophone()
        }

        btnVideo.setOnClickListener {
            toggleVideo()
        }

        btnEndCall.setOnClickListener {
            endCall()
        }
    }

    private fun simulateCallConnection() {
        tvConnectionStatus.visibility = View.VISIBLE
        tvConnectionStatus.text = "Connexion en cours..."

        handler.postDelayed({
            tvConnectionStatus.text = "Connecté"
            handler.postDelayed({
                tvConnectionStatus.visibility = View.GONE
                startCallTimer()
            }, 1000)
        }, 2000)
    }

    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()

        durationRunnable = object : Runnable {
            override fun run() {
                val duration = System.currentTimeMillis() - callStartTime
                val seconds = (duration / 1000) % 60
                val minutes = (duration / 1000) / 60

                tvCallDuration.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(durationRunnable!!)
    }

    private fun toggleMicrophone() {
        isMicrophoneOn = !isMicrophoneOn

        if (isMicrophoneOn) {
            btnMicrophone.setImageResource(R.drawable.ic_mic)
            btnMicrophone.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.holo_green_dark)
            )
            Toast.makeText(this, "Micro activé", Toast.LENGTH_SHORT).show()
        } else {
            btnMicrophone.setImageResource(R.drawable.ic_mic_off)
            btnMicrophone.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.darker_gray)
            )
            Toast.makeText(this, "Micro désactivé", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleVideo() {
        isVideoOn = !isVideoOn

        if (isVideoOn) {
            btnVideo.setImageResource(R.drawable.ic_videocam)
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.holo_blue_dark)
            )
            selfPlaceholder.visibility = View.VISIBLE
            Toast.makeText(this, "Vidéo activée", Toast.LENGTH_SHORT).show()
        } else {
            btnVideo.setImageResource(R.drawable.ic_videocam_off)
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.darker_gray)
            )
            selfPlaceholder.visibility = View.GONE
            Toast.makeText(this, "Vidéo désactivée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun endCall() {
        durationRunnable?.let { handler.removeCallbacks(it) }

        Toast.makeText(this, "Appel terminé", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        durationRunnable?.let { handler.removeCallbacks(it) }
    }
}