package com.health.virtualdoctor.ui.data.models


data class AuthResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val roles: Set<String>,
    val isActivated: Boolean
)