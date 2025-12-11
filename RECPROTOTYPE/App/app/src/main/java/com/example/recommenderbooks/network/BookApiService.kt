package com.example.recommenderbooks.network

import com.example.recommenderbooks.model.Book
import com.example.recommenderbooks.model.AuthRequest
import com.example.recommenderbooks.model.AuthResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

interface BookApiService {
    @GET("books")
    suspend fun getBooks(): List<Book>

    @GET("recommend/{title}")
    suspend fun getRecommendations(@Path("title") title: String): List<Book>

    @POST("login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("register")
    suspend fun register(@Body request: AuthRequest): AuthResponse
}