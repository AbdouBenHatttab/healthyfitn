package com.health.virtualdoctor.ui.utils


import android.content.Context
import android.content.Intent
import com.health.virtualdoctor.ui.auth.LoginActivity
// TODO: Import your actual activities when created
// import com.health.virtualdoctor.ui.user.UserHomeActivity
// import com.health.virtualdoctor.ui.doctor.DoctorDashboardActivity
// import com.health.virtualdoctor.ui.admin.AdminDashboardActivity

object RoleManager {

    fun navigateByRole(context: Context, role: String) {
        val intent = when (role) {
            "USER" -> {
                // TODO: Replace with actual UserHomeActivity
                // Intent(context, UserHomeActivity::class.java)
                Intent(context, LoginActivity::class.java).apply {
                    putExtra("message", "🏠 User Home (à implémenter)")
                }
            }
            "DOCTOR" -> {
                // TODO: Replace with actual DoctorDashboardActivity
                // Intent(context, DoctorDashboardActivity::class.java)
                Intent(context, LoginActivity::class.java).apply {
                    putExtra("message", "👨‍⚕️ Doctor Dashboard (à implémenter)")
                }
            }
            "ADMIN" -> {
                // TODO: Replace with actual AdminDashboardActivity
                // Intent(context, AdminDashboardActivity::class.java)
                Intent(context, LoginActivity::class.java).apply {
                    putExtra("message", "⚙️ Admin Dashboard (à implémenter)")
                }
            }
            else -> {
                Intent(context, LoginActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun getRoleDisplayName(role: String): String {
        return when (role) {
            "USER" -> "Patient"
            "DOCTOR" -> "Médecin"
            "ADMIN" -> "Administrateur"
            else -> "Inconnu"
        }
    }

    fun getRoleIcon(role: String): String {
        return when (role) {
            "USER" -> "👤"
            "DOCTOR" -> "👨‍⚕️"
            "ADMIN" -> "⚙️"
            else -> "❓"
        }
    }
}