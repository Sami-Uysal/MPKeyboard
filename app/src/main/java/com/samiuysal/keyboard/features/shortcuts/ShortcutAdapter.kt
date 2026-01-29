package com.samiuysal.keyboard.features.shortcuts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samiuysal.keyboard.R

class ShortcutAdapter(
        private val onEditClick: (ShortcutManager.Shortcut) -> Unit,
        private val onDeleteClick: (ShortcutManager.Shortcut) -> Unit
) : RecyclerView.Adapter<ShortcutAdapter.ShortcutViewHolder>() {

    private val shortcuts = mutableListOf<ShortcutManager.Shortcut>()

    fun setShortcuts(newShortcuts: List<ShortcutManager.Shortcut>) {
        shortcuts.clear()
        shortcuts.addAll(newShortcuts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_shortcut, parent, false)
        return ShortcutViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(shortcuts[position])
    }

    override fun getItemCount(): Int = shortcuts.size

    inner class ShortcutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTrigger: TextView = itemView.findViewById(R.id.txtTrigger)
        private val txtReplacement: TextView = itemView.findViewById(R.id.txtReplacement)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditShortcut)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteShortcut)

        fun bind(shortcut: ShortcutManager.Shortcut) {
            txtTrigger.text = shortcut.trigger
            txtReplacement.text = shortcut.replacement

            itemView.setOnClickListener { onEditClick(shortcut) }
            btnEdit.setOnClickListener { onEditClick(shortcut) }
            btnDelete.setOnClickListener { onDeleteClick(shortcut) }
        }
    }
}
