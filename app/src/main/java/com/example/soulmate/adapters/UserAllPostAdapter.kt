package com.example.soulmate.adapters

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.activities.NextUserProfile
import com.example.soulmate.fragments.CommentsBottomSheetFragment
import com.example.soulmate.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class UserAllPostAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<UserAllPostAdapter.UserAllPostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    inner class UserAllPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.edt_profile)
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userBio: TextView = itemView.findViewById(R.id.user_bio)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val likeButton: LottieAnimationView =
            itemView.findViewById(R.id.likeButtonAnimation) // Lottie for like button
        private val likeCountText: TextView =
            itemView.findViewById(R.id.likeBtnCount) // Your like count TextView
        private val shareButton: ImageButton = itemView.findViewById(R.id.share_button)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.save_button)
        private val comment: ImageButton = itemView.findViewById(R.id.comment)

        fun bind(post: Post) {
            // Display user info and post image
            userBio.text = post.caption
            Glide.with(itemView.context).load(post.imageUrl).into(postImage)

            // Load user details
            firestore.collection("UserDetails").document(post.userUID)
                .get()
                .addOnSuccessListener { userDoc ->
                    userName.text = userDoc.getString("fullName")
                    val profileImageUrl = userDoc.getString("profileImage")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(itemView.context).load(profileImageUrl).into(profileImage)
                    }
                }

            shareButton.setOnClickListener {
                // Replace "yourPostId" with the actual ID of the post you want to share
                val postId = post.postId
                val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)

                postRef.get().addOnSuccessListener { document ->
                    if (document != null && document.contains("imageUrl")) {
                        val imageUrl = document.getString("imageUrl") ?: ""
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, imageUrl)
                        }
                        itemView.context.startActivity(
                            Intent.createChooser(
                                intent,
                                "Share Post Link"
                            )
                        )
                    } else {
                        // Handle case where imageUrl is not found
                        Toast.makeText(itemView.context, "Image URL not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.addOnFailureListener { e ->
                    // Handle any errors
                    Toast.makeText(
                        itemView.context,
                        "Failed to fetch post link: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            comment.setOnClickListener {
                val commentBottomSheet = CommentsBottomSheetFragment(post.postId)
                commentBottomSheet.show((itemView.context as AppCompatActivity).supportFragmentManager, "CommentBottomSheet")
            }

            downloadButton.setOnClickListener {
                // Replace "yourPostId" with the actual ID of the post you want to download
                val postId = post.postId
                val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)

                postRef.get().addOnSuccessListener { document ->
                    if (document != null && document.contains("imageUrl")) {
                        val downloadUrl = document.getString("imageUrl")
                        if (downloadUrl != null) {
                            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                setTitle("Post Image")
                                setDescription("Downloading post content")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    "post_image.jpg"
                                )
                            }
                            val downloadManager =
                                itemView.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            downloadManager.enqueue(request)
                            Toast.makeText(itemView.context, "Download started", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        // Handle case where imageUrl is not found
                        Toast.makeText(itemView.context, "Image URL not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.addOnFailureListener { e ->
                    // Handle any errors
                    Toast.makeText(
                        itemView.context,
                        "Failed to fetch download URL: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


            // Set like count
            likeCountText.text = post.likeCount.toString()

            // Check if the user has liked this post
            checkIfUserLikedPost(post)

            // Handle like button click
            likeButton.setOnClickListener {
                if (currentUserUID.isNotEmpty()) {
                    likeOrUnlikePost(post.postId, likeCountText, likeButton)
                }
            }
        }

        private fun likeOrUnlikePost(
            postId: String,
            likeCountText: TextView,
            likeButton: LottieAnimationView
        ) {
            val postRef = firestore.collection("posts").document(postId)
            val likesSubCollectionRef = postRef.collection("postsLikes").document(currentUserUID)

            likesSubCollectionRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // User has liked the post, so we will unlike it
                    unlikePost(postRef, likesSubCollectionRef, likeCountText, likeButton)
                } else {
                    // User has not liked the post, so we will like it
                    likePost(postRef, likesSubCollectionRef, likeCountText, likeButton)
                }
            }
        }

        private fun likePost(
            postRef: DocumentReference,
            likesSubCollectionRef: DocumentReference,
            likeCountText: TextView,
            likeButton: LottieAnimationView
        ) {
            likesSubCollectionRef.set(mapOf("likedAt" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    postRef.update("likeCount", FieldValue.increment(1))
                    // Update UI
                    updateLikeCount(likeCountText, 1)
                    likeButton.playAnimation() // Play animation on like
                }
        }

        private fun unlikePost(
            postRef: DocumentReference,
            likesSubCollectionRef: DocumentReference,
            likeCountText: TextView,
            likeButton: LottieAnimationView
        ) {
            likesSubCollectionRef.delete()
                .addOnSuccessListener {
                    postRef.update("likeCount", FieldValue.increment(-1))
                    // Update UI
                    updateLikeCount(likeCountText, -1)
                    likeButton.reverseAnimationSpeed() // Reverse speed for unliking
                    likeButton.playAnimation() // Play animation on unlike
                }
        }

        private fun updateLikeCount(likeCountText: TextView, increment: Int) {
            val currentCount = likeCountText.text.toString().toIntOrNull() ?: 0
            likeCountText.text = (currentCount + increment).toString()
        }

        private fun checkIfUserLikedPost(post: Post) {
            val postLikesRef = firestore.collection("posts").document(post.postId)
                .collection("postsLikes").document(currentUserUID)

            postLikesRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    likeButton.progress = 0.5f // Indicate liked state
                } else {
                    likeButton.progress = 0f // Indicate not liked state
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAllPostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return UserAllPostViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: UserAllPostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }
}