package com.health.virtualdoctor.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.models.UserManagementResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UsersManagementAdapter(
    private val users: List<UserManagementResponse>,
    private val onAction: (UserManagementResponse, String) -> Unit
) : RecyclerView.Adapter<UsersManagementAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvUserRole: TextView = view.findViewById(R.id.tvUserRole)
        val tvPhoneNumber: TextView = view.findViewById(R.id.tvPhoneNumber)
        val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
        val tvLastLogin: TextView = view.findViewById(R.id.tvLastLogin)
        val chipStatus: Chip = view.findViewById(R.id.chipStatus)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        holder.tvUserName.text = "👤 ${user.fullName}"
        holder.tvUserEmail.text = "📧 ${user.email}"

        // Afficher les rôles
        val rolesText = user.roles.joinToString(", ")
        holder.tvUserRole.text = "🎭 $rolesText"

        holder.tvPhoneNumber.text = "📱 ${user.phoneNumber ?: "N/A"}"

        // Format dates
        holder.tvCreatedAt.text = "📅 Créé: ${formatDate(user.createdAt)}"
        holder.tvLastLogin.text = "🕐 Dernière connexion: ${formatDate(user.lastLoginAt)}"

        // Status chip based on account status
        when (user.accountStatus) {
            "ACTIVE" -> {
                holder.chipStatus.text = "✅ ACTIF"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
            }
            "INACTIVE" -> {
                holder.chipStatus.text = "❌ INACTIF"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
            }
            "LOCKED" -> {
                holder.chipStatus.text = "🔒 VERROUILLÉ"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
            }
            "SUSPENDED" -> {
                holder.chipStatus.text = "⏸️ SUSPENDU"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_dark)
            }
            else -> {
                holder.chipStatus.text = user.accountStatus
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
            }
        }
        holder.chipStatus.setTextColor(Color.WHITE)

        // Button listeners
        holder.btnViewDetails.setOnClickListener {
            onAction(user, "view")
        }

        holder.btnDelete.setOnClickListener {
            onAction(user, "delete")
        }
    }

    override fun getItemCount() = users.size

    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString.isNullOrEmpty()) {
                "N/A"
            } else {
                val dateTime = LocalDateTime.parse(dateString)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                dateTime.format(formatter)
            }
        } catch (e: Exception) {
            dateString ?: "N/A"
        }
    }
}