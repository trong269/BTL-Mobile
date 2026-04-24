package com.bookapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_chapters",
    foreignKeys = [
        ForeignKey(
            entity = LocalBook::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class LocalChapter(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val title: String?,
    val content: String?
)
