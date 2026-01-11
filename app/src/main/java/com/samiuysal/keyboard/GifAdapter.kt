package com.samiuysal.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class GifAdapter(
        private var gifs: List<GiphyObject>,
        private val onGifClick: (String, ProgressBar) -> Unit
) : RecyclerView.Adapter<GifAdapter.GifViewHolder>() {

    class GifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.img_gif_preview)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_loading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gif, parent, false)
        return GifViewHolder(view)
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        val gif = gifs[position]

        holder.progressBar.visibility = View.GONE

        Glide.with(holder.itemView.context)
                .load(gif.images.preview.url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageView)

        holder.itemView.setOnClickListener {
            holder.progressBar.visibility = View.VISIBLE
            onGifClick(gif.images.original.url, holder.progressBar)
        }
    }

    override fun getItemCount() = gifs.size

    fun updateData(newGifs: List<GiphyObject>) {
        gifs = newGifs
        notifyDataSetChanged()
    }
}
