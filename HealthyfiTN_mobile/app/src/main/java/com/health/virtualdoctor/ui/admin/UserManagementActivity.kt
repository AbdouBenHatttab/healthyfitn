package com.health.virtualdoctor.ui.admin

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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.data.models.UserManagementResponse
import com.health.virtualdoctor.ui.data.models.UserSearchRequest
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardTotalUsers: CardView
    private lateinit var cardTotalDoctors: CardView
    private lateinit var cardTotalAdmins: CardView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalDoctors: TextView
    private lateinit var tvTotalAdmins: TextView
    private lateinit var chipGroupRoles: ChipGroup
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button

    private var usersAdapter: UsersManagementAdapter? = null
    private var currentRoleFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        tokenManager = TokenManager(this)

        // Vérifier que l'utilisateur est admin
        if (tokenManager.getUserRole() != "ADMIN") {
            Toast.makeText(this, "❌ Accès refusé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupRoleFilters()
        setupSearch()
        loadStatistics()
        loadAllUsers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvUsers = findViewById(R.id.rvUsers)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        cardTotalUsers = findViewById(R.id.cardTotalUsers)
        cardTotalDoctors = findViewById(R.id.cardTotalDoctors)
        cardTotalAdmins = findViewById(R.id.cardTotalAdmins)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalDoctors = findViewById(R.id.tvTotalDoctors)
        tvTotalAdmins = findViewById(R.id.tvTotalAdmins)
        chipGroupRoles = findViewById(R.id.chipGroupRoles)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "👥 Gestion des Utilisateurs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_user_management, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                refreshData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        rvUsers.layoutManager = LinearLayoutManager(this)
    }

    private fun setupRoleFilters() {
        // Chip "Tous"
        val chipAll: Chip = findViewById(R.id.chipAll)
        chipAll.setOnClickListener {
            currentRoleFilter = null
            loadAllUsers()
        }

        // Chip "Users"
        val chipUsers: Chip = findViewById(R.id.chipUsers)
        chipUsers.setOnClickListener {
            currentRoleFilter = "USER"
            loadUsersByRole("USER")
        }

        // Chip "Doctors"
        val chipDoctors: Chip = findViewById(R.id.chipDoctors)
        chipDoctors.setOnClickListener {
            currentRoleFilter = "DOCTOR"
            loadUsersByRole("DOCTOR")
        }

        // Chip "Admins"
        val chipAdmins: Chip = findViewById(R.id.chipAdmins)
        chipAdmins.setOnClickListener {
            currentRoleFilter = "ADMIN"
            loadUsersByRole("ADMIN")
        }
    }

    private fun setupSearch() {
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
            } else {
                Toast.makeText(this, "Entrez un terme de recherche", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getUserService(this@UserManagementActivity)
                    .getUserStatistics(token)

                if (response.isSuccessful && response.body()?.data != null) {
                    val stats = response.body()!!.data!!

                    runOnUiThread {
                        tvTotalUsers.text = stats.totalUsers.toString()
                        tvTotalDoctors.text = stats.totalDoctors.toString()
                        tvTotalAdmins.text = stats.totalAdmins.toString()
                    }
                }

            } catch (e: Exception) {
                Log.e("UserManagement", "Error loading statistics: ${e.message}")
            }
        }
    }

    private fun loadAllUsers() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getUserService(this@UserManagementActivity)
                    .getAllUsers(token)

                runOnUiThread {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.data != null) {
                        val users = response.body()!!.data!!

                        if (users.isEmpty()) {
                            showEmptyState(true, "Aucun utilisateur trouvé")
                        } else {
                            showEmptyState(false, "")
                            usersAdapter = UsersManagementAdapter(users) { user, action ->
                                handleUserAction(user, action)
                            }
                            rvUsers.adapter = usersAdapter
                        }
                    } else {
                        Toast.makeText(
                            this@UserManagementActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e("UserManagement", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@UserManagementActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadUsersByRole(role: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getUserService(this@UserManagementActivity)
                    .getUsersByRole(token, role)

                runOnUiThread {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.data != null) {
                        val users = response.body()!!.data!!

                        if (users.isEmpty()) {
                            showEmptyState(true, "Aucun utilisateur avec le rôle $role")
                        } else {
                            showEmptyState(false, "")
                            usersAdapter = UsersManagementAdapter(users) { user, action ->
                                handleUserAction(user, action)
                            }
                            rvUsers.adapter = usersAdapter
                        }
                    } else {
                        Toast.makeText(
                            this@UserManagementActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e("UserManagement", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@UserManagementActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun searchUsers(query: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val searchRequest = UserSearchRequest(
                    email = query,
                    firstName = query,
                    lastName = query,
                    role = currentRoleFilter,
                    page = 0,
                    size = 50
                )

                val response = RetrofitClient.getUserService(this@UserManagementActivity)
                    .searchUsers(token, searchRequest)

                runOnUiThread {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.data != null) {
                        val pageResponse = response.body()!!.data!!
                        val users = pageResponse.content

                        if (users.isEmpty()) {
                            showEmptyState(true, "Aucun résultat pour '$query'")
                        } else {
                            showEmptyState(false, "")
                            usersAdapter = UsersManagementAdapter(users) { user, action ->
                                handleUserAction(user, action)
                            }
                            rvUsers.adapter = usersAdapter
                        }
                    } else {
                        Toast.makeText(
                            this@UserManagementActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e("UserManagement", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@UserManagementActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleUserAction(user: UserManagementResponse, action: String) {
        when (action) {
            "view" -> showUserDetailsDialog(user)
            "delete" -> showDeleteDialog(user)
        }
    }

    private fun showUserDetailsDialog(user: UserManagementResponse) {
        val message = """
            👤 Nom: ${user.fullName}
            📧 Email: ${user.email}
            📱 Téléphone: ${user.phoneNumber ?: "N/A"}
            🎭 Rôles: ${user.roles.joinToString(", ")}
            📊 Statut: ${user.accountStatus}
            ✅ Activé: ${if (user.isActivated) "Oui" else "Non"}
            ✉️ Email vérifié: ${if (user.isEmailVerified == true) "Oui" else "Non"}
            📅 Créé le: ${user.createdAt}
            🕐 Dernière connexion: ${user.lastLoginAt ?: "Jamais"}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("📋 Détails de l'utilisateur")
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun showDeleteDialog(user: UserManagementResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Supprimer l'utilisateur")
            .setMessage("Voulez-vous vraiment supprimer ${user.fullName} ?\n\nCette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteUser(userId: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"

                val response = RetrofitClient.getUserService(this@UserManagementActivity)
                    .deleteUser(token, userId)

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@UserManagementActivity,
                            "✅ Utilisateur supprimé avec succès",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshData()
                    } else {
                        Toast.makeText(
                            this@UserManagementActivity,
                            "❌ Erreur ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("UserManagement", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@UserManagementActivity,
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun refreshData() {
        loadStatistics()
        when (currentRoleFilter) {
            null -> loadAllUsers()
            else -> loadUsersByRole(currentRoleFilter!!)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean, message: String) {
        if (show) {
            tvEmptyState.text = message
            tvEmptyState.visibility = View.VISIBLE
            rvUsers.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvUsers.visibility = View.VISIBLE
        }
    }
}