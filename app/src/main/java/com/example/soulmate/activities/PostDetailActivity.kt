package com.example.soulmate.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.soulmate.R
import com.example.soulmate.adapters.PostDetailAdapter
import com.example.soulmate.models.Post
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class PostDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postDetailAdapter: PostDetailAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        toolbar = findViewById(R.id.toolbar2)

        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        recyclerView = findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val postId = intent.getStringExtra("postId") ?: return
        loadPostDetails(postId)
    }

    private fun loadPostDetails(postId: String) {
        // Get the clicked post
        firestore.collection("posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val clickedPost =
                        document.toObject(Post::class.java) ?: return@addOnSuccessListener

                    // Get other posts from the Firestore collection
                    firestore.collection("posts")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val posts =
                                querySnapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                            val allPosts =
                                mutableListOf(clickedPost).apply { addAll(posts.filter { it.postId != postId }) }

                            postDetailAdapter = PostDetailAdapter(allPosts)
                            recyclerView.adapter = postDetailAdapter
                        }
                }
            }
    }
}