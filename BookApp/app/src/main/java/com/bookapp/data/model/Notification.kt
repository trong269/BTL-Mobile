package com.bookapp.data.model

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    var read: Boolean,
    val createdAt: String
)
