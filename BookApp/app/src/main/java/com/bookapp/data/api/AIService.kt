package com.bookapp.data.api

import com.bookapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

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

data class AIQaChatMessage(
    val role: String,
    val content: String
)

data class AIQaRequest(
    val question: String,
    val book_name: String = "",
    val current_chapter_title: String = "",
    val context_chunks: List<String> = emptyList(),
    val chat_history: List<AIQaChatMessage> = emptyList()
)

data class AIQaResponse(
    val result: String,
    val task: String
)

data class AIQaSuggestionsRequest(
    val book_name: String = "",
    val current_chapter_title: String = "",
    val chapter_text: String,
    val max_questions: Int = 5
)

data class AIQaSuggestionsResponse(
    val questions: List<String>,
    val task: String
)

interface AIService {
    @POST("api/ai/explain")
    fun explain(@Body request: AITextRequest): Call<AITextResponse>

    @POST("api/ai/summarize")
    fun summarize(@Body request: AITextRequest): Call<AITextResponse>

    @POST("api/ai/qa")
    fun qa(@Body request: AIQaRequest): Call<AIQaResponse>

    @POST("api/ai/suggestions")
    fun suggestions(@Body request: AIQaSuggestionsRequest): Call<AIQaSuggestionsResponse>
}

object AIRetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // BUG-6 FIX: Add explicit timeouts so that slow LLM / network conditions do not
    // leave the UI frozen on the loading spinner indefinitely. The AI service can take
    // up to ~15 s for a cold-start LLM call, so we use a generous 45 s read timeout
    // while keeping the connect timeout at 30 s for faster failure on unreachable hosts.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: AIService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AIService::class.java)
    }

    val streamClient: OkHttpClient
        get() = okHttpClient
}
