package com.bookapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_books")
data class LocalBook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String?,
    val coverImage: String?,
    val totalChapters: Int,
    val downloadedAt: Long = System.currentTimeMillis()
)
