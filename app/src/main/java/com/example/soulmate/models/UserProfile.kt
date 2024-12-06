package com.example.soulmate.models

data class UserProfile(
    val uid: String,
    val fullName: String,
    val profileImage: String,
    val address: String = "", // Optional, if you want to include address
    val followersCount: Int = 0, // Optional, if you want to include followers count
    val mostLiked: Boolean = false // Optional, to indicate if the user is mostly liked
)