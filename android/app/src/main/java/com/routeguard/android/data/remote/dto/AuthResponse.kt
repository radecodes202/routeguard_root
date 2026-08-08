package com.routeguard.android.data.remote.dto

data class AuthResponse(
    val success: Boolean,
    val data: AuthData?,
    val error: AuthError?
) {
    data class AuthData(
        val accessToken: String,
        val refreshToken: String,
        val user: User
    ) {
        data class User(
            val id: String,
            val email: String,
            val fullName: String,
            val role: String,
            val reputationScore: Double,
            val isActive: Boolean
        )
    }

    data class AuthError(
        val message: String,
        val details: Map<String, List<String>>? = null
    )
}