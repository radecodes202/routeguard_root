package com.routeguard.android.data

import com.routeguard.android.data.local.TokenStore
import com.routeguard.android.data.remote.AuthApi
import com.routeguard.android.data.remote.dto.AuthResponse
import com.routeguard.android.data.remote.dto.AuthResponse.AuthData
import com.routeguard.android.ui.auth.AuthUiState
import kotlinx.coroutines.flow.*
import retrofit2.Response

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) {

    fun register(email: String, phoneNumber: String?, fullName: String, password: String, passwordConfirmation: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<AuthResponse> = authApi.register(
                com.routeguard.android.data.remote.dto.RegisterRequest(
                    email = email,
                    phone_number = phoneNumber,
                    full_name = fullName,
                    password = password,
                    password_confirmation = passwordConfirmation
                )
            ).await()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    // In a real app, we would save tokens here
                    // For now, we'll just emit success
                    trySend(AuthUiState.RegisterSuccess(
                        message = "Registration successful"
                    ))
                } else {
                    val errorMessage = authResponse?.error?.message ?: "Registration failed"
                    trySend(AuthUiState.RegisterError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                trySend(AuthUiState.RegisterError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.RegisterError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun login(email: String, password: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<AuthResponse> = authApi.login(
                com.routeguard.android.data.remote.dto.LoginRequest(
                    email = email,
                    password = password
                )
            ).await()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    // Save tokens
                    tokenStore.saveTokens(
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes
                    )
                    trySend(AuthUiState.LoginSuccess(
                        user = com.routeguard.android.ui.auth.AuthUiState.User(
                            id = userData.user.id,
                            email = userData.user.email,
                            fullName = userData.user.fullName,
                            role = userData.user.role,
                            reputationScore = userData.user.reputationScore,
                            isActive = userData.user.isActive
                        )
                    ))
                } else {
                    val errorMessage = authResponse?.error?.message ?: "Login failed"
                    trySend(AuthUiState.LoginError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Login failed"
                trySend(AuthUiState.LoginError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.LoginError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun refreshToken(refreshToken: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<AuthResponse> = authApi.refreshToken(
                com.routeguard.android.data.remote.dto.TokenRefreshRequest(
                    refresh_token = refreshToken
                )
            ).await()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    // Save new tokens
                    tokenStore.saveTokens(
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes
                    )
                    trySend(AuthUiState.TokenRefreshSuccess(
                        user = com.routeguard.android.ui.auth.AuthUiState.User(
                            id = userData.user.id,
                            email = userData.user.email,
                            fullName = userData.user.fullName,
                            role = userData.user.role,
                            reputationScore = userData.user.reputationScore,
                            isActive = userData.user.isActive
                        )
                    ))
                } else {
                    val errorMessage = authResponse?.error?.message ?: "Token refresh failed"
                    trySend(AuthUiState.TokenRefreshError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Token refresh failed"
                trySend(AuthUiState.TokenRefreshError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.TokenRefreshError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun logout(refreshToken: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<Void> = authApi.logout(
                com.routeguard.android.data.remote.dto.TokenRefreshRequest(
                    refresh_token = refreshToken
                )
            ).await()

            if (response.isSuccessful) {
                tokenStore.clearTokens()
                trySend(AuthUiState.LogoutSuccess(message = "Logged out successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Logout failed"
                trySend(AuthUiState.LogoutError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.LogoutError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun verifyEmail(token: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<Void> = authApi.verifyEmail(
                com.routeguard.android.data.remote.dto.VerifyEmailRequest(token = token)
            ).await()

            if (response.isSuccessful) {
                trySend(AuthUiState.EmailVerificationSuccess(message = "Email verified successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Email verification failed"
                trySend(AuthUiState.EmailVerificationError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.EmailVerificationError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun forgotPassword(email: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<Void> = authApi.forgotPassword(
                com.routeguard.android.data.remote.dto.ForgotPasswordRequest(email = email)
            ).await()

            if (response.isSuccessful) {
                trySend(AuthUiState.ForgotPasswordSuccess(message = "If the email exists, a password reset link has been sent"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Forgot password failed"
                trySend(AuthUiState.ForgotPasswordError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.ForgotPasswordError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun resetPassword(token: String, newPassword: String): Flow<AuthUiState> = callbackFlow {
        try {
            val response: Response<Void> = authApi.resetPassword(
                com.routeguard.android.data.remote.dto.ResetPasswordRequest(
                    token = token,
                    new_password = newPassword
                )
            ).await()

            if (response.isSuccessful) {
                trySend(AuthUiState.ResetPasswordSuccess(message = "Password has been reset successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Reset password failed"
                trySend(AuthUiState.ResetPasswordError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.ResetPasswordError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun getCurrentUser(): Flow<AuthUiState> = callbackFlow {
        try {
            val accessToken = tokenStore.getAccessToken()
            if (accessToken == null || accessToken.isEmpty()) {
                trySend(AuthUiState.Unauthorized(message = "No access token"))
                return@callbackFlow
            }

            val response: Response<AuthResponse> = authApi.getCurrentUser(
                "Bearer $accessToken"
            ).await()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    trySend(AuthUiState.CurrentUserSuccess(
                        user = com.routeguard.android.ui.auth.AuthUiState.User(
                            id = userData.user.id,
                            email = userData.user.email,
                            fullName = userData.user.fullName,
                            role = userData.user.role,
                            reputationScore = userData.user.reputationScore,
                            isActive = userData.user.isActive
                        )
                    ))
                } else {
                    val errorMessage = authResponse?.error?.message ?: "Failed to get current user"
                    trySend(AuthUiState.CurrentUserError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get current user"
                trySend(AuthUiState.CurrentUserError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.CurrentUserError(message = e.localizedMessage ?: "Network error"))
        }
    }

    /**
     * Register FCM token with the backend for push notifications
     * @param fcmToken The FCM token received from Firebase
     * @return Flow indicating success or failure
     */
    fun registerFcmToken(fcmToken: String): Flow<AuthUiState> = callbackFlow {
        try {
            val accessToken = tokenStore.getAccessToken()
            if (accessToken == null || accessToken.isEmpty()) {
                trySend(AuthUiState.Unauthorized(message = "No access token"))
                return@callbackFlow
            }

            val response: Response<Void> = authApi.registerFcmToken(
                "Bearer $accessToken",
                com.routeguard.android.data.remote.dto.RegisterFcmTokenRequest(fcmToken)
            ).await()

            if (response.isSuccessful) {
                trySend(AuthUiState.TokenRefreshSuccess(
                    // Reuse TokenRefreshSuccess for generic success, or create a new one if needed
                    // For now, we'll just emit a success message through a different approach
                    // Let's create a simple success state
                ))
                // Since we don't have a specific success state for FCM registration,
                // we'll just log it and not emit anything that would confuse the UI
                Log.d("AuthRepository", "FCM token registered successfully")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "FCM token registration failed"
                trySend(AuthUiState.TokenRefreshError(message = errorMessage))
            }
        } catch (e: Exception) {
            trySend(AuthUiState.TokenRefreshError(message = e.localizedMessage ?: "Network error"))
        }
    }