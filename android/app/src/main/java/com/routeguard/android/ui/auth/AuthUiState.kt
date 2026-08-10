package com.routeguard.android.ui.auth

sealed class AuthUiState {
    data class RegisterSuccess(val message: String) : AuthUiState()
    data class RegisterError(val message: String) : AuthUiState()
    object RegisterLoading : AuthUiState()

    data class LoginSuccess(val user: User) : AuthUiState()
    data class LoginError(val message: String) : AuthUiState()
    object LoginLoading : AuthUiState()

    data class TokenRefreshSuccess(val user: User) : AuthUiState()
    data class TokenRefreshError(val message: String) : AuthUiState()
    object TokenRefreshLoading : AuthUiState()

    data class LogoutSuccess(val message: String) : AuthUiState()
    data class LogoutError(val message: String) : AuthUiState()
    object LogoutLoading : AuthUiState()

    data class EmailVerificationSuccess(val message: String) : AuthUiState()
    data class EmailVerificationError(val message: String) : AuthUiState()
    object EmailVerificationLoading : AuthUiState()

    data class ForgotPasswordSuccess(val message: String) : AuthUiState()
    data class ForgotPasswordError(val message: String) : AuthUiState()
    object ForgotPasswordLoading : AuthUiState()

    data class ResetPasswordSuccess(val message: String) : AuthUiState()
    data class ResetPasswordError(val message: String) : AuthUiState()
    object ResetPasswordLoading : AuthUiState()

    data class CurrentUserSuccess(val user: User) : AuthUiState()
    data class CurrentUserError(val message: String) : AuthUiState()
    object CurrentUserLoading : AuthUiState()

    data class Unauthorized(val message: String) : AuthUiState()

    data class User(
        val id: String,
        val email: String,
        val fullName: String,
        val role: String,
        val reputationScore: Double,
        val isActive: Boolean
    )
}