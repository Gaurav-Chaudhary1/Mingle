package com.example.soulmate.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soulmate.R
import com.example.soulmate.adapters.CommentsAdapter
import com.example.soulmate.models.Comments
import com.example.soulmate.models.Post // Import your Post model
import com.example.soulmate.databinding.FragmentCommentsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentsBottomSheetFragment(private val postId: String) : BottomSheetDialogFragment() { // Accept postId as a parameter
    private var _binding: FragmentCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val commentsList = mutableListOf<Comments>()
    private lateinit var commentsAdapter: CommentsAdapter
    private val firestore = FirebaseFirestore.getInstance()

    // Companion object for creating a new instance of CommentsBottomSheetFragment
    companion object {
        fun newInstance(postId: String): CommentsBottomSheetFragment {
            return CommentsBottomSheetFragment(postId) // Pass the postId directly
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchComments()

        binding.buttonPostComment.setOnClickListener {
            val commentText = binding.editTextComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(commentsList, postId) // Pass the postId to the adapter
        binding.recyclerViewComments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewComments.adapter = commentsAdapter
    }

    private fun fetchComments() {
        firestore.collection("posts").document(postId).collection("comments")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CommentsFragment", "Listen failed.", e)
                    Toast.makeText(requireContext(), "Error loading comments", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                commentsList.clear()
                snapshot?.documents?.forEach { document ->
                    val comment = document.toObject(Comments::class.java)?.apply {
                        commentId = document.id // Store the comment ID
                    }
                    comment?.let { commentsList.add(it) }
                }
                commentsAdapter.notifyDataSetChanged() // Notify the adapter to refresh the list
            }
    }

    private fun addComment(commentText: String) {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("UserDetails").document(userUID)
            .get()
            .addOnSuccessListener { userDocument ->
                val userFullName = userDocument.getString("fullName") ?: "Unknown User"
                val profileImageUrl = userDocument.getString("profileImage") ?: ""

                // Create a new document reference for a unique comment ID
                val commentRef = firestore.collection("posts").document(postId)
                    .collection("comments").document()

                val newComment = Comments(
                    commentId = commentRef.id, // Assign the generated document ID
                    commentText = commentText,
                    userUID = userUID,
                    userFullName = userFullName,
                    profileImageUrl = profileImageUrl,
                    timestamp = System.currentTimeMillis()
                )

                // Use set instead of add to explicitly save the comment with the assigned ID
                commentRef.set(newComment)
                    .addOnSuccessListener {
                        binding.editTextComment.text.clear() // Clear the input field
                    }
                    .addOnFailureListener { e ->
                        Log.e("CommentsFragment", "Error adding comment: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CommentsFragment", "Error fetching user data: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
