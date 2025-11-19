package com.health.virtualdoctor.ui.chatBot

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.utils.TokenManager
import org.json.JSONObject

class ChatBotActivity : AppCompatActivity() {

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var editMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var queue: RequestQueue
    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: ChatAdapter

    // CONFIG
    private val BASE_URL = "https://ruinous-loma-nondipterous.ngrok-free.dev"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        tokenManager = TokenManager(this)
        initViews()
        initVolley()
        setupRecyclerView()
        setupBackButton()
        setupSendButton()

        // VÉRIFIE SI L'UTILISATEUR EST CONNECTÉ
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Veuillez vous connecter pour utiliser le chatbot.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // MESSAGE DE BIENVENUE
        val userName = tokenManager.getUserName() ?: "Utilisateur"
        addBotMessage("Bonjour $userName ! Je suis votre assistant santé IA. Comment puis-je vous aider ?")
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun initVolley() {
        queue = Volley.newRequestQueue(this)
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.layoutManager = layoutManager
        adapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener { finish() }
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val text = editMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                editMessage.text?.clear()
            }
        }
    }

    private fun sendUserMessage(text: String) {
        val userId = tokenManager.getUserId()
            ?: run {
                addBotMessage("Erreur : utilisateur non identifié.")
                return
            }

        adapter.addMessage(ChatMessage(text, isUser = true))
        recyclerView.scrollToPosition(adapter.itemCount - 1)

        val json = JSONObject().apply {
            put("userId", userId)
            put("prompt", text)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, "$BASE_URL/chat", json,
            { response ->
                val botText = response.optString("response", "Réponse IA...")
                addBotMessage(botText)
            },
            { error ->
                val msg = error.networkResponse?.let { "Erreur ${it.statusCode}" }
                    ?: "Pas de connexion. Vérifiez votre réseau."
                addBotMessage(msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun addBotMessage(text: String) {
        adapter.addMessage(ChatMessage(text, isUser = false))
        recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        queue.cancelAll(this)
    }
}