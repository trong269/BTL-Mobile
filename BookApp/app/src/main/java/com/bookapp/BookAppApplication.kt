package com.bookapp

import android.app.Application
import com.bookapp.theme.ThemePreferenceManager

class BookAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferenceManager.applySavedTheme(this)
    }
}