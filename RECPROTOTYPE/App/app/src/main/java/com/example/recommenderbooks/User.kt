package com.example.recommenderbooks

import com.google.firebase.Timestamp

data class User(
    val name: String = "",
    val email: String = "",
    val password: String = "", // Note: Storing passwords in Firestore is generally not recommended for security reasons, but I'll include it as requested.
    val createdAt: Timestamp = Timestamp.now()
)
