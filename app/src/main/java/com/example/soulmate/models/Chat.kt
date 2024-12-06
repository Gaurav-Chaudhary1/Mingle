package com.example.soulmate.models

data class Chat(
    val chatId: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
)
