package com.example.soulmate.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.models.Chat
import com.example.soulmate.models.ChatUser
import de.hdodenhof.circleimageview.CircleImageView

class UserChatAdapter(
    private val chatList: List<Pair<Chat, ChatUser?>> // List of chats paired with user details
) : RecyclerView.Adapter<UserChatAdapter.UserChatViewHolder>() {

    private var onClickListener: OnClickListener? = null

    // ViewHolder to bind the layout
    inner class UserChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.other_user_pic)
        val fullName: TextView = itemView.findViewById(R.id.tvName)
        val recentMessage: TextView = itemView.findViewById(R.id.recentMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_message_item, parent, false)
        return UserChatViewHolder(view)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: UserChatViewHolder, position: Int) {
        val (chat, userDetails) = chatList[position]

        // Safely bind user details
        holder.fullName.text = userDetails?.fullName ?: "Unknown User"
        Glide.with(holder.itemView.context)
            .load(userDetails?.profileImage ?: R.drawable.user_pro) // Default image if null
            .placeholder(R.drawable.user_pro)
            .into(holder.profileImage)
        holder.recentMessage.text = chat.lastMessage ?: "No messages yet"

        // Handle click events safely
        holder.itemView.setOnClickListener {
            if (userDetails != null && !userDetails.uid.isNullOrEmpty()) {
                onClickListener?.onClick(chat.chatId, userDetails.uid!!)
            } else {
                // Log the issue or provide feedback
                Log.e("UserChatAdapter", "Invalid user details: ${userDetails?.uid}")
            }
        }
    }

    // Interface for handling click events
    interface OnClickListener {
        fun onClick(chatId: String, otherUserId: String)
    }

    // Method to set the click listener
    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }
}
