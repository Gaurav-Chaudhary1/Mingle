package com.example.soulmate.tabfragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.soulmate.activities.UserAllPostActivity
import com.example.soulmate.adapters.PostAdapter
import com.example.soulmate.databinding.FragmentPostsBinding
import com.example.soulmate.models.Post
import com.google.firebase.firestore.FirebaseFirestore

class PostsFragment(private val userUID: String) : Fragment() {

    private lateinit var binding: FragmentPostsBinding
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        fetchPostsFromFirestore()
        return binding.root
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(listOf()) { post, position ->
            navigateToUserAllPostActivity(post.postId, position)
        }
        binding.recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
            addItemDecoration(PostAdapter.ItemOffsetDecoration(4)) // Adjust spacing
            adapter = postAdapter
        }
    }

    private fun fetchPostsFromFirestore() {
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userUID", userUID)
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                if (postList.isNotEmpty()) {
                    postAdapter.updatePosts(postList) // Pass the full post objects
                } else {
                    Toast.makeText(requireContext(), "No posts available.", Toast.LENGTH_SHORT).show()
                    postAdapter.updatePosts(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching posts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToUserAllPostActivity(postId: String, clickedPosition: Int) {
        val intent = Intent(requireContext(), UserAllPostActivity::class.java).apply {
            putExtra("postId", postId)
            putExtra("clickedPosition", clickedPosition)
            putExtra("userUID", userUID)
        }
        startActivity(intent)
    }
}
