package com.example.soulmate.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.soulmate.models.ChatUser
import com.example.soulmate.models.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    private val _user = MutableLiveData<ChatUser>()
    val user: LiveData<ChatUser> get() = _user

    private val _userStatus = MutableLiveData<String>()
    val userStatus: LiveData<String> get() = _userStatus

    // Fetch user profile data and online status
    fun fetchUserData(chatWithUID: String) {
        firestore.collection("UserDetails").document(chatWithUID)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _user.value = it.toObject(ChatUser::class.java)
                    _userStatus.value = _user.value?.onlineStatus ?: "offline"
                }
            }
    }

    // Fetch chat messages
    fun fetchMessages(chatId: String) {
        firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val messagesList = it.toObjects(Message::class.java)
                    _messages.value = messagesList
                }
            }
    }

    // Send a message
    fun sendMessage(chatId: String, message: Message) {
        // Initialize chat if it doesn't exist
        initializeChat(chatId, message.senderId, message.receiverId)

        // Use the same timestamp as dateStamp for simplicity
        val messageWithDate = message.copy(dateStamp = message.timestamp)

        firestore.collection("chats").document(chatId).collection("messages")
            .add(messageWithDate)
            .addOnSuccessListener {
                // Update the last message metadata in the chat
                firestore.collection("chats").document(chatId)
                    .update(
                        "lastMessage", message.messageText,
                        "lastMessageTime", message.timestamp
                    )
            }
            .addOnFailureListener {
                // Handle failure, e.g., log the error
                Log.e("ChatViewModel", "Error sending message: ${it.message}")
            }
    }

    private fun initializeChat(chatId: String, user1Id: String, user2Id: String) {
        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // Chat document does not exist; create a new one
                val chatData = mapOf(
                    "chatId" to chatId,
                    "user1Id" to user1Id,
                    "user2Id" to user2Id,
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis()
                )

                chatRef.set(chatData)
                    .addOnSuccessListener {
                        // Chat initialized successfully; you can notify the UI if needed
                        Log.d("ChatViewModel", "Chat initialized successfully")
                    }
                    .addOnFailureListener {
                        // Handle failure, e.g., show a message or log an error
                        Log.e("ChatViewModel", "Error initializing chat: ${it.message}")
                    }
            }
        }.addOnFailureListener {
            // Handle failure, e.g., show a message or log an error
            Log.e("ChatViewModel", "Error fetching chat: ${it.message}")
        }
    }
}
