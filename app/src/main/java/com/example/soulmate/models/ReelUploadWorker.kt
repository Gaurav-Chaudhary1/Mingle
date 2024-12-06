package com.example.soulmate.models

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log
import android.widget.Toast

class ReelUploadWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val notificationHelper = NotificationHelper(appContext) // NotificationHelper for progress notifications

    override fun doWork(): Result {
        // Extract inputs
        val videoUrl = inputData.getString("videoUri") ?: return Result.failure()
        val caption = inputData.getString("caption") ?: return Result.failure()
        val username = inputData.getString("username") ?: return Result.failure()
        val profileImage = inputData.getString("profileImage") ?: return Result.failure()
        val visibility = inputData.getString("visibility") ?: return Result.failure()
        val hideLikes = inputData.getBoolean("hideLikes", false)
        val hideShares = inputData.getBoolean("hideShares", false)
        val hideComments = inputData.getBoolean("hideComments", false)
        val reelId = inputData.getString("reelId") ?: return Result.failure() // Get reelId from inputData
        val userUID = inputData.getString("userUID") ?: return Result.failure() // Get userUID from inputData

        // Create storage reference for the video file
        val storageRef: StorageReference = FirebaseStorage.getInstance().getReference("reels/$userUID/$reelId.mp4")

        // Upload video to Firebase Storage
        val videoUri = Uri.parse(videoUrl)
        val uploadTask = storageRef.putFile(videoUri)

        // Track progress of the upload and update the notification
        uploadTask.addOnProgressListener { snapshot ->
            // Calculate progress percentage
            val progress = ((100 * snapshot.bytesTransferred) / snapshot.totalByteCount).toInt()
            notificationHelper.showProgressNotification(progress) // Update notification with progress
        }

        // Handle success and failure of the upload
        uploadTask.addOnSuccessListener {
            // Get download URL after successful upload
            storageRef.downloadUrl.addOnSuccessListener { videoDownloadUrl ->
                // Successfully uploaded the video, save data to Firestore
                saveReelDataToFirestore(
                    videoDownloadUrl.toString(),
                    caption,
                    username,
                    profileImage,
                    visibility,
                    hideLikes,
                    hideShares,
                    hideComments,
                    reelId,
                    userUID
                )
            }.addOnFailureListener { e ->
                // Error getting download URL
                notificationHelper.clearNotification() // Clear the progress notification
                Log.e("ReelUploadWorker", "Failed to get video URL: ${e.message}")
                notificationHelper.showUploadCompleteNotification(false) // Show failure notification
            }
        }.addOnFailureListener { e ->
            // Handle failure in uploading the video
            notificationHelper.clearNotification() // Clear the progress notification
            Log.e("ReelUploadWorker", "Error uploading video: ${e.message}")
            Toast.makeText(applicationContext, "Error uploading video", Toast.LENGTH_SHORT).show()
            notificationHelper.showUploadCompleteNotification(false) // Show failure notification
        }

        return Result.success() // Return success even if the notification is not shown here
    }

    private fun saveReelDataToFirestore(
        videoUrl: String,
        caption: String,
        username: String,
        profileImage: String,
        visibility: String,
        hideLikes: Boolean,
        hideShares: Boolean,
        hideComments: Boolean,
        reelId: String,
        userUID: String
    ) {
        val reelData = hashMapOf(
            "videoUrl" to videoUrl,
            "caption" to caption,
            "username" to username,
            "profileImage" to profileImage,
            "visibility" to visibility,
            "hideLikes" to hideLikes,
            "hideShares" to hideShares,
            "hideComments" to hideComments,
            "createdAt" to System.currentTimeMillis(),
            "likeCount" to 0,
            "viewCount" to 0,
            "reelId" to reelId, // Add the reelId to Firestore document
            "userUID" to userUID  // Add the userUID to Firestore document
        )

        // Save the reel data in Firestore
        val reelRef = FirebaseFirestore.getInstance().collection("reels").document(reelId)
        reelRef.set(reelData)
            .addOnSuccessListener {
                // Initialize empty reelsLikes subcollection (if required)
                reelRef.collection("reelsLikes").document("init").set(emptyMap<String, Any>())
                    .addOnSuccessListener {
                        Log.d("ReelUploadWorker", "Reel data uploaded successfully.")
                        Toast.makeText(applicationContext, "Reel Uploaded Successfully", Toast.LENGTH_SHORT).show()
                        notificationHelper.showUploadCompleteNotification(true) // Show success notification
                        notificationHelper.clearNotification() // Clear the progress notification
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReelUploadWorker", "Error initializing reelsLikes: ${e.message}")
                        notificationHelper.showUploadCompleteNotification(false) // Show failure notification
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ReelUploadWorker", "Error uploading reel data: ${e.message}")
                notificationHelper.showUploadCompleteNotification(false) // Show failure notification
            }
    }
}
