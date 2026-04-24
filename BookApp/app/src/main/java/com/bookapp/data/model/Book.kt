package com.bookapp.data.model

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter

@JsonAdapter(BookDeserializer::class)
data class Book(
    val id: String? = null,
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val categoryId: String? = null,
    val totalChapters: Int? = null,
    val totalPages: Int? = null,
    val views: Int? = null,
    val avgRating: Double? = null,
    val featured: Boolean? = null,
    val tags: List<String>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

class BookDeserializer : JsonDeserializer<Book> {
    override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): Book {
        val obj = json.asJsonObject
        
        return Book(
            id = obj.getAsStringOrNull("id"),
            title = obj.getAsStringOrNull("title"),
            author = obj.getAsStringOrNull("author"),
            description = obj.getAsStringOrNull("description"),
            coverImage = obj.getAsStringOrNull("coverImage") ?: obj.getAsStringOrNull("cover_image"),
            categoryId = obj.getAsStringOrNull("categoryId") ?: obj.getAsStringOrNull("category_id"),
            totalChapters = obj.getAsIntOrNull("totalChapters") ?: obj.getAsIntOrNull("total_chapters"),
            totalPages = obj.getAsIntOrNull("totalPages") ?: obj.getAsIntOrNull("total_pages"),
            views = obj.getAsIntOrNull("views"),
            avgRating = obj.getAsDoubleOrNull("avgRating") ?: obj.getAsDoubleOrNull("avg_rating"),
            featured = obj.getAsBooleanOrNull("featured"),
            tags = null, // Handle separately if needed
            createdAt = obj.getAsStringOrNull("createdAt") ?: obj.getAsStringOrNull("created_at"),
            updatedAt = obj.getAsStringOrNull("updatedAt") ?: obj.getAsStringOrNull("updated_at")
        )
    }
    
    private fun JsonObject.getAsStringOrNull(key: String): String? {
        return if (has(key) && !get(key).isJsonNull) get(key).asString else null
    }
    
    private fun JsonObject.getAsIntOrNull(key: String): Int? {
        return if (has(key) && !get(key).isJsonNull) get(key).asInt else null
    }
    
    private fun JsonObject.getAsDoubleOrNull(key: String): Double? {
        return if (has(key) && !get(key).isJsonNull) get(key).asDouble else null
    }
    
    private fun JsonObject.getAsBooleanOrNull(key: String): Boolean? {
        return if (has(key) && !get(key).isJsonNull) get(key).asBoolean else null
    }
}