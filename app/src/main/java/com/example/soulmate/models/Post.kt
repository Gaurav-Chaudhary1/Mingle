package com.example.soulmate.models

data class Post(
    val userUID: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val postId: String = "",
    val likeCount: Int = 0, // Field for the number of likes
    val createdAt: Long = 0L
)
