package com.example.recommenderbooks

import com.google.firebase.Timestamp

data class User(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
