package com.example.recommenderbooks.model

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user_id: String? = null
)
