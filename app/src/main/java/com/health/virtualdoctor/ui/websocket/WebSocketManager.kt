package com.health.virtualdoctor.ui.websocket

import android.content.Context
import android.util.Log
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val callId: String,
    private val tokenManager: TokenManager,
    private val onOfferReceived: (String) -> Unit,
    private val onAnswerReceived: (String) -> Unit,
    private val onIceCandidateReceived: (candidate: String, sdpMid: String, sdpMLineIndex: Int) -> Unit,
    private val onCallEnded: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val context: Context,
) {
    companion object {
        private const val TAG = "WebSocketManager"
    }

    private var webSocket: WebSocket? = null
    private var isConnected = false

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    fun connect() {
        try {
            // ✅ Utiliser getWebSocketUrl avec context
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("user_email", "") ?: ""

            val wsUrl = RetrofitClient.getWebSocketUrl(callId, userId, context)

            Log.d(TAG, "Connecting to: $wsUrl")

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            isConnected = true
                            Log.d(TAG, "═══════════════════════════════════════")
                            Log.d(TAG, "✅✅✅ WEBSOCKET OPENED ✅✅✅")
                            Log.d(TAG, "═══════════════════════════════════════")
                            Log.d(TAG, "Response Code: ${response.code}")
                            Log.d(TAG, "Response Message: ${response.message}")
                            Log.d(TAG, "Protocol: ${response.protocol}")
                            Log.d(TAG, "═══════════════════════════════════════")
                            onConnected()
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            Log.d(TAG, "📨 Message received (${text.length} chars)")
                            handleMessage(text)
                        }

                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            Log.d(TAG, "📨 Binary message (${bytes.size} bytes)")
                            onMessage(webSocket, bytes.utf8())
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "🔴 CLOSING: $code - $reason")
                            isConnected = false
                            webSocket.close(1000, null)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "🔌 CLOSED: $code - $reason")
                            isConnected = false
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            isConnected = false
                            Log.e(TAG, "═══════════════════════════════════════")
                            Log.e(TAG, "❌❌❌ WEBSOCKET FAILED ❌❌❌")
                            Log.e(TAG, "═══════════════════════════════════════")
                            Log.e(TAG, "Exception: ${t.javaClass.simpleName}")
                            Log.e(TAG, "Message: ${t.message}")
                            Log.e(TAG, "Response: ${response?.code} ${response?.message}")
                            Log.e(TAG, "═══════════════════════════════════════")
                            t.printStackTrace()
                            onError("WebSocket failed: ${t.message}")
                        }
                    })

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception creating WebSocket", e)
                    e.printStackTrace()
                    onError("Failed to create WebSocket: ${e.message}")
                }
            }

                    private fun handleMessage(text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type", "UNKNOWN")

                    Log.d(TAG, "📝 Message type: $type")

                    when (type) {
                        "OFFER" -> {
                            val sdp = json.getString("sdp")
                            Log.d(TAG, "📥 OFFER (${sdp.length} chars)")
                            onOfferReceived(sdp)
                        }
                        "ANSWER" -> {
                            val sdp = json.getString("sdp")
                            Log.d(TAG, "📥 ANSWER (${sdp.length} chars)")
                            onAnswerReceived(sdp)
                        }
                        "ICE_CANDIDATE" -> {
                            val candidate = json.getString("candidate")
                            val sdpMid = json.getString("sdpMid")
                            val sdpMLineIndex = json.getInt("sdpMLineIndex")
                            Log.d(TAG, "🧊 ICE_CANDIDATE")
                            onIceCandidateReceived(candidate, sdpMid, sdpMLineIndex)
                        }
                        "CALL_ENDED" -> {
                            val reason = json.optString("reason", "UNKNOWN")
                            Log.d(TAG, "📞 CALL_ENDED: $reason")
                            onCallEnded(reason)
                        }
                        "CONNECTED" -> {
                            Log.d(TAG, "✅ Server confirmed connection")
                        }
                        "PEER_DISCONNECTED" -> {
                            val userId = json.optString("userId", "unknown")
                            Log.d(TAG, "⚠️ Peer disconnected: $userId")
                        }
                        else -> {
                            Log.w(TAG, "⚠️ Unknown type: $type")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Parse error", e)
                    e.printStackTrace()
                }
            }

            fun sendMessage(message: String) {
                if (!isConnected) {
                    Log.e(TAG, "❌ Not connected - cannot send")
                    return
                }

                val success = webSocket?.send(message) ?: false
                if (success) {
                    Log.d(TAG, "✅ Sent (${message.length} chars)")
                } else {
                    Log.e(TAG, "❌ Send failed")
                }
            }

            fun disconnect() {
                try {
                    Log.d(TAG, "🔌 Disconnecting...")
                    isConnected = false
                    webSocket?.close(1000, "Call ended")
                    webSocket = null
                    Log.d(TAG, "✅ Disconnected")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Disconnect error", e)
                }
            }

            fun isConnected(): Boolean {
                return isConnected
            }
}