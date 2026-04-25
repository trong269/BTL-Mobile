package com.bookapp.ui.feature

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R

class FeatureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tính năng"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
            ?: "Nội dung tính năng sẽ được tải tại đây."

        findViewById<TextView>(R.id.tvFeatureTitle).text = title
        findViewById<TextView>(R.id.tvFeatureDescription).text = description
        findViewById<Button>(R.id.btnBackHome).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
    }
}