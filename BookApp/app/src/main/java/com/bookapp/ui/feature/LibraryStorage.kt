package com.bookapp.ui.feature

import android.content.Context
import com.bookapp.data.model.Book
import org.json.JSONArray
import org.json.JSONObject

object LibraryStorage {

    data class LibraryBookItem(
        val bookId: String,
        val title: String,
        val author: String,
        val coverImage: String?,
        val categoryId: String?,
        val updatedAt: Long
    )

    private const val PREFS_NAME = "BookAppLibraryPrefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENTS = "recent_reads"
    private const val MAX_RECENTS = 30

    fun addFavorite(context: Context, book: LibraryBookItem) {
        val current = getFavorites(context).toMutableList()
        val filtered = current.filterNot { it.bookId == book.bookId }.toMutableList()
        filtered.add(0, book.copy(updatedAt = System.currentTimeMillis()))
        save(context, KEY_FAVORITES, filtered)
    }

    fun removeFavorite(context: Context, bookId: String) {
        val filtered = getFavorites(context).filterNot { it.bookId == bookId }
        save(context, KEY_FAVORITES, filtered)
    }

    fun addRecent(context: Context, book: LibraryBookItem) {
        val filtered = getRecentReads(context).filterNot { it.bookId == book.bookId }.toMutableList()
        filtered.add(0, book.copy(updatedAt = System.currentTimeMillis()))
        save(context, KEY_RECENTS, filtered.take(MAX_RECENTS))
    }

    fun removeRecentByBookIds(context: Context, bookIds: Set<String>) {
        if (bookIds.isEmpty()) return
        val filtered = getRecentReads(context).filterNot { it.bookId in bookIds }
        save(context, KEY_RECENTS, filtered)
    }

    fun clearRecentsBefore(context: Context, timestamp: Long): Int {
        val current = getRecentReads(context)
        val filtered = current.filter { it.updatedAt >= timestamp }
        save(context, KEY_RECENTS, filtered)
        return current.size - filtered.size
    }

    fun clearRecentsWithinLast(context: Context, durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        val current = getRecentReads(context)
        val threshold = System.currentTimeMillis() - durationMs
        val filtered = current.filter { it.updatedAt < threshold }
        save(context, KEY_RECENTS, filtered)
        return current.size - filtered.size
    }

    fun clearAllRecents(context: Context) {
        save(context, KEY_RECENTS, emptyList())
    }

    fun getFavorites(context: Context): List<LibraryBookItem> = load(context, KEY_FAVORITES)

    fun getRecentReads(context: Context): List<LibraryBookItem> = load(context, KEY_RECENTS)

    fun fromBook(book: Book, fallbackBookId: String? = null, fallbackTitle: String? = null): LibraryBookItem? {
        val id = book.id?.trim().orEmpty().ifEmpty { fallbackBookId?.trim().orEmpty() }
        if (id.isEmpty()) return null

        val title = book.title?.takeIf { it.isNotBlank() }
            ?: fallbackTitle?.takeIf { it.isNotBlank() }
            ?: "Chưa có tiêu đề"

        return LibraryBookItem(
            bookId = id,
            title = title,
            author = book.author?.takeIf { it.isNotBlank() } ?: "Không rõ tác giả",
            coverImage = book.coverImage,
            categoryId = book.categoryId,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun basicItem(bookId: String, title: String): LibraryBookItem {
        return LibraryBookItem(
            bookId = bookId,
            title = title.ifBlank { "Chưa có tiêu đề" },
            author = "Không rõ tác giả",
            coverImage = null,
            categoryId = null,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun load(context: Context, key: String): List<LibraryBookItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val result = mutableListOf<LibraryBookItem>()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("bookId").trim()
            if (id.isEmpty()) continue

            result.add(
                LibraryBookItem(
                    bookId = id,
                    title = obj.optString("title").ifBlank { "Chưa có tiêu đề" },
                    author = obj.optString("author").ifBlank { "Không rõ tác giả" },
                    coverImage = obj.optString("coverImage").takeIf { it.isNotBlank() },
                    categoryId = obj.optString("categoryId").takeIf { it.isNotBlank() },
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }

        return result
    }

    private fun save(context: Context, key: String, items: List<LibraryBookItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("bookId", item.bookId)
            obj.put("title", item.title)
            obj.put("author", item.author)
            obj.put("coverImage", item.coverImage)
            obj.put("categoryId", item.categoryId)
            obj.put("updatedAt", item.updatedAt)
            arr.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, arr.toString())
            .apply()
    }
}
