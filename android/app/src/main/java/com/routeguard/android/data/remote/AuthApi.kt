package com.routeguard.android.data.remote

import com.routeguard.android.data.remote.dto.AuthResponse
import com.routeguard.android.data.remote.dto.ForgotPasswordRequest
import com.routeguard.android.data.remote.dto.LoginRequest
import com.routeguard.android.data.remote.dto.RegisterRequest
import com.routeguard.android.data.remote.dto.ResetPasswordRequest
import com.routeguard.android.data.remote.dto.TokenRefreshRequest
import com.routeguard.android.data.remote.dto.VerifyEmailRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("/auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("/auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("/auth/refresh")
    fun refreshToken(@Body request: TokenRefreshRequest): Call<AuthResponse>

    @POST("/auth/logout")
    fun logout(@Body request: TokenRefreshRequest): Call<Void>

    @POST("/auth/verify-email")
    fun verifyEmail(@Body request: VerifyEmailRequest): Call<Void>

    @POST("/auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<Void>

    @POST("/auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Void>

    @GET("/auth/me")
    fun getCurrentUser(@Header("Authorization") authHeader: String): Call<AuthResponse>

    @POST("/auth/fcm-token")
    fun registerFcmToken(
        @Header("Authorization") authHeader: String,
        @Body request: RegisterFcmTokenRequest
    ): Call<Void>
}