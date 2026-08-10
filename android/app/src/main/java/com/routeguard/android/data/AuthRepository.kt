package com.routeguard.android.data

import android.util.Log
import com.routeguard.android.data.local.TokenStore
import com.routeguard.android.data.remote.AuthApi
import com.routeguard.android.data.remote.dto.AuthResponse
import com.routeguard.android.ui.auth.AuthUiState
import com.routeguard.android.util.AppConfig
import kotlinx.coroutines.flow.*
import retrofit2.Response
import retrofit2.awaitResponse

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) {

    fun register(email: String, phoneNumber: String?, fullName: String, password: String, passwordConfirmation: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.RegisterSuccess(message = "Demo registration successful"))
            return@flow
        }
        try {
            val response: Response<AuthResponse> = authApi.register(
                com.routeguard.android.data.remote.dto.RegisterRequest(
                    email = email,
                    phone_number = phoneNumber,
                    full_name = fullName,
                    password = password,
                    password_confirmation = passwordConfirmation
                )
            ).awaitResponse()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    emit(AuthUiState.RegisterSuccess(
                        message = "Registration successful"
                    ))
                } else {
                    val errorMessage = authResponse?.error?.message ?: "Registration failed"
                    emit(AuthUiState.RegisterError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                emit(AuthUiState.RegisterError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.RegisterError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun login(email: String, password: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.LoginSuccess(
                user = AuthUiState.User(
                    id = "demo-123",
                    email = email,
                    fullName = "Demo User",
                    role = "user",
                    reputationScore = 5.0,
                    isActive = true
                )
            ))
            return@flow
        }
        try {
            val response: Response<AuthResponse> = authApi.login(
                com.routeguard.android.data.remote.dto.LoginRequest(
                    email = email,
                    password = password
                )
            ).awaitResponse()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    tokenStore.saveTokens(
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        expiresAt = System.currentTimeMillis() + (15 * 60 * 1000)
                    )
                    emit(AuthUiState.LoginSuccess(
                        user = AuthUiState.User(
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
                    emit(AuthUiState.LoginError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Login failed"
                emit(AuthUiState.LoginError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.LoginError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun refreshToken(refreshToken: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.TokenRefreshSuccess(
                user = AuthUiState.User(
                    id = "demo-123",
                    email = "demo@example.com",
                    fullName = "Demo User",
                    role = "user",
                    reputationScore = 5.0,
                    isActive = true
                )
            ))
            return@flow
        }
        try {
            val response: Response<AuthResponse> = authApi.refreshToken(
                com.routeguard.android.data.remote.dto.TokenRefreshRequest(
                    refresh_token = refreshToken
                )
            ).awaitResponse()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    tokenStore.saveTokens(
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        expiresAt = System.currentTimeMillis() + (15 * 60 * 1000)
                    )
                    emit(AuthUiState.TokenRefreshSuccess(
                        user = AuthUiState.User(
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
                    emit(AuthUiState.TokenRefreshError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Token refresh failed"
                emit(AuthUiState.TokenRefreshError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.TokenRefreshError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun logout(refreshToken: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.LogoutSuccess(message = "Demo logout successful"))
            return@flow
        }
        try {
            val response: Response<Void> = authApi.logout(
                com.routeguard.android.data.remote.dto.TokenRefreshRequest(
                    refresh_token = refreshToken
                )
            ).awaitResponse()

            if (response.isSuccessful) {
                tokenStore.clearTokens()
                emit(AuthUiState.LogoutSuccess(message = "Logged out successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Logout failed"
                emit(AuthUiState.LogoutError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.LogoutError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun verifyEmail(token: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.EmailVerificationSuccess(message = "Demo email verification successful"))
            return@flow
        }
        try {
            val response: Response<Void> = authApi.verifyEmail(
                com.routeguard.android.data.remote.dto.VerifyEmailRequest(token = token)
            ).awaitResponse()

            if (response.isSuccessful) {
                emit(AuthUiState.EmailVerificationSuccess(message = "Email verified successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Email verification failed"
                emit(AuthUiState.EmailVerificationError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.EmailVerificationError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun forgotPassword(email: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.ForgotPasswordSuccess(message = "Demo: Reset link sent to $email"))
            return@flow
        }
        try {
            val response: Response<Void> = authApi.forgotPassword(
                com.routeguard.android.data.remote.dto.ForgotPasswordRequest(email = email)
            ).awaitResponse()

            if (response.isSuccessful) {
                emit(AuthUiState.ForgotPasswordSuccess(message = "If the email exists, a password reset link has been sent"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Forgot password failed"
                emit(AuthUiState.ForgotPasswordError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.ForgotPasswordError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun resetPassword(token: String, newPassword: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.ResetPasswordSuccess(message = "Demo password reset successful"))
            return@flow
        }
        try {
            val response: Response<Void> = authApi.resetPassword(
                com.routeguard.android.data.remote.dto.ResetPasswordRequest(
                    token = token,
                    new_password = newPassword
                )
            ).awaitResponse()

            if (response.isSuccessful) {
                emit(AuthUiState.ResetPasswordSuccess(message = "Password has been reset successfully"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Reset password failed"
                emit(AuthUiState.ResetPasswordError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.ResetPasswordError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun getCurrentUser(): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            emit(AuthUiState.CurrentUserSuccess(
                user = AuthUiState.User(
                    id = "demo-123",
                    email = "demo@example.com",
                    fullName = "Demo User",
                    role = "user",
                    reputationScore = 5.0,
                    isActive = true
                )
            ))
            return@flow
        }
        try {
            val accessToken = tokenStore.getAccessToken()
            if (accessToken.isNullOrEmpty()) {
                emit(AuthUiState.Unauthorized(message = "No access token"))
                return@flow
            }

            val response: Response<AuthResponse> = authApi.getCurrentUser(
                "Bearer $accessToken"
            ).awaitResponse()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.data != null) {
                    val userData = authResponse.data!!
                    emit(AuthUiState.CurrentUserSuccess(
                        user = AuthUiState.User(
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
                    emit(AuthUiState.CurrentUserError(message = errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get current user"
                emit(AuthUiState.CurrentUserError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.CurrentUserError(message = e.localizedMessage ?: "Network error"))
        }
    }

    fun registerFcmToken(fcmToken: String): Flow<AuthUiState> = flow {
        if (AppConfig.DEMO_MODE) {
            Log.d("AuthRepository", "Demo: FCM token registered: $fcmToken")
            return@flow
        }
        try {
            val accessToken = tokenStore.getAccessToken()
            if (accessToken.isNullOrEmpty()) {
                emit(AuthUiState.Unauthorized(message = "No access token"))
                return@flow
            }

            val response: Response<Void> = authApi.registerFcmToken(
                "Bearer $accessToken",
                com.routeguard.android.data.remote.dto.RegisterFcmTokenRequest(fcmToken)
            ).awaitResponse()

            if (response.isSuccessful) {
                Log.d("AuthRepository", "FCM token registered successfully")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "FCM token registration failed"
                emit(AuthUiState.TokenRefreshError(message = errorMessage))
            }
        } catch (e: Exception) {
            emit(AuthUiState.TokenRefreshError(message = e.localizedMessage ?: "Network error"))
        }
    }
}
