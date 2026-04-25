package com.bookapp.data.local

import android.content.Context
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.local.entities.LocalBook
import com.bookapp.data.local.entities.LocalChapter
import com.bookapp.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val bookDao = database.bookDao()

    suspend fun isBookDownloaded(bookId: String): Boolean {
        return bookDao.getBookById(bookId) != null
    }

    suspend fun downloadBook(book: Book, onProgress: (Int, Int) -> Unit) {
        withContext(Dispatchers.IO) {
            // 1. Get all chapters
            val chaptersResponse = RetrofitClient.instance.getChaptersByBook(book.id!!).execute()
            if (!chaptersResponse.isSuccessful) return@withContext
            
            val chapters = chaptersResponse.body() ?: return@withContext
            val total = chapters.size
            
            // 2. Save book metadata
            val localBook = LocalBook(
                id = book.id,
                title = book.title ?: "",
                author = book.author ?: "",
                description = book.description,
                coverImage = book.coverImage,
                totalChapters = total
            )
            bookDao.insertBook(localBook)
            
            // 3. Save chapters
            val localChapters = chapters.map { chapter ->
                LocalChapter(
                    id = chapter.id!!,
                    bookId = book.id,
                    chapterNumber = chapter.chapterNumber ?: 0,
                    title = chapter.title,
                    content = chapter.content
                )
            }
            bookDao.insertChapters(localChapters)
            
            withContext(Dispatchers.Main) {
                onProgress(total, total)
            }
        }
    }

    suspend fun getLocalBook(bookId: String) = bookDao.getBookById(bookId)
    
    suspend fun getLocalBookList(): List<LocalBook> = bookDao.getAllDownloadedBooks()
    
    suspend fun getLocalChapters(bookId: String) = bookDao.getChaptersByBookId(bookId)

    suspend fun deleteDownloadedBook(bookId: String) {
        val book = bookDao.getBookById(bookId)
        if (book != null) {
            bookDao.deleteBook(book)
        }
    }
}
