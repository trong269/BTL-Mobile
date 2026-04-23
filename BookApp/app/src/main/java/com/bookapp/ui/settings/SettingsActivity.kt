package com.bookapp.ui.settings

import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.theme.ThemePreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnSettingsBack)
        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val rbLight = findViewById<RadioButton>(R.id.rbThemeLight)
        val rbDark = findViewById<RadioButton>(R.id.rbThemeDark)
        val rbSystem = findViewById<RadioButton>(R.id.rbThemeSystem)

        when (ThemePreferenceManager.getSavedThemeMode(this)) {
            ThemePreferenceManager.THEME_LIGHT -> rbLight.isChecked = true
            ThemePreferenceManager.THEME_DARK -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }

        btnBack.setOnClickListener {
            finish()
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.rbThemeLight -> ThemePreferenceManager.THEME_LIGHT
                R.id.rbThemeDark -> ThemePreferenceManager.THEME_DARK
                else -> ThemePreferenceManager.THEME_SYSTEM
            }
            ThemePreferenceManager.saveThemeMode(this, selectedMode)
        }
    }
}