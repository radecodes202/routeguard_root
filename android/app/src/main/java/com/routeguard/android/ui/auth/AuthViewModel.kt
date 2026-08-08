package com.routeguard.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeguard.android.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    val uiState = androidx.lifecycle.MutableStateFlow<AuthUiState>(AuthUiState.RegisterLoading)

    fun register(email: String, phoneNumber: String?, fullName: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.RegisterLoading
            authRepository.register(email, phoneNumber, fullName, password, passwordConfirmation)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.LoginLoading
            authRepository.login(email, password)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun refreshToken(refreshToken: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.TokenRefreshLoading
            authRepository.refreshToken(refreshToken)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun logout(refreshToken: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.LogoutLoading
            authRepository.logout(refreshToken)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun verifyEmail(token: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.EmailVerificationLoading
            authRepository.verifyEmail(token)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.ForgotPasswordLoading
            authRepository.forgotPassword(email)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        viewModelScope.launch {
            uiState.value = AuthUiState.ResetPasswordLoading
            authRepository.resetPassword(token, newPassword)
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            uiState.value = AuthUiState.CurrentUserLoading
            authRepository.getCurrentUser()
                .collectLatest { state ->
                    uiState.value = state
                }
        }
    }

    /**
     * Register FCM token with backend for push notifications
     */
    fun registerFcmToken(fcmToken: String) {
        viewModelScope.launch {
            authRepository.registerFcmToken(fcmToken)
                .collectLatest { state ->
                    // Handle FCM token registration result if needed
                    // For now, we'll just log it
                    if (state is AuthUiState.TokenRefreshSuccess) {
                        // Token registered successfully
                    } else if (state is AuthUiState.TokenRefreshError) {
                        // Handle error
                    }
                }
        }
    }
}