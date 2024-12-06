package com.example.soulmate.models

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.soulmate.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "reel_upload_channel"
        const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reel Upload"
            val descriptionText = "Notification channel for reel uploads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(progress: Int) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, do not show notification
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Uploading Reel")
            .setContentText("Reel upload in progress")
            .setSmallIcon(R.drawable.upload)  // Use a vector icon or simple image that fits
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)  // Makes the notification ongoing
            .setProgress(100, progress, false)  // Show progress bar from 0 to 100
            .setSubText("$progress%")  // Show percentage as subtext
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showUploadCompleteNotification(success: Boolean) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, do not show notification
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Reel Upload")
            .setContentText(if (success) "Reel uploaded successfully!" else "Reel upload failed.")
            .setSmallIcon(if (success) R.drawable.done else R.drawable.cancel)  // Make sure these are proper icons
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)  // No longer ongoing after completion
            .setAutoCancel(true)  // Automatically cancels the notification when tapped
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    // Clear the ongoing notification after the upload completes
    fun clearNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
