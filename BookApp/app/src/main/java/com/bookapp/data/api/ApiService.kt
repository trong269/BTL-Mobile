package com.bookapp.data.api

import com.bookapp.data.model.Book
import com.bookapp.data.model.User
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

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

interface ApiService {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<User>

    @GET("api/books")
    fun getAllBooks(): Call<List<Book>>

    @GET("api/books/top-week")
    fun getTopBooksWeek(): Call<List<Book>>

    @GET("api/books/top-month")
    fun getTopBooksMonth(): Call<List<Book>>
}