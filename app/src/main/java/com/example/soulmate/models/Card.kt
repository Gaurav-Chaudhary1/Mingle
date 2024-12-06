package com.example.soulmate.models

data class Card(
    val userId: String,
    val fullName: String,
    val bio: String,
    val profileImage: String // Resource ID for profile picture (you can fetch actual images from Firebase Storage)
)