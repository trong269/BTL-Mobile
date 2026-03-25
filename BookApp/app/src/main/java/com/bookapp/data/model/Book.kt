package com.bookapp.data.model

data class Book(
    val id: String? = null,
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
    val categoryId: String? = null,
    val totalChapters: Int? = null,
    val totalPages: Int? = null,
    val views: Int? = null,
    val avgRating: Double? = null,
    val createdAt: String? = null
)