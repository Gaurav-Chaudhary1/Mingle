package com.example.soulmate.models

data class Reel(
    val reelId: String = "", // Add this field to uniquely identify the reel
    val videoUrl: String = "",
    val caption: String? = null,
    val userId: String = "",
    val timestamp: Long = 0,
    val username: String = "", // Field for the username
    val profileImage: String = "", // Field for the profile image URL
    val likeCount: Int = 0 // Field for the number of likes
)
