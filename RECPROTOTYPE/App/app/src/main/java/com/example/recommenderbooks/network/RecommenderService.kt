package com.example.recommenderbooks.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

// Represents a single Book object from the server (remains for other potential uses)
data class Book(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String,
    @SerializedName("Author") val author: String? = null,
    @SerializedName("Image") val imageUrl: String? = null,
    @SerializedName("Rating") val rating: Float? = null
)

// A wrapper class for the getAllBooks endpoint, now expecting a list of strings.
data class AllBooksResponse(
    @SerializedName("books") val books: List<String>
)

// Corresponds to the outer JSON object for a single recommendation: {"recommendations": {...}}
data class OuterResponse(
    @SerializedName("recommendations") val inner: InnerResponse
)

// Corresponds to the inner JSON object: {"recommendations": ["book1", "book2", ...]}
data class InnerResponse(
    @SerializedName("recommendations") val recommendations: List<String>
)

// The service interface
interface RecommenderService {
    @GET("getRec/{command}")
    suspend fun getRecommendation(@Path("command") command: String): OuterResponse

    // The return type is now the wrapper class containing a list of strings
    @GET("getAllBooks")
    suspend fun getAllBooks(): AllBooksResponse
}
