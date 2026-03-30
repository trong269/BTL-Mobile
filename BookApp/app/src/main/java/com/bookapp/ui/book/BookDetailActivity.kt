package com.bookapp.ui.book

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R

class BookDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        val title = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Chi tiet sach"

        findViewById<TextView>(R.id.tvBookDetailTitle).text = title
        findViewById<Button>(R.id.btnBackFromBookDetail).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }
}
