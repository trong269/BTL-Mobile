package com.bookapp.data.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    val fullName: String? = null,
    val avatar: String? = null
)