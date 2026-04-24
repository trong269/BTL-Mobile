package com.bookapp.data.model

data class Comment(
    val id: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val fullName: String? = null,
    val avatar: String? = null,
    val bookId: String? = null,
    val content: String? = null,
    val createdAt: String? = null
)
