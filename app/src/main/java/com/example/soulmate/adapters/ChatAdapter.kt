package com.example.soulmate.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.soulmate.R
import com.example.soulmate.models.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private var messages: List<Message>, private val currentUserId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = inflater.inflate(R.layout.send_message, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.received_message, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val previousMessage = if (position > 0) messages[position - 1] else null

        if (holder is SentMessageViewHolder) {
            holder.bind(message, previousMessage)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message, previousMessage)
        }
    }

    override fun getItemCount(): Int = messages.size

    // ViewHolder for sent messages
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageDate: TextView = itemView.findViewById(R.id.text_date_send)
        private val messageText: TextView = itemView.findViewById(R.id.sent_message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.sent_message_time)
        private val sharedImage: ImageView = itemView.findViewById(R.id.shared_image_sent)
        private val videoView: VideoView = itemView.findViewById(R.id.shared_video_sent)
        private val sendLayout: LinearLayout = itemView.findViewById(R.id.sent_message_layout)

        fun bind(message: Message, previousMessage: Message?) {
            // Reset visibility to prevent residual states
            messageText.visibility = View.GONE
            sharedImage.visibility = View.GONE
            videoView.visibility = View.GONE

            // Show date if it's a new day
            if (previousMessage == null || isNewDay(message.dateStamp, previousMessage.dateStamp)) {
                messageDate.visibility = View.VISIBLE
                messageDate.text = formatDate(message.dateStamp)
            } else {
                messageDate.visibility = View.GONE
            }

            // Handle message type
            when (message.messageType) {
                "text" -> {
                    sendLayout.setBackgroundResource(R.drawable.sent_message_background)
                    messageText.visibility = View.VISIBLE
                    messageText.text = message.messageText
                }

                "image" -> {
                    sendLayout.setBackgroundResource(0)
                    sharedImage.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(message.imageUrl)
                        .into(sharedImage)
                }

                "video" -> {
                    sendLayout.setBackgroundResource(0)
                    videoView.visibility = View.VISIBLE
                    videoView.setVideoPath(message.videoUrl)
                    videoView.setOnPreparedListener { it.isLooping = true }
                    videoView.start()
                }
            }

            // Set timestamp
            messageTime.text = formatTime(message.timestamp)
        }
    }

    // ViewHolder for received messages
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageDate: TextView = itemView.findViewById(R.id.text_date_receive)
        private val messageText: TextView = itemView.findViewById(R.id.received_message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.received_message_time)
        private val sharedImageReceived: ImageView = itemView.findViewById(R.id.shared_image_received)
        private val videoViewReceived: VideoView = itemView.findViewById(R.id.shared_video_received)
        private val receiveLayout: LinearLayout = itemView.findViewById(R.id.received_message_layout)

        fun bind(message: Message, previousMessage: Message?) {
            // Reset visibility to prevent residual states
            messageText.visibility = View.GONE
            sharedImageReceived.visibility = View.GONE
            videoViewReceived.visibility = View.GONE

            // Show date if it's a new day
            if (previousMessage == null || isNewDay(message.dateStamp, previousMessage.dateStamp)) {
                messageDate.visibility = View.VISIBLE
                messageDate.text = formatDate(message.dateStamp)
            } else {
                messageDate.visibility = View.GONE
            }

            // Handle message type
            when (message.messageType) {
                "text" -> {
                    receiveLayout.setBackgroundResource(R.drawable.received_message_background)
                    messageText.visibility = View.VISIBLE
                    messageText.text = message.messageText
                }

                "image" -> {
                    Log.d("ChatAdapter", "Receiver Image URL: ${message.imageUrl}")  // Log the URL
                    receiveLayout.setBackgroundResource(0)
                    sharedImageReceived.visibility = View.VISIBLE

                    // Convert the string URL to Uri and load using Glide
                    val uri = Uri.parse(message.imageUrl)
                    Glide.with(itemView.context)
                        .load(uri)
                        .placeholder(R.drawable.image_not_supported)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)  // Disable caching
                        .skipMemoryCache(true)  // Skip memory cache
                        .into(sharedImageReceived)
                }


                "video" -> {
                    receiveLayout.setBackgroundResource(0)
                    videoViewReceived.visibility = View.VISIBLE
                    videoViewReceived.setVideoPath(message.videoUrl)
                    videoViewReceived.setOnPreparedListener { it.isLooping = true }
                    videoViewReceived.start()
                }
            }

            // Set timestamp
            messageTime.text = formatTime(message.timestamp)
        }
    }

    // Check if the message starts a new day
    private fun isNewDay(current: Long, previous: Long): Boolean {
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(current))
        val previousDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(previous))
        return currentDate != previousDate
    }

    // Format timestamp into "hh:mm a"
    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    // Format date into "EEE, MMM d"
    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timestamp))
    }

    fun updateMessages(newMessages: List<Message>) {
        this.messages = newMessages
        notifyDataSetChanged()
    }
}
