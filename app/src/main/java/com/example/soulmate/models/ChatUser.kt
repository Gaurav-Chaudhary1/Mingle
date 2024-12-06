package com.example.soulmate.models

data class ChatUser(
    var uid: String? = null,
    val fullName: String? = null,
    val profileImage: String? = null,
    val onlineStatus: String? = "offline",
    val address: String? = null,
    val gender: String? = null
) {
    // No-argument constructor for Firebase
    constructor() : this(null, null, null, "offline", null, null)
}
