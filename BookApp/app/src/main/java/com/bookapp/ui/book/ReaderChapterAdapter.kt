package com.bookapp.ui.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Chapter

class ReaderChapterAdapter(
    private val onChapterClick: (Int) -> Unit
) : RecyclerView.Adapter<ReaderChapterAdapter.ViewHolder>() {

    private val items = mutableListOf<Chapter>()
    private var currentIndex: Int = -1

    fun submitList(chapters: List<Chapter>, selectedIndex: Int) {
        items.clear()
        items.addAll(chapters)
        currentIndex = selectedIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reader_chapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position, position == currentIndex)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvChapterItemTitle)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvChapterItemMeta)

        fun bind(chapter: Chapter, index: Int, isSelected: Boolean) {
            val chapterTitle = chapter.title?.takeIf { it.isNotBlank() } ?: "(Khong co tieu de)"
            tvTitle.text = chapterTitle
            tvMeta.text = ""

            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.reader_chapter_selected_bg)
                tvTitle.setTextColor(0xFF1F6FB2.toInt())
                tvMeta.setTextColor(0xFF1F6FB2.toInt())
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
                tvTitle.setTextColor(0xFF1B1F24.toInt())
                tvMeta.setTextColor(0xFF8A9099.toInt())
            }

            itemView.setOnClickListener {
                onChapterClick(index)
            }
        }
    }
}
