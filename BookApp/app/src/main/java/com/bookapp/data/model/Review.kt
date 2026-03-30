package com.bookapp.data.model

data class Review(
    val id: String? = null,
    val userId: String? = null,
    val bookId: String? = null,
    val rating: Int? = null,
    val review: String? = null,
    val createdAt: String? = null
)
