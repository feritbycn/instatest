package com.example.pubstats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostThumbnailAdapter(private val urls: List<String>) :
    RecyclerView.Adapter<PostThumbnailAdapter.VH>() {

    class VH(val imageView: android.widget.ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        val imageView = view.findViewById<android.widget.ImageView>(R.id.postImage)
        return VH(imageView)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.imageView.context)
            .load(urls[position])
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = urls.size
}
