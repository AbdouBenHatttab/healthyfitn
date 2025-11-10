package com.health.virtualdoctor.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.auth.LoginActivity
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.DoctorPendingResponse
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvPendingDoctors: RecyclerView
    private lateinit var rvActivatedDoctors: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardPendingCount: CardView
    private lateinit var cardActivatedCount: CardView
    private lateinit var cardTotalDoctors: CardView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvActivatedCount: TextView
    private lateinit var tvTotalDoctors: TextView

    private var pendingDoctorsAdapter: PendingDoctorsAdapter? = null
    private var activatedDoctorsAdapter: ActivatedDoctorsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        tokenManager = TokenManager(this)

        // Vérifier que l'utilisateur est admin
        if (tokenManager.getUserRole() != "ADMIN") {
            Toast.makeText(this, "❌ Accès refusé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupTabs()
        setupRecyclerViews()
        loadStatistics()
        loadPendingDoctors()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvPendingDoctors = findViewById(R.id.rvPendingDoctors)
        rvActivatedDoctors = findViewById(R.id.rvActivatedDoctors)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        cardPendingCount = findViewById(R.id.cardPendingCount)
        cardActivatedCount = findViewById(R.id.cardActivatedCount)
        cardTotalDoctors = findViewById(R.id.cardTotalDoctors)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvActivatedCount = findViewById(R.id.tvActivatedCount)
        tvTotalDoctors = findViewById(R.id.tvTotalDoctors)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "👨‍⚕️ Admin Dashboard"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshData()
                true
            }
            R.id.action_manage_users -> {
                // ✅ Ouvrir l'activité de gestion des utilisateurs
                val intent = Intent(this, UserManagementActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPendingDoctors()
                    1 -> showActivatedDoctors()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerViews() {
        rvPendingDoctors.layoutManager = LinearLayoutManager(this)
        rvActivatedDoctors.layoutManager = LinearLayoutManager(this)
    }

    private fun showPendingDoctors() {
        rvPendingDoctors.visibility = View.VISIBLE
        rvActivatedDoctors.visibility = View.GONE
        loadPendingDoctors()
    }

    private fun showActivatedDoctors() {
        rvPendingDoctors.visibility = View.GONE
        rvActivatedDoctors.visibility = View.VISIBLE
        loadActivatedDoctors()
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                // Compter les doctors en attente
                val pendingResponse = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .getPendingDoctorsCount(token)

                if (pendingResponse.isSuccessful) {
                    val count = pendingResponse.body()?.get("count") ?: 0L
                    tvPendingCount.text = count.toString()
                }

                // Charger tous les doctors pour le total
                loadTotalDoctorsCount()

            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error loading statistics: ${e.message}")
            }
        }
    }

    private fun loadTotalDoctorsCount() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val pendingResponse = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .getPendingDoctors(token)

                val activatedResponse = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .getActivatedDoctors(token)

                val pendingCount = if (pendingResponse.isSuccessful) {
                    pendingResponse.body()?.size ?: 0
                } else 0

                val activatedCount = if (activatedResponse.isSuccessful) {
                    activatedResponse.body()?.size ?: 0
                } else 0

                runOnUiThread {
                    tvPendingCount.text = pendingCount.toString()
                    tvActivatedCount.text = activatedCount.toString()
                    tvTotalDoctors.text = (pendingCount + activatedCount).toString()
                }

            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error loading total count: ${e.message}")
            }
        }
    }

    private fun loadPendingDoctors() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .getPendingDoctors(token)

                runOnUiThread {
                    showLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        val doctors = response.body()!!

                        if (doctors.isEmpty()) {
                            showEmptyState(true, "Aucun médecin en attente")
                        } else {
                            showEmptyState(false, "")
                            pendingDoctorsAdapter = PendingDoctorsAdapter(doctors) { doctor, action ->
                                handleDoctorAction(doctor, action)
                            }
                            rvPendingDoctors.adapter = pendingDoctorsAdapter
                        }
                    } else {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e("AdminDashboard", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadActivatedDoctors() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .getActivatedDoctors(token)

                runOnUiThread {
                    showLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        val doctors = response.body()!!

                        if (doctors.isEmpty()) {
                            showEmptyState(true, "Aucun médecin activé")
                        } else {
                            showEmptyState(false, "")
                            activatedDoctorsAdapter = ActivatedDoctorsAdapter(doctors)
                            rvActivatedDoctors.adapter = activatedDoctorsAdapter
                        }
                    } else {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e("AdminDashboard", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleDoctorAction(doctor: DoctorPendingResponse, action: String) {
        when (action) {
            "approve" -> showApprovalDialog(doctor)
            "reject" -> showRejectionDialog(doctor)
            "view" -> showDoctorDetailsDialog(doctor)
        }
    }

    private fun showApprovalDialog(doctor: DoctorPendingResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle("✅ Approuver le médecin")
            .setMessage("Voulez-vous approuver le Dr. ${doctor.fullName} ?")
            .setPositiveButton("Approuver") { _, _ ->
                approveDoctorRequest(doctor.doctorId, "APPROVE", null)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showRejectionDialog(doctor: DoctorPendingResponse) {
        val input = EditText(this)
        input.hint = "Raison du rejet (optionnel)"

        MaterialAlertDialogBuilder(this)
            .setTitle("❌ Rejeter le médecin")
            .setMessage("Voulez-vous rejeter le Dr. ${doctor.fullName} ?")
            .setView(input)
            .setPositiveButton("Rejeter") { _, _ ->
                val reason = input.text.toString().ifEmpty { "Credentials could not be verified" }
                approveDoctorRequest(doctor.doctorId, "REJECT", reason)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showDoctorDetailsDialog(doctor: DoctorPendingResponse) {
        val message = """
            👤 Nom: ${doctor.fullName}
            📧 Email: ${doctor.email}
            🆔 Licence: ${doctor.medicalLicenseNumber}
            🏥 Spécialisation: ${doctor.specialization}
            🏨 Hôpital: ${doctor.hospitalAffiliation}
            📅 Expérience: ${doctor.yearsOfExperience} ans
            📆 Date d'inscription: ${doctor.registrationDate}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("📋 Détails du médecin")
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun approveDoctorRequest(doctorId: String, action: String, notes: String?) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val request = mapOf(
                    "doctorId" to doctorId,
                    "action" to action,
                    "notes" to (notes ?: "")
                )

                val response = RetrofitClient.getDoctorService(this@AdminDashboardActivity)
                    .activateDoctor(token, request)

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "✅ Action effectuée avec succès",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshData()
                    } else {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("AdminDashboard", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun refreshData() {
        loadStatistics()
        when (tabLayout.selectedTabPosition) {
            0 -> loadPendingDoctors()
            1 -> loadActivatedDoctors()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean, message: String) {
        if (show) {
            tvEmptyState.text = message
            tvEmptyState.visibility = View.VISIBLE
            rvPendingDoctors.visibility = View.GONE
            rvActivatedDoctors.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                logout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun logout() {
        tokenManager.clearTokens()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}