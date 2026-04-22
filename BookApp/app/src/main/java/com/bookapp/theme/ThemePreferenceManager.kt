package com.bookapp.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferenceManager {

    private const val PREFS_NAME = "BookAppPrefs"
    private const val KEY_THEME_MODE = "themeMode"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    fun applySavedTheme(context: Context) {
        val mode = getSavedThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    fun getSavedThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveThemeMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    private fun toNightMode(mode: String): Int {
        return when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}