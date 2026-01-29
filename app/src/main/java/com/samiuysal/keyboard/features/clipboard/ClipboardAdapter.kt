package com.samiuysal.keyboard.features.clipboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samiuysal.keyboard.R

class ClipboardAdapter(private var clips: List<String>, private val onClick: (String) -> Unit) :
        RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboardText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_clipboard, parent, false)
        return ViewHolder(view)
    }

    private var keyColor: Int? = null
    private var textColor: Int? = null

    fun setTheme(cardColor: Int, txtColor: Int) {
        keyColor = cardColor
        textColor = txtColor
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = clips[position]
        holder.textView.text = text
        holder.itemView.setOnClickListener { onClick(text) }

        if (keyColor != null) {
            (holder.itemView as androidx.cardview.widget.CardView).setCardBackgroundColor(
                    keyColor!!
            )
        }
        if (textColor != null) {
            holder.textView.setTextColor(textColor!!)
        }
    }

    override fun getItemCount() = clips.size

    fun updateClips(newClips: List<String>) {
        clips = newClips
        notifyDataSetChanged()
    }
}
