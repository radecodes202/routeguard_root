package com.routeguard.android.data.remote.dto

/**
 * Request to register FCM token with the backend
 */
data class RegisterFcmTokenRequest(
    val fcm_token: String
)