package com.example.soulmate.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.databinding.ActivityReelsBinding
import com.example.soulmate.utils.ProgressDialogUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.soulmate.models.ReelUploadWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FieldValue
import java.util.*

class ReelsActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityReelsBinding.inflate(layoutInflater)
    }

    private var selectedVideoUri: Uri? = null
    private lateinit var loadingDialog: ProgressDialogUtil
    private var visibility: String = "Anyone"
    private var hideLikes = false
    private var hideShares = false
    private var hideComments = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize loading dialog
        loadingDialog = ProgressDialogUtil()

        // Fetch and load profile image from Firebase Storage
        loadProfileImageFromFirebase()

        // Back button functionality
        binding.closeBtn.setOnClickListener {
            startActivity(Intent(this@ReelsActivity, UserMainActivity::class.java))
            finish()
        }

        // Set up click listener for the visibility TextView (for BottomSheet)
        binding.privacyDropdown.setOnClickListener {
            showVisibilityOptions()
        }

        // Video picker
        binding.idIVOriginalShorts.setOnClickListener {
            openVideoPicker()
        }

        // Set Post button functionality
        binding.postButton.setOnClickListener {
            checkAndPost()
        }

        // Set toggles for hiding likes, shares, and comments
        binding.hideLikeToggle.setOnCheckedChangeListener { _, isChecked ->
            hideLikes = isChecked
        }
        binding.hideShareToggle.setOnCheckedChangeListener { _, isChecked ->
            hideShares = isChecked
        }
        binding.hideCommentToggle.setOnCheckedChangeListener { _, isChecked ->
            hideComments = isChecked
        }
    }

    private fun loadProfileImageFromFirebase() {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userUID.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.user_profile_svgrepo_com)
                    .into(binding.profileImage)
            }
            .addOnFailureListener { exception ->
                showToast("Failed to load profile image: ${exception.message}")
            }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_VIDEO_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.data

            selectedVideoUri?.let { uri ->
                // Check if the selected video is under 25 seconds
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, uri)
                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()

                if (duration != null && duration > 25000) {  // Check for 25 seconds in milliseconds
                    showToast("Please select a video of 25 seconds or less.")
                    selectedVideoUri = null
                } else {
                    // Set thumbnail to ImageView
                    val thumbnail = retriever.getFrameAtTime(0)
                    binding.idIVOriginalShorts.setImageBitmap(thumbnail)
                }
                retriever.release()
            }
        } else {
            showToast("Video selection was cancelled.")
        }
    }

    private fun showVisibilityOptions() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.visibility_bottom_sheet, null)

        val everyoneRadioButton = view.findViewById<RadioButton>(R.id.anyone)
        val friendsRadioButton = view.findViewById<RadioButton>(R.id.friends)

        everyoneRadioButton.setOnClickListener {
            it.postDelayed({
                binding.privacyDropdown.text = "Anyone ▼"
                visibility = "Anyone"
                bottomSheet.dismiss()
            }, 200)
        }

        friendsRadioButton.setOnClickListener {
            it.postDelayed({
                binding.privacyDropdown.text = "Friends only ▼"
                visibility = "Friends only"
                bottomSheet.dismiss()
            }, 200)
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun checkAndPost() {
        val caption = binding.edtCaption.text.toString().trim()

        if (selectedVideoUri == null) {
            showToast("Please select a video!")
            return
        }

        if (caption.isEmpty()) {
            showToast("Please write a caption!")
            return
        }

        // Fetch the user's profile details before uploading the reel
        fetchUserProfileAndUploadReel(caption, selectedVideoUri)
    }

    private fun fetchUserProfileAndUploadReel(caption: String, videoUri: Uri?) {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid
        if (userUID != null) {
            FirebaseFirestore.getInstance().collection("UserDetails").document(userUID).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: "Unknown User"
                        val profileImage = document.getString("profileImage") ?: ""

                        // Generate a unique reelId using UUID
                        val reelId = UUID.randomUUID().toString()

                        // Upload the reel using WorkManager
                        uploadReelWithWorker(caption, videoUri, username, profileImage, userUID, reelId)

                        // Now navigate to UserMainActivity
                        startActivity(Intent(this, UserMainActivity::class.java))
                        finish() // Finish the current activity to avoid going back

                    } else {
                        showToast("User not found.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Failed to fetch user profile: ${e.message}")
                }
        }
    }

    private fun uploadReelWithWorker(
        caption: String,
        videoUri: Uri?,
        username: String,
        profileImage: String,
        userUID: String,
        reelId: String
    ) {
        val workData = workDataOf(
            "videoUri" to videoUri.toString(),
            "caption" to caption,
            "username" to username,
            "profileImage" to profileImage,
            "visibility" to visibility,
            "hideLikes" to hideLikes,
            "hideShares" to hideShares,
            "hideComments" to hideComments,
            "userUID" to userUID,
            "reelId" to reelId  // Add the reelId to the data
        )

        val uploadWorkRequest = OneTimeWorkRequestBuilder<ReelUploadWorker>()
            .setInputData(workData)
            .build()

        // Enqueue the work request to upload the reel
        WorkManager.getInstance(this).enqueue(uploadWorkRequest)

        // Observe the work's state to determine success or failure
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        // Increment the reels count on success
                        incrementReelsCount(userUID)
                    } else {
                        showToast("Failed to upload reel.")
                    }
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun incrementReelsCount(userId: String?) {
        userId?.let {
            FirebaseFirestore.getInstance().collection("UserDetails").document(it)
                .update("shortsCount", FieldValue.increment(1))
                .addOnSuccessListener {
                    Log.d("ReelsActivity", "Reel count incremented successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("ReelsActivity", "Error incrementing reel count: ${e.message}")
                }
        }
    }


    companion object {
        const val REQUEST_VIDEO_PICK = 1002
    }
}

