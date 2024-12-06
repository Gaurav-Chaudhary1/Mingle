package com.example.soulmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.models.Chat
import com.example.soulmate.models.ChatUser
import de.hdodenhof.circleimageview.CircleImageView

class UserStatusAdapter(private val chatList: List<Pair<Chat, ChatUser>>) :
    RecyclerView.Adapter<UserStatusAdapter.UserStatusViewHolder>() {

    inner class UserStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image_status)
        val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserStatusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status, parent, false)
        return UserStatusViewHolder(view)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: UserStatusViewHolder, position: Int) {
        val (_, chatUser) = chatList[position]

        chatUser.profileImage?.let {
            Glide.with(holder.itemView.context)
                .load(chatUser.profileImage)
                .placeholder(R.drawable.user_profile_svgrepo_com)
                .into(holder.profileImage)
        }

        val isOnline = chatUser.onlineStatus == "online"
        val statusDrawable = if (isOnline) {
            R.drawable.status_circle_red
        } else {
            R.drawable.status_circle_grey
        }
        holder.statusIndicator.setBackgroundResource(statusDrawable)
    }
}