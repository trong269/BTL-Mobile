package com.bookapp.data.model

data class ReadingProgress(
    val id: String? = null,
    val userId: String? = null,
    val bookId: String? = null,
    val chapterId: String? = null,
    val chapterProgressPercent: Int? = 0,
    val updatedAt: String? = null
)
