package com.health.virtualdoctor.ui.webrtc

import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class WebSocketSignalingClient(
    private val url: String,
    private val listener: Listener
) {
    companion object {
        private const val TAG = "SignalingClient"
    }

    interface Listener {
        fun onConnected()
        fun onUserJoined(userId: String, participantCount: Int)
        fun onPeerReady(peerId: String)
        fun onOfferReceived(sdp: SessionDescription, fromUserId: String)
        fun onAnswerReceived(sdp: SessionDescription, fromUserId: String)
        fun onIceCandidateReceived(candidate: IceCandidate, fromUserId: String)
        fun onUserLeft(userId: String)
        fun onError(error: String)
        fun onDisconnected()
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun connect() {
        try {
            Log.d(TAG, "═══════════════════════════════════════════════════════════════")
            Log.d(TAG, "🔌 CONNECTING TO WEBSOCKET")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════")
            Log.d(TAG, "   URL: $url")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════")

            val request = Request.Builder()
                .url(url)
                .addHeader("ngrok-skip-browser-warning", "true") // ✅ For ngrok/Cloudflare
                .addHeader("User-Agent", "Android-WebRTC-Client")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "═══════════════════════════════════════════════════════════════")
                    Log.d(TAG, "✅ WEBSOCKET CONNECTED")
                    Log.d(TAG, "═══════════════════════════════════════════════════════════════")
                    Log.d(TAG, "   Response code: ${response.code}")
                    Log.d(TAG, "   Protocol: ${response.protocol}")
                    Log.d(TAG, "═══════════════════════════════════════════════════════════════")
                    listener.onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "📨 Message received (${text.length} chars): ${text.take(100)}...")
                    handleMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    onMessage(webSocket, bytes.utf8())
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "🔴 WebSocket closing: $code - $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "🔌 WebSocket closed: $code - $reason")
                    listener.onDisconnected()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "═══════════════════════════════════════════════════════════════")
                    Log.e(TAG, "❌ WEBSOCKET CONNECTION FAILED")
                    Log.e(TAG, "═══════════════════════════════════════════════════════════════")
                    Log.e(TAG, "   Error: ${t.javaClass.simpleName}")
                    Log.e(TAG, "   Message: ${t.message}")
                    Log.e(TAG, "   Response code: ${response?.code}")
                    Log.e(TAG, "   Response message: ${response?.message}")

                    // ✅ Log response body for debugging
                    response?.body?.let { body ->
                        try {
                            val errorBody = body.string()
                            Log.e(TAG, "   Response body: $errorBody")
                        } catch (e: Exception) {
                            Log.e(TAG, "   Could not read response body")
                        }
                    }

                    Log.e(TAG, "═══════════════════════════════════════════════════════════════")
                    t.printStackTrace()
                    listener.onError("Connection failed: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating WebSocket", e)
            listener.onError("Failed to connect: ${e.message}")
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "UNKNOWN")

            Log.d(TAG, "📋 Processing message type: $type")

            when (type) {
                "CONNECTED" -> {
                    val callId = json.optString("callId")
                    val userId = json.optString("userId")
                    Log.d(TAG, "✅ Server confirmed connection - Call: $callId, User: $userId")
                }

                "USER_JOINED" -> {
                    val userId = json.getString("userId")
                    val count = json.optInt("participantCount", 2)
                    Log.d(TAG, "👤 User joined: $userId (total: $count)")
                    listener.onUserJoined(userId, count)
                }

                "READY" -> {
                    val peerId = json.getString("userId")
                    Log.d(TAG, "✅ Peer ready: $peerId")
                    listener.onPeerReady(peerId)
                }

                "OFFER" -> {
                    val sdpString = json.getString("sdp")
                    val fromUserId = json.optString("fromUserId", "unknown")
                    val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpString)
                    Log.d(TAG, "📥 Offer received from: $fromUserId (${sdpString.length} chars)")
                    listener.onOfferReceived(sdp, fromUserId)
                }

                "ANSWER" -> {
                    val sdpString = json.getString("sdp")
                    val fromUserId = json.optString("fromUserId", "unknown")
                    val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpString)
                    Log.d(TAG, "📥 Answer received from: $fromUserId (${sdpString.length} chars)")
                    listener.onAnswerReceived(sdp, fromUserId)
                }

                "ICE_CANDIDATE" -> {
                    val candidateString = json.getString("candidate")
                    val sdpMid = json.getString("sdpMid")
                    val sdpMLineIndex = json.getInt("sdpMLineIndex")
                    val fromUserId = json.optString("fromUserId", "unknown")

                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateString)
                    Log.d(TAG, "🧊 ICE candidate from: $fromUserId")
                    listener.onIceCandidateReceived(candidate, fromUserId)
                }

                "PEER_DISCONNECTED" -> {
                    val userId = json.getString("userId")
                    Log.d(TAG, "👋 Peer disconnected: $userId")
                    listener.onUserLeft(userId)
                }

                "HANGUP" -> {
                    val userId = json.optString("userId", "unknown")
                    Log.d(TAG, "📞 Hangup from: $userId")
                    listener.onUserLeft(userId)
                }

                else -> {
                    Log.w(TAG, "⚠️ Unknown message type: $type")
                    Log.w(TAG, "   Full message: $text")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Parse error", e)
            Log.e(TAG, "   Message was: $text")
            listener.onError("Parse error: ${e.message}")
        }
    }

    fun sendReady() {
        val message = JSONObject().apply {
            put("type", "READY")
        }
        sendMessage(message.toString())
    }

    fun sendOffer(sdp: SessionDescription) {
        val message = JSONObject().apply {
            put("type", "OFFER")
            put("sdp", sdp.description)
        }
        Log.d(TAG, "📤 Sending OFFER (${sdp.description.length} chars)")
        sendMessage(message.toString())
    }

    fun sendAnswer(sdp: SessionDescription) {
        val message = JSONObject().apply {
            put("type", "ANSWER")
            put("sdp", sdp.description)
        }
        Log.d(TAG, "📤 Sending ANSWER (${sdp.description.length} chars)")
        sendMessage(message.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val message = JSONObject().apply {
            put("type", "ICE_CANDIDATE")
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        Log.d(TAG, "📤 Sending ICE_CANDIDATE")
        sendMessage(message.toString())
    }

    fun sendHangup() {
        val message = JSONObject().apply {
            put("type", "HANGUP")
        }
        sendMessage(message.toString())
    }

    private fun sendMessage(message: String) {
        val success = webSocket?.send(message) ?: false
        if (success) {
            Log.d(TAG, "✅ Message sent (${message.length} chars)")
        } else {
            Log.e(TAG, "❌ Failed to send message - WebSocket not connected")
            listener.onError("Failed to send message - not connected")
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Call ended")
            webSocket = null
            Log.d(TAG, "🔌 Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Disconnect error", e)
        }
    }
}