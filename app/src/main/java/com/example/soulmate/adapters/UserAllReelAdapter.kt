package com.example.soulmate.adapters

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.databinding.ReelItemBinding
import com.example.soulmate.models.Reel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserAllReelAdapter(private val context: Context, private var reels: List<Reel>) :
    RecyclerView.Adapter<UserAllReelAdapter.UserAllReelViewHolder>() {

    private val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val firestore = FirebaseFirestore.getInstance()
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION

    fun submitList(reels: List<Reel>) {
        this.reels = reels
        notifyDataSetChanged()
    }

    inner class UserAllReelViewHolder(private val binding: ReelItemBinding): RecyclerView.ViewHolder(binding.root) {
        val player: ExoPlayer = ExoPlayer.Builder(binding.root.context).build()

        init {
            binding.videoView.player = player
            binding.videoView.setOnClickListener {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
            binding.likeButtonAnimation.setOnClickListener {
                if (currentUserUID.isNotEmpty()) {
                    likeOrUnlikeReel(reels[adapterPosition].reelId, binding)
                }
            }

            binding.downloadButtonLottie.setOnClickListener {
                downloadVideo(reels[adapterPosition].videoUrl, binding)
            }

            binding.shareButton.setOnClickListener {
                shareVideoLink(reels[adapterPosition].videoUrl)
            }
        }

        fun bind(reel: Reel) {
            // Clear previous media items and set the new media item
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(reel.videoUrl)
            player.setMediaItem(mediaItem)

            // Update UI components
            binding.reelBio.text = reel.caption ?: "No caption available"
            binding.username.text = reel.username
            Glide.with(binding.root.context).load(reel.profileImage.ifEmpty { R.drawable.user_profile_svgrepo_com }).into(binding.profileImage)
            binding.likeBtnCount.text = reel.likeCount.toString()

            // Check if the reel ID is valid before checking if the user liked it
            if (reel.reelId.isNotEmpty()) {
                checkIfUserLikedReel(reel, binding)
            } else {
                binding.likeButtonAnimation.progress = 0f // Default to not liked if reel.id is empty
            }

            // Prepare the player and set the playback state listener
            player.prepare()

            // Add playback state change listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.loadingProgress.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAllReelViewHolder {
        val binding =
            ReelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserAllReelViewHolder(binding)
    }

    override fun getItemCount(): Int = reels.size

    override fun onBindViewHolder(holder: UserAllReelViewHolder, position: Int) {
        val reel = reels[position]
        holder.bind(reel)

        // Auto-play if this is the currently playing position
        holder.player.playWhenReady = (position == currentlyPlayingPosition)
    }

    override fun onViewRecycled(holder: UserAllReelViewHolder) {
        super.onViewRecycled(holder)
        if (holder.player.isPlaying) {
            holder.player.pause()
        }
    }

    private fun checkIfUserLikedReel(reel: Reel, binding: ReelItemBinding) {
        val userUID = currentUserUID.ifEmpty { return }
        val reelLikesRef = firestore.collection("reels").document(reel.reelId)
            .collection("reelsLikes").document(userUID)

        reelLikesRef.get().addOnSuccessListener { document ->
            binding.likeButtonAnimation.progress = if (document.exists()) 0.5f else 0f
        }
    }

    private fun likeOrUnlikeReel(reelId: String, binding: ReelItemBinding) {
        val userUID = currentUserUID.ifEmpty { return }
        val reelRef = firestore.collection("reels").document(reelId)
        val likesSubCollectionRef = reelRef.collection("reelsLikes").document(userUID)

        likesSubCollectionRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                unlikeReel(reelRef, likesSubCollectionRef, binding)
            } else {
                likeReel(reelRef, likesSubCollectionRef, binding)
            }
        }
    }

    private fun likeReel(reelRef: DocumentReference, likesSubCollectionRef: DocumentReference, binding: ReelItemBinding) {
        binding.likeButtonAnimation.playAnimation()
        incrementLikeCount(binding)

        likesSubCollectionRef.set(mapOf("likedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { reelRef.update("likeCount", FieldValue.increment(1)) }
            .addOnFailureListener { decrementLikeCount(binding) }
    }

    private fun unlikeReel(reelRef: DocumentReference, likesSubCollectionRef: DocumentReference, binding: ReelItemBinding) {
        binding.likeButtonAnimation.reverseAnimationSpeed()
        binding.likeButtonAnimation.playAnimation()
        decrementLikeCount(binding)

        likesSubCollectionRef.delete()
            .addOnSuccessListener { reelRef.update("likeCount", FieldValue.increment(-1)) }
            .addOnFailureListener { incrementLikeCount(binding) }
    }

    private fun incrementLikeCount(binding: ReelItemBinding) {
        val newCount = binding.likeBtnCount.text.toString().toInt() + 1
        binding.likeBtnCount.text = newCount.toString()
    }

    private fun decrementLikeCount(binding: ReelItemBinding) {
        val newCount = binding.likeBtnCount.text.toString().toInt() - 1
        binding.likeBtnCount.text = newCount.toString()
    }

    private fun downloadVideo(videoUrl: String, binding: ReelItemBinding) {
        val request = DownloadManager.Request(Uri.parse(videoUrl))
        request.setTitle("Video Download")
        request.setDescription("Downloading video...")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "downloaded_video.mp4")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        // Optional: You can play an animation or show a toast here
        binding.downloadButtonLottie.playAnimation()
    }

    private fun shareVideoLink(videoUrl: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, videoUrl)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share video link"))
    }

    fun setCurrentlyPlayingPosition(newPosition: Int) {
        if (currentlyPlayingPosition == newPosition) return

        val previousPosition = currentlyPlayingPosition
        currentlyPlayingPosition = newPosition
        notifyItemChanged(previousPosition)
        notifyItemChanged(newPosition)
    }
}


