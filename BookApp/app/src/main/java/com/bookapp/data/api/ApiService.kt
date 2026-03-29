package com.bookapp.data.api

import com.bookapp.data.model.Book
import com.bookapp.data.model.Category
import com.bookapp.data.model.Comment
import com.bookapp.data.model.Review
import com.bookapp.data.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val user: User,
    val role: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class ToggleFavoriteRequest(
    val userId: String,
    val bookId: String
)

data class AddReviewRequest(
    val bookId: String,
    val userId: String,
    val rating: Int,
    val review: String
)

data class AddCommentRequest(
    val bookId: String,
    val userId: String,
    val content: String
)

data class FavoriteStatusResponse(
    val favorited: Boolean
)

data class ToggleFavoriteResponse(
    val favorited: Boolean,
    val message: String
)

interface ApiService {

    // ========== AUTH ==========
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<User>

    // ========== BOOKS ==========
    @GET("api/books")
    fun getAllBooks(): Call<List<Book>>

    @GET("api/books/featured")
    fun getFeaturedBooks(): Call<List<Book>>

    @GET("api/books/new")
    fun getNewBooks(): Call<List<Book>>

    @GET("api/books/search")
    fun searchBooks(@Query("q") keyword: String): Call<List<Book>>

    @GET("api/books/category/{categoryId}")
    fun getBooksByCategory(@Path("categoryId") categoryId: String): Call<List<Book>>

    @GET("api/books/{id}")
    fun getBookById(@Path("id") id: String): Call<Book>

    // ========== CATEGORIES ==========
    @GET("api/categories")
    fun getAllCategories(): Call<List<Category>>

    // ========== REVIEWS ==========
    @GET("api/reviews/book/{bookId}")
    fun getReviewsByBook(@Path("bookId") bookId: String): Call<List<Review>>

    @POST("api/reviews")
    fun addReview(@Body request: AddReviewRequest): Call<Review>

    // ========== COMMENTS ==========
    @GET("api/comments/book/{bookId}")
    fun getCommentsByBook(@Path("bookId") bookId: String): Call<List<Comment>>

    @POST("api/comments")
    fun addComment(@Body request: AddCommentRequest): Call<Comment>

    // ========== FAVORITES ==========
    @POST("api/favorites/toggle")
    fun toggleFavorite(@Body request: ToggleFavoriteRequest): Call<ToggleFavoriteResponse>

    @GET("api/favorites/check")
    fun checkFavorite(
        @Query("userId") userId: String,
        @Query("bookId") bookId: String
    ): Call<FavoriteStatusResponse>
}