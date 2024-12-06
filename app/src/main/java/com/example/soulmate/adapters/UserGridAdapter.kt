package com.example.soulmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.databinding.ItemFilterUserGridBinding
import com.example.soulmate.models.UserProfile

class UserGridAdapter(
    private val userList: List<UserProfile>,
    private val onItemClick: (String) -> Unit // Lambda for handling item click
) : RecyclerView.Adapter<UserGridAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemFilterUserGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(userProfile: UserProfile) {
            binding.userNameTextView.text = userProfile.fullName
            binding.userAddressTextView.text = userProfile.address
            binding.userFollowersTextView.text = "${userProfile.followersCount} Followers"

            // Load profile image
            Glide.with(binding.profileImageView.context)
                .load(userProfile.profileImage)
                .placeholder(com.facebook.login.R.drawable.com_facebook_profile_picture_blank_portrait)
                .into(binding.profileImageView)

            // Handle "Most Liked" visibility
            binding.mostLikedTextView.visibility =
                if (userProfile.mostLiked) View.VISIBLE else View.GONE

            // Item click listener
            itemView.setOnClickListener {
                onItemClick(userProfile.uid)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemFilterUserGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size
}
