package com.health.virtualdoctor.ui.consultation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.health.virtualdoctor.R

import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.utils.TokenManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.health.virtualdoctor.ui.webrtc.WebRTCClient
import com.health.virtualdoctor.ui.webrtc.WebSocketSignalingClient
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import kotlinx.coroutines.launch
import org.json.JSONArray

class VideoCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoCallActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    // Views
    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var btnMic: FloatingActionButton
    private lateinit var btnVideo: FloatingActionButton
    private lateinit var btnEndCall: FloatingActionButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    // WebRTC
    private var webRTCClient: WebRTCClient? = null
    private var signalingClient: WebSocketSignalingClient? = null
    private var eglBase: EglBase? = null

    // Call info
    private lateinit var appointmentId: String
    private lateinit var callType: String
    private var isInitiator = false
    private lateinit var tokenManager: TokenManager
    private var callId: String? = null

    // State
    private var isMicEnabled = true
    private var isVideoEnabled = true
    private var isConnected = false
    private var isCleanedUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        tokenManager = TokenManager(this)

        // Get intent extras
        appointmentId = intent.getStringExtra("appointmentId") ?: ""
        callType = intent.getStringExtra("callType") ?: "VIDEO"
        isInitiator = intent.getBooleanExtra("isInitiator", false)
        val providedCallId = intent.getStringExtra("callId")

        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "📞 VIDEO CALL STARTED")
        Log.d(TAG, "   Appointment ID: $appointmentId")
        Log.d(TAG, "   Call Type: $callType")
        Log.d(TAG, "   Is Initiator: $isInitiator")
        Log.d(TAG, "   Provided Call ID: $providedCallId")
        Log.d(TAG, "═══════════════════════════════════════════")

        initViews()
        checkPermissions()
        // ✅ If call ID was provided, skip initiate and go straight to join
        if (providedCallId != null) {
            callId = providedCallId
            Log.d(TAG, "✅ Using provided call ID: $callId")
        }
    }
    private fun initViews() {
        localVideoView = findViewById(R.id.localVideoView)
        remoteVideoView = findViewById(R.id.remoteVideoView)
        btnMic = findViewById(R.id.btnMic)
        btnVideo = findViewById(R.id.btnVideo)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        btnMic.setOnClickListener { toggleMic() }
        btnVideo.setOnClickListener { toggleVideo() }
        btnEndCall.setOnClickListener { endCall() }
        btnSwitchCamera.setOnClickListener { switchCamera() }

        updateStatus("Initialisation...")
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            initiateCallSession()
        } else {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initiateCallSession()
            } else {
                Toast.makeText(this, "Permissions requises pour l'appel vidéo", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initiateCallSession() {
        // ✅ Skip if we already have a call ID
        if (callId != null) {
            Log.d(TAG, "⏭️ Skipping call initiation, already have call ID: $callId")
            // Fetch call session details to get ICE servers
            fetchCallSessionDetails()
            return
        }
        updateStatus("Création de la session...")

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = mapOf(
                    "appointmentId" to appointmentId,
                    "callType" to callType
                )

                Log.d(TAG, "📞 Initiating call session...")
                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .initiateCall(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val callSession = response.body()!!
                    callId = callSession.callId

                    Log.d(TAG, "✅ Call session created: $callId")

                    // Parse ICE servers
                    val iceServers = parseIceServers(callSession.iceServers)

                    // Initialize WebRTC
                    initWebRTC(iceServers)
                } else {
                    Log.e(TAG, "❌ Failed to create call session: ${response.code()}")
                    updateStatus("Erreur: ${response.code()}")
                    Toast.makeText(this@VideoCallActivity, "Erreur lors de la création de la session", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception initiating call", e)
                updateStatus("Erreur: ${e.message}")
                Toast.makeText(this@VideoCallActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun fetchCallSessionDetails() {
        updateStatus("Récupération de la session...")

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                Log.d(TAG, "📥 Fetching call session details: $callId")
                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .getCallSession(token, callId!!)

                if (response.isSuccessful && response.body() != null) {
                    val callSession = response.body()!!

                    Log.d(TAG, "✅ Got call session details")

                    // Parse ICE servers
                    val iceServers = parseIceServers(callSession.iceServers)

                    // Initialize WebRTC
                    initWebRTC(iceServers)
                } else {
                    Log.e(TAG, "❌ Failed to get call session: ${response.code()}")
                    Toast.makeText(this@VideoCallActivity,
                        "Erreur lors de la récupération de la session",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception fetching call session", e)
                Toast.makeText(this@VideoCallActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    private fun parseIceServers(iceServersJson: String?): List<PeerConnection.IceServer> {
        if (iceServersJson.isNullOrEmpty()) {
            Log.w(TAG, "No ICE servers provided, using default STUN")
            return listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        }

        return try {
            val jsonArray = JSONArray(iceServersJson)
            val iceServers = mutableListOf<PeerConnection.IceServer>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val urls = obj.getString("urls")

                val builder = PeerConnection.IceServer.builder(urls)

                if (obj.has("username") && obj.has("credential")) {
                    builder.setUsername(obj.getString("username"))
                    builder.setPassword(obj.getString("credential"))
                }

                iceServers.add(builder.createIceServer())
            }

            Log.d(TAG, "✅ Parsed ${iceServers.size} ICE servers")
            iceServers
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse ICE servers", e)
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        }
    }

    private fun initWebRTC(iceServers: List<PeerConnection.IceServer>) {
        Log.d(TAG, "🚀 Initializing WebRTC...")
        updateStatus("Initialisation caméra...")

        try {
            eglBase = EglBase.create()

            localVideoView.init(eglBase!!.eglBaseContext, null)
            localVideoView.setMirror(true)
            localVideoView.setEnableHardwareScaler(true)
            localVideoView.setZOrderMediaOverlay(true)

            remoteVideoView.init(eglBase!!.eglBaseContext, null)
            remoteVideoView.setMirror(false)
            remoteVideoView.setEnableHardwareScaler(true)

            webRTCClient = WebRTCClient(
                context = applicationContext,
                eglBase = eglBase!!,
                localVideoView = localVideoView,
                remoteVideoView = remoteVideoView,
                listener = webRTCListener
            )

            webRTCClient?.startLocalVideo()
            webRTCClient?.createPeerConnection(iceServers)

            Log.d(TAG, "✅ WebRTC initialized")
            connectToSignalingServer()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing WebRTC", e)
            updateStatus("Erreur: ${e.message}")
            Toast.makeText(this, "Erreur WebRTC: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectToSignalingServer() {
        if (callId == null) {
            Log.e(TAG, "❌ No call ID available")
            return
        }

        updateStatus("Connexion au serveur...")

        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_email", "") ?: ""

        val wsUrl = RetrofitClient.getWebSocketUrl(callId!!, userId, this)
        Log.d(TAG, "🔌 Connecting to: $wsUrl")

        signalingClient = WebSocketSignalingClient(
            url = wsUrl,
            listener = signalingListener
        )
        signalingClient?.connect()
    }

    // WebRTC Listener
    private val webRTCListener = object : WebRTCClient.Listener {
        override fun onLocalVideoReady() {
            runOnUiThread {
                Log.d(TAG, "📹 Local video ready")
                localVideoView.visibility = View.VISIBLE
            }
        }

        override fun onRemoteVideoReady() {
            runOnUiThread {
                Log.d(TAG, "📺 Remote video ready")
                remoteVideoView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                updateStatus("Connecté")
                isConnected = true
            }
        }

        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "🧊 Sending ICE candidate")
            signalingClient?.sendIceCandidate(candidate)
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            runOnUiThread {
                Log.d(TAG, "🔗 ICE state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        updateStatus("Connecté")
                        isConnected = true
                        progressBar.visibility = View.GONE
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        updateStatus("Déconnecté")
                        isConnected = false
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        updateStatus("Échec de connexion")
                    }
                    else -> {}
                }
            }
        }

        override fun onOfferCreated(sdp: SessionDescription) {
            Log.d(TAG, "📤 Offer created, sending...")
            signalingClient?.sendOffer(sdp)
        }

        override fun onAnswerCreated(sdp: SessionDescription) {
            Log.d(TAG, "📤 Answer created, sending...")
            signalingClient?.sendAnswer(sdp)
        }
    }

    // Signaling Listener
    private val signalingListener = object : WebSocketSignalingClient.Listener {
        override fun onConnected() {
            runOnUiThread {
                Log.d(TAG, "✅ Signaling connected")
                updateStatus("En attente de l'autre participant...")
                signalingClient?.sendReady()
            }
        }

        override fun onUserJoined(userId: String, participantCount: Int) {
            runOnUiThread {
                Log.d(TAG, "👤 User joined: $userId (total: $participantCount)")

                if (participantCount >= 2 && isInitiator) {
                    Log.d(TAG, "🎬 Creating offer as initiator...")
                    updateStatus("Établissement de la connexion...")
                    webRTCClient?.createOffer()
                }
            }
        }

        override fun onPeerReady(peerId: String) {
            runOnUiThread {
                Log.d(TAG, "✅ Peer ready: $peerId")

                if (isInitiator) {
                    Log.d(TAG, "🎬 Peer ready, creating offer...")
                    updateStatus("Connexion en cours...")
                    webRTCClient?.createOffer()
                }
            }
        }

        override fun onOfferReceived(sdp: SessionDescription, fromUserId: String) {
            runOnUiThread {
                Log.d(TAG, "📥 Offer received from: $fromUserId")
                updateStatus("Réponse en cours...")
                webRTCClient?.handleOffer(sdp)
            }
        }

        override fun onAnswerReceived(sdp: SessionDescription, fromUserId: String) {
            runOnUiThread {
                Log.d(TAG, "📥 Answer received from: $fromUserId")
                webRTCClient?.handleAnswer(sdp)
            }
        }

        override fun onIceCandidateReceived(candidate: IceCandidate, fromUserId: String) {
            Log.d(TAG, "🧊 ICE candidate received from: $fromUserId")
            webRTCClient?.addIceCandidate(candidate)
        }

        override fun onUserLeft(userId: String) {
            runOnUiThread {
                Log.d(TAG, "👋 User left: $userId")
                Toast.makeText(this@VideoCallActivity, "L'autre participant a quitté", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }

        override fun onError(error: String) {
            runOnUiThread {
                Log.e(TAG, "❌ Signaling error: $error")
                updateStatus("Erreur: $error")
            }
        }

        override fun onDisconnected() {
            runOnUiThread {
                Log.d(TAG, "🔌 Signaling disconnected")
                if (isConnected) {
                    updateStatus("Déconnecté")
                }
            }
        }
    }

    // UI Controls
    private fun toggleMic() {
        isMicEnabled = !isMicEnabled
        webRTCClient?.setMicEnabled(isMicEnabled)
        btnMic.setImageResource(
            if (isMicEnabled) R.drawable.ic_mic else R.drawable.ic_mic_off
        )
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        webRTCClient?.setVideoEnabled(isVideoEnabled)
        localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
        btnVideo.setImageResource(
            if (isVideoEnabled) R.drawable.ic_videocam else R.drawable.ic_videocam_off
        )
    }

    private fun switchCamera() {
        webRTCClient?.switchCamera()
    }

    private fun endCall() {
        Log.d(TAG, "🔴 Ending call...")
        signalingClient?.sendHangup()
        cleanup()
        finish()
    }

    private fun updateStatus(status: String) {
        tvStatus.text = status
    }

    private fun cleanup() {
        if (isCleanedUp) return
        isCleanedUp = true

        Log.d(TAG, "🧹 Cleaning up...")

        try {
            signalingClient?.disconnect()
            signalingClient = null

            webRTCClient?.dispose()
            webRTCClient = null

            localVideoView.release()
            remoteVideoView.release()

            eglBase?.release()
            eglBase = null

            Log.d(TAG, "✅ Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}