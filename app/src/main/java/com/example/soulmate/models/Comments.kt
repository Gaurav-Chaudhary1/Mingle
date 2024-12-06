package com.example.soulmate.models

data class Comments(
    var commentId: String = "",             // Unique identifier for the comment
    val userUID: String = "",                // UID of the user who made the comment
    val commentText: String = "",            // The text content of the comment
    val timestamp: Long = System.currentTimeMillis(), // Timestamp when the comment was created
    val userFullName: String = "",           // Full name of the user who made the comment
    val profileImageUrl: String = ""         // URL to the user's profile image
)