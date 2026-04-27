package com.example.recommenderbooks.model

import com.google.firebase.firestore.PropertyName

data class Library(
    @get:PropertyName("libraryId") @set:PropertyName("libraryId") var libraryId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("books") @set:PropertyName("books") var books: List<Map<String, String>> = emptyList()
)
