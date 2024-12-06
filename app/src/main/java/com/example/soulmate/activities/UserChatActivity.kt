package com.example.soulmate.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.adapters.ChatAdapter
import com.example.soulmate.databinding.ActivityUserChatBinding
import com.example.soulmate.fragments.AttachBottomSheetFragment
import com.example.soulmate.models.Message
import com.example.soulmate.mvvm.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import java.util.UUID

class UserChatActivity : AppCompatActivity(), AttachBottomSheetFragment.OnMediaSelectedListener {

    private lateinit var binding: ActivityUserChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var chatId: String
    private lateinit var chatWithUID: String
    private lateinit var messagesAdapter: ChatAdapter
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        const val MESSAGE_TYPE_IMAGE = "image"
        const val MESSAGE_TYPE_VIDEO = "video"
        const val IMAGE_PICK_REQUEST_CODE = 1001
        const val VIDEO_PICK_REQUEST_CODE = 1002


        fun start(context: Context, chatWithUID: String) {
            val intent = Intent(context, UserChatActivity::class.java)
            intent.putExtra("chatWithUID", chatWithUID)
            context.startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val selectedUri: Uri? = data.data
            selectedUri?.let {
                if (requestCode == IMAGE_PICK_REQUEST_CODE) {
                    uploadMediaToFirebase(it, MESSAGE_TYPE_IMAGE)
                } else if (requestCode == VIDEO_PICK_REQUEST_CODE) {
                    uploadMediaToFirebase(it, MESSAGE_TYPE_VIDEO)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        chatWithUID = intent.getStringExtra("chatWithUID") ?: ""
        chatId = createChatId(FirebaseAuth.getInstance().currentUser?.uid, chatWithUID)

        setupRecyclerView()

        binding.backButton.setOnClickListener { finish() }

        observeUserData()
        observeMessages()

        binding.sendButton.setOnClickListener { sendTextMessage() }
        binding.attachButton.setOnClickListener {
            val bottomSheetFragment = AttachBottomSheetFragment()
            bottomSheetFragment.listener = this
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        binding.userPhoneCall.setOnClickListener { it ->
            val targetUserId = chatWithUID
            startVoiceCall(targetUserId)
        }

        binding.userVideoCall.setOnClickListener { it ->
            val targetUserId = chatWithUID
            startVideoCall(targetUserId)
        }

    }

    private fun startVoiceCall(targetUserId: String){
        val targetUserName: String = targetUserId

        binding.userPhoneCall.setIsVideoCall(false)
        binding.userPhoneCall.resourceID = "zego_uikit_call"
        binding.userPhoneCall.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
    }

    private fun startVideoCall(targetUserId: String){
        val targetUserName: String = targetUserId

        binding.userVideoCall.setIsVideoCall(true)
        binding.userVideoCall.resourceID = "zego_uikit_call"
        binding.userVideoCall.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
    }

    private fun setupRecyclerView() {
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        messagesAdapter = ChatAdapter(emptyList(), FirebaseAuth.getInstance().currentUser?.uid ?: "")
        binding.rvChat.adapter = messagesAdapter
    }

    private fun observeUserData() {
        viewModel.fetchUserData(chatWithUID)
        viewModel.user.observe(this) { user ->
            val firstName = user.fullName?.split(" ")?.firstOrNull() ?: ""
            binding.userName.text = firstName
            user.profileImage?.let { loadImageIntoView(binding.userProfilePic, it) }
        }

        viewModel.userStatus.observe(this) { status ->
            binding.userStatus.text = status
        }
    }

    private fun observeMessages() {
        viewModel.fetchMessages(chatId)
        viewModel.messages.observe(this) { messages ->
            messagesAdapter.updateMessages(messages)
            binding.rvChat.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendTextMessage() {
        val messageText = binding.messageInput.text.toString()
        if (messageText.isNotEmpty()) {
            val message = Message(
                senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                receiverId = chatWithUID,
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                dateStamp = System.currentTimeMillis()
            )
            viewModel.sendMessage(chatId, message)
            binding.messageInput.text.clear()
        }
    }

    private fun uploadMediaToFirebase(uri: Uri, mediaType: String) {
        val storageReference: StorageReference = FirebaseStorage.getInstance().reference
        val fileName = UUID.randomUUID().toString()
        val mediaRef = if (mediaType == MESSAGE_TYPE_IMAGE) {
            storageReference.child("images/$fileName")
        } else {
            storageReference.child("videos/$fileName")
        }

        mediaRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendMediaMessage(mediaType, downloadUrl.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserChatActivity", "Failed to upload media: ${exception.message}")
            }
    }

    private fun sendMediaMessage(type: String, mediaUri: String) {
        val message = Message(
            senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            receiverId = chatWithUID,
            messageText = if (type == MESSAGE_TYPE_IMAGE) "Image" else "Video",
            timestamp = System.currentTimeMillis(),
            dateStamp = System.currentTimeMillis(),
            messageType = type,
            imageUrl = if (type == MESSAGE_TYPE_IMAGE) mediaUri else null,
            videoUrl = if (type == MESSAGE_TYPE_VIDEO) mediaUri else null
        )
        viewModel.sendMessage(chatId, message)
    }

    private fun createChatId(userId1: String?, userId2: String?): String {
        val uid1 = userId1 ?: "unknown_user1"
        val uid2 = userId2 ?: "unknown_user2"
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    private fun loadImageIntoView(imageView: ImageView, imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.user_pro)
            .into(imageView)
    }

    override fun onImageSelected() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    override fun onVideoSelected() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, VIDEO_PICK_REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("online")
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus("offline")
    }

    private fun updateUserStatus(status: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            firestore.collection("UserDetails").document(it)
                .update("onlineStatus", status)
                .addOnFailureListener { e ->
                    Log.e("UserDetailsFragment", "Error updating online status: ${e.message}")
                }
        }
    }
}
