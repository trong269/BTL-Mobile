package com.bookapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bookapp.data.local.dao.BookDao
import com.bookapp.data.local.entities.LocalBook
import com.bookapp.data.local.entities.LocalChapter

@Database(entities = [LocalBook::class, LocalChapter::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "book_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
