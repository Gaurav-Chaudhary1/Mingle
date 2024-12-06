package com.example.soulmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.models.ChatUser

class UserMatchAdapter(
    private val userList: List<ChatUser>
) : RecyclerView.Adapter<UserMatchAdapter.UserViewHolder>() {

    private var onItemClickListener: ((ChatUser) -> Unit)? = null

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userProfileImage: ImageView = itemView.findViewById(R.id.imageProfile)
        val userName: TextView = itemView.findViewById(R.id.tvUserName)
        val userAddress: TextView = itemView.findViewById(R.id.tvUserAddress)

        init {
            itemView.setOnClickListener {
                onItemClickListener?.invoke(userList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_hot_match, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userList = userList[position]

        Glide.with(holder.itemView.context).load(userList.profileImage)
            .placeholder(com.facebook.login.R.drawable.com_facebook_profile_picture_blank_portrait)
            .into(holder.userProfileImage)

        holder.userName.text = userList.fullName

        holder.userAddress.text = userList.address

    }

    fun setOnItemClickListener(listener: (ChatUser) -> Unit) {
        onItemClickListener = listener
    }
}