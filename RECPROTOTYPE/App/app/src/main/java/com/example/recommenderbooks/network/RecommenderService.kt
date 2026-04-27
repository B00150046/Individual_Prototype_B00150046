package com.example.recommenderbooks.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class BookMetadata(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String,
    @SerializedName("Author") val author: String? = null,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int? = null,
    @SerializedName("ISBN") val isbn: String? = null,
    @SerializedName("PublishDate") val publishDate: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("ImageUrl") val imageUrl: String? = null
)

data class AllBooksResponse(
    @SerializedName("books") val books: List<String>
)

data class OuterResponse(
    @SerializedName("recommendations") val inner: InnerResponse
)

data class InnerResponse(
    @SerializedName("recommendations") val recommendations: List<String>
)

interface RecommenderService {
    @GET("getRec/{user_id}")
    suspend fun getRecommendation(
        @Path("user_id") userId: String,
        @Query("book_name") bookName: String
    ): OuterResponse

    @GET("getAllBooks")
    suspend fun getAllBooks(): AllBooksResponse

    @GET("getBookDetails")
    suspend fun getBookDetails(@Query("book_name") bookName: String): BookMetadata

    @POST("interact")
    suspend fun interact(
        @Query("user_id") userId: String,
        @Query("book_id") bookId: Int,
        @Query("action") action: String
    ): Any
}
