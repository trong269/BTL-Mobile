package com.bookapp.data.local.dao

import androidx.room.*
import com.bookapp.data.local.entities.LocalBook
import com.bookapp.data.local.entities.LocalChapter

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LocalBook)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<LocalChapter>)

    @Query("SELECT * FROM offline_books")
    suspend fun getAllDownloadedBooks(): List<LocalBook>

    @Query("SELECT * FROM offline_books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): LocalBook?

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    suspend fun getChaptersByBookId(bookId: String): List<LocalChapter>

    @Query("SELECT * FROM offline_chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): LocalChapter?

    @Delete
    suspend fun deleteBook(book: LocalBook)

    @Query("DELETE FROM offline_chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: String)
}
