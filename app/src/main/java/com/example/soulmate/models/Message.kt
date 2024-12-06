package com.example.soulmate.models

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    val dateStamp: Long = 0L,
    val messageType: String = "text", // "text", "image", "video"
    val imageUrl: String? = null,
    val videoUrl: String? = null
)
