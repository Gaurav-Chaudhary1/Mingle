package com.example.soulmate.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.databinding.ActivityPostBinding
import com.example.soulmate.utils.ProgressDialogUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class PostActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPostBinding.inflate(layoutInflater)
    }

    private var profileImageUri: Uri? = null
    private lateinit var loadingDialog: ProgressDialogUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadingDialog = ProgressDialogUtil()

        loadProfileImageFromFirebase()

        binding.closeBtn.setOnClickListener {
            navigateToUserMainActivity()
        }

        binding.privacyDropdown.setOnClickListener {
            showVisibilityOptions()
        }

        binding.idIVOriginalImage.setOnClickListener {
            openImagePicker()
        }

        binding.postButton.setOnClickListener {
            checkAndPost()
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            profileImageUri = data.data
            binding.idIVOriginalImage.setImageURI(profileImageUri)
        } else {
            showToast("Image selection was cancelled.")
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
                bottomSheet.dismiss()
            }, 200)
        }

        friendsRadioButton.setOnClickListener {
            it.postDelayed({
                binding.privacyDropdown.text = "Friends only ▼"
                bottomSheet.dismiss()
            }, 200)
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun checkAndPost() {
        val caption = binding.edtCaption.text.toString().trim()

        if (profileImageUri == null) {
            showToast("Please select an image!")
            return
        }

        if (caption.isEmpty()) {
            showToast("Please write a caption!")
            return
        }

        uploadPostToFirebase(caption, profileImageUri)
    }

    private fun uploadPostToFirebase(caption: String, imageUri: Uri?) {
        loadingDialog.showProgressDialog(this, "Uploading post...")
        val storageRef = FirebaseStorage.getInstance().getReference("posts/${System.currentTimeMillis()}.jpg")

        imageUri?.let {
            storageRef.putFile(it)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        savePostDataToFirestore(uri.toString(), caption)
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.hideProgressDialog()
                    showToast("Failed to upload image: ${e.message}")
                }
        }
    }

    private fun savePostDataToFirestore(imageUrl: String, caption: String) {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid
        val postDocumentRef = FirebaseFirestore.getInstance().collection("posts").document()

        val postId = postDocumentRef.id

        val post = hashMapOf(
            "postId" to postId,
            "userUID" to userUID,
            "caption" to caption,
            "imageUrl" to imageUrl,
            "createdAt" to System.currentTimeMillis(),
            "likeCount" to 0,
            "commentCount" to 0,
            "visibility" to binding.privacyDropdown.text.toString(),
            "hideLikes" to binding.hideLikeToggle.isChecked,
            "hideShares" to binding.hideShareToggle.isChecked,
            "hideComments" to binding.hideCommentToggle.isChecked
        )

        postDocumentRef.set(post)
            .addOnSuccessListener {
                loadingDialog.hideProgressDialog()
                incrementPostsCount(userUID)

                postDocumentRef.collection("postsLikes").document("init").set(emptyMap<String, Any>())
                showToast("Post uploaded successfully!")
                binding.root.postDelayed({
                    navigateToUserMainActivity()
                }, 1000)
            }
            .addOnFailureListener { e ->
                loadingDialog.hideProgressDialog()
                showToast("Failed to upload post: ${e.message}")
            }
    }

    private fun incrementPostsCount(userId: String?) {
        userId?.let {
            FirebaseFirestore.getInstance().collection("UserDetails").document(it)
                .update("postsCount", FieldValue.increment(1))
                .addOnSuccessListener {
                    Log.d("PostActivity", "Post count incremented successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("PostActivity", "Error incrementing post count: ${e.message}")
                }
        }
    }

    private fun initializeCommentDocument(postId: String, commentText: String) {
        val commentsCollectionRef = FirebaseFirestore.getInstance()
            .collection("posts").document(postId).collection("comments")

        val commentId = commentsCollectionRef.document().id
        val commentData = hashMapOf(
            "commentId" to commentId,
            "userUID" to FirebaseAuth.getInstance().currentUser?.uid,
            "commentText" to commentText,
            "timestamp" to System.currentTimeMillis(),
            "userFullName" to "fullName", // Replace with actual user's full name
            "profileImageUrl" to "profileImage" // Replace with actual profile image URL
        )

        commentsCollectionRef.document(commentId).set(commentData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("PostActivity", "Comment added successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("PostActivity", "Error adding comment: ${e.message}")
            }
    }

    private fun navigateToUserMainActivity() {
        startActivity(Intent(this, UserMainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1001
    }
}
