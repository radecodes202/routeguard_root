package com.routeguard.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeguard.android.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.RegisterLoading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(email: String, phoneNumber: String?, fullName: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.RegisterLoading
            authRepository.register(email, phoneNumber, fullName, password, passwordConfirmation)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LoginLoading
            authRepository.login(email, password)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun refreshToken(refreshToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.TokenRefreshLoading
            authRepository.refreshToken(refreshToken)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun logout(refreshToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LogoutLoading
            authRepository.logout(refreshToken)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun verifyEmail(token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.EmailVerificationLoading
            authRepository.verifyEmail(token)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.ForgotPasswordLoading
            authRepository.forgotPassword(email)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.ResetPasswordLoading
            authRepository.resetPassword(token, newPassword)
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.CurrentUserLoading
            authRepository.getCurrentUser()
                .collectLatest { state ->
                    _uiState.value = state
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
