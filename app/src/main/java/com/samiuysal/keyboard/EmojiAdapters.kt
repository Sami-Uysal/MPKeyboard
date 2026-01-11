package com.samiuysal.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiAdapter(
        private var emojis: List<EmojiData>,
        private val onEmojiClick: (EmojiData) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    fun updateEmojis(newEmojis: List<EmojiData>) {
        this.emojis = newEmojis
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.bind(emoji)
    }

    override fun getItemCount(): Int = emojis.size

    inner class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.text_emoji)

        fun bind(emoji: EmojiData) {
            textView.text = emoji.char
            itemView.setOnClickListener { onEmojiClick(emoji) }
        }
    }
}

data class CategoryItem(val key: String, val displayName: String)

class CategoryAdapter(
        private val categories: List<CategoryItem>,
        private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_emoji_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val isSelected = position == selectedPosition
        holder.bind(category, isSelected)
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.text_category)

        fun bind(category: CategoryItem, isSelected: Boolean) {
            textView.text = category.displayName

            if (isSelected) {
                textView.setBackgroundResource(
                        R.drawable.button_secondary
                )
                textView.alpha = 1.0f
            } else {
                textView.background = null
                textView.alpha = 0.5f
            }

            itemView.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category.key)
            }
        }
    }
}
