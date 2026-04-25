package com.bookapp

import android.app.Application
import com.bookapp.data.api.RetrofitClient
import com.bookapp.theme.ThemePreferenceManager

class BookAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        ThemePreferenceManager.applySavedTheme(this)
    }
}