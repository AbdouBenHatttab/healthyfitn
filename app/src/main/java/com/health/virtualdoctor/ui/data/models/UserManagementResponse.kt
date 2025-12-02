package com.health.virtualdoctor.ui.data.models

import com.google.gson.annotations.SerializedName

// ==========================================
// USER MANAGEMENT RESPONSE MODELS
// ==========================================

data class UserManagementResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val roles: Set<String>,
    val accountStatus: String,
    val isActivated: Boolean,
    val isEmailVerified: Boolean?,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String?
)

// Wrapper pour les réponses API du User Service
data class UserApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val timestamp: String?
)

// Page Response pour la recherche paginée
data class UserPageResponse(
    val content: List<UserManagementResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

// Request pour la recherche d'utilisateurs
data class UserSearchRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String? = null,
    val page: Int = 0,
    val size: Int = 10
)

// Statistiques des utilisateurs
data class UserStatistics(
    val totalUsers: Long,
    val totalDoctors: Long,
    val totalAdmins: Long
)