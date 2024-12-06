package com.example.soulmate.adapters

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.soulmate.databinding.ItemShortBinding
import com.example.soulmate.models.Reel

class ShortsAdapter(
    private val context: Context,
    private val shorts: List<Reel>,
    private val listener: OnReelClickListener
) : RecyclerView.Adapter<ShortsAdapter.ShortsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortsViewHolder {
        val binding = ItemShortBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShortsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortsViewHolder, position: Int) {
        holder.bind(shorts[position])
    }

    override fun getItemCount(): Int = shorts.size

    inner class ShortsViewHolder(private val binding: ItemShortBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(shorts: Reel) {
            // Extract and display the first frame of the video as a thumbnail
            val thumbnail = getVideoThumbnail(shorts.videoUrl)
            if (thumbnail != null) {
                binding.reelThumbnail.setImageBitmap(thumbnail)
            }

            // Set up click listener to open UserAllReelActivity
            binding.reelThumbnail.setOnClickListener {
                listener.onReelClick(shorts)
            }
        }
    }

    private fun getVideoThumbnail(videoUrl: String): Bitmap? {
        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoUrl, HashMap()) // Set the video URL
            bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) // 1st second
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return bitmap
    }

    interface OnReelClickListener {
        fun onReelClick(reel: Reel)
    }
}
