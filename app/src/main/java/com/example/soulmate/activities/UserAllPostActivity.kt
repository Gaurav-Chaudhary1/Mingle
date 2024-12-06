package com.example.soulmate.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.soulmate.R
import com.example.soulmate.adapters.UserAllPostAdapter
import com.example.soulmate.models.Post
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserAllPostActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAllPostAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_all_post)

        toolbar = findViewById(R.id.toolbar3)
        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        recyclerView = findViewById(R.id.recycler_view_all_post)

        val postId = intent.getStringExtra("postId") ?: return
        val clickedPosition = intent.getIntExtra("clickedPosition", -1)
        val userUID = intent.getStringExtra("userUID") ?: return

        loadPostDetails(postId, clickedPosition, userUID)
    }

    private fun loadPostDetails(postId: String, clickedPosition: Int, userUID: String) {
        // Fetch all posts by the user
        firestore.collection("posts")
            .whereEqualTo("userUID", userUID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val allPosts = querySnapshot.documents.mapNotNull { it.toObject(Post::class.java) }

                // Find the position of the clicked post
                val clickedPostIndex = allPosts.indexOfFirst { it.postId == postId }

                if (clickedPostIndex != -1) {
                    // If clicked post is found, reorder the list to show clicked post in its original position
                    val reorderedPosts = mutableListOf<Post>()
                    reorderedPosts.addAll(allPosts)

                    // Move the clicked post back to its original position if needed
                    if (clickedPostIndex != clickedPosition && clickedPosition != -1) {
                        val clickedPost = reorderedPosts.removeAt(clickedPostIndex)
                        reorderedPosts.add(clickedPosition, clickedPost)
                    }

                    adapter = UserAllPostAdapter(reorderedPosts)
                    recyclerView.layoutManager = LinearLayoutManager(this)
                    recyclerView.adapter = adapter
                }
            }
            .addOnFailureListener {
                // Handle failure to fetch posts
                Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
    }

}