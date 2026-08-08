package com.routeguard.android.data.remote.dto

data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)