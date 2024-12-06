package com.example.soulmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.databinding.ItemUserPostGridBinding
import com.example.soulmate.models.Post

class PostGridAdapter(
    private val postList: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PostGridAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: ItemUserPostGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            Glide.with(binding.userImage.context)
                .load(post.imageUrl)
                .placeholder(R.drawable.baseline_person_24) // Default placeholder
                .into(binding.userImage)

            itemView.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemUserPostGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postList[position])
    }

    override fun getItemCount(): Int = postList.size
}
