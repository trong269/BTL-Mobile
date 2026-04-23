package com.bookapp.data.api

import com.bookapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class AITextRequest(
    val text: String,
    val book_name: String = "",
    val context_before: String = "",
    val context_after: String = ""
)

data class AITextResponse(
    val result: String,
    val task: String
)

interface AIService {
    @POST("api/ai/explain")
    fun explain(@Body request: AITextRequest): Call<AITextResponse>

    @POST("api/ai/summarize")
    fun summarize(@Body request: AITextRequest): Call<AITextResponse>
}

object AIRetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: AIService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AIService::class.java)
    }
}
