package com.routeguard.android.data.remote.dto

data class RegisterRequest(
    val email: String,
    val phone_number: String?,
    val full_name: String,
    val password: String,
    val password_confirmation: String
)