package com.example.soulmate.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.soulmate.R
import com.example.soulmate.models.Comments
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsAdapter(
    private val commentsList: MutableList<Comments>, // Use MutableList for easy modifications
    private val postId: String // Pass postId to identify the comments for the specific post
) : RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder>() {

    inner class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewComment: TextView = itemView.findViewById(R.id.textViewComment)
        val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val buttonDeleteComment: ImageButton = itemView.findViewById(R.id.buttonDeleteComment)
        val imageViewProfile: CircleImageView = itemView.findViewById(R.id.edt_profile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentsViewHolder(view)
    }

    override fun getItemCount(): Int = commentsList.size

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.textViewComment.text = comment.commentText
        holder.textViewTimestamp.text = formatTimestamp(comment.timestamp)
        holder.textViewUserName.text = comment.userFullName

        // Load profile image from Firebase with error handling
        Glide.with(holder.itemView.context)
            .load(comment.profileImageUrl)
            .apply(RequestOptions().error(R.drawable.user_pro)) // Placeholder on error
            .into(holder.imageViewProfile)

        // Show delete button only for the comment owner
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        holder.buttonDeleteComment.visibility = if (currentUserId == comment.userUID) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Set up delete button click listener
        holder.buttonDeleteComment.setOnClickListener {
            deleteComment(comment.commentId, holder.adapterPosition)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun deleteComment(commentId: String?, position: Int) {
        if (commentId == null) {
            Log.e("CommentsAdapter", "Comment ID is null, cannot delete comment.")
            return
        }

        val commentsCollectionRef = FirebaseFirestore.getInstance()
            .collection("posts").document(postId).collection("comments").document(commentId)

        commentsCollectionRef.delete()
            .addOnSuccessListener {
                // Ensure position is still within bounds before removing
                if (position >= 0 && position < commentsList.size) {
                    commentsList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, commentsList.size) // Update the range
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommentsAdapter", "Error deleting comment: ${e.message}")
            }
    }
}
