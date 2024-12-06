package com.example.soulmate.adapters

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.databinding.ItemPostsBinding
import com.example.soulmate.models.Post

class PostAdapter(
    private var postList: List<Post>,  // Use the full Post object
    private val onPostClick: (Post, position: Int) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]  // Use the full Post object
        holder.bind(post)
        holder.itemView.setOnClickListener {
            onPostClick(post, position) // Pass the actual post object
        }
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newPosts: List<Post>) {
        postList = newPosts
        notifyDataSetChanged()
    }

    inner class PostViewHolder(private val binding: ItemPostsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            Glide.with(binding.root)
                .load(post.imageUrl) // Assuming Post has an imageUrl
                .into(binding.imageViewPost)
        }
    }

    class ItemOffsetDecoration(private val offset: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.set(offset, offset, offset, offset)
        }
    }
}
