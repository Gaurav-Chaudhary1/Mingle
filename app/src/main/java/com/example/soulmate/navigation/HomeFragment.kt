package com.example.soulmate.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soulmate.R
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.adapters.AllUserPostAdapter
import com.example.soulmate.adapters.PostDetailAdapter
import com.example.soulmate.adapters.UserStatusAdapter
import com.example.soulmate.databinding.FragmentHomeBinding
import com.example.soulmate.models.Chat
import com.example.soulmate.models.ChatUser
import com.example.soulmate.models.Post
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: AllUserPostAdapter
    private val postList = mutableListOf<Post>()
    private val firestore = FirebaseFirestore.getInstance()
    private val chatStatusList = mutableListOf<Pair<Chat, ChatUser>>()
    private lateinit var userStatusAdapter: UserStatusAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.messageAnimation.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatFragment)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        setupRecyclerView()
        fetchPosts()

        setupStatusRecyclerView()  // Set up the horizontal RecyclerView
        if (currentUserId != null) {
            loadUserStatus(currentUserId)
        }  // Load users for status display

        return binding.root
    }

    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list and set it to the RecyclerView
        adapter = AllUserPostAdapter(postList)
        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun fetchPosts() {
        // Fetch posts from Firestore
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    // Clear existing posts to avoid duplicates
                    postList.clear()
                    for (doc in snapshot.documents) {
                        val post = doc.toObject(Post::class.java)
                        if (post != null) {
                            postList.add(post)
                        }
                    }
                    // Notify the adapter about data changes
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors
                println("Error fetching posts: ${exception.message}")
            }
    }

    private fun setupStatusRecyclerView() {
        userStatusAdapter = UserStatusAdapter(chatStatusList)
        binding.recyclerViewStatus.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewStatus.adapter = userStatusAdapter
    }

    private fun loadUserStatus(currentUserId: String) {
        val db = FirebaseFirestore.getInstance()
        val chatsRef = db.collection("chats")

        val query1 = chatsRef.whereEqualTo("user1Id", currentUserId)
        val query2 = chatsRef.whereEqualTo("user2Id", currentUserId)

        Tasks.whenAllSuccess<QuerySnapshot>(query1.get(), query2.get()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                chatStatusList.clear()
                task.result.forEach { snapshot ->
                    snapshot.documents.forEach { document ->
                        val chat = document.toObject(Chat::class.java)
                        chat?.let {
                            val otherUserId = if (it.user1Id == currentUserId) it.user2Id else it.user1Id
                            fetchUserDetails(otherUserId)
                        }
                    }
                }
            } else {
                Log.e("HomeFragment", "Error loading user status: ${task.exception?.message}")
            }
        }
    }

    private fun fetchUserDetails(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("UserDetails").document(userId).get().addOnSuccessListener { document ->
            if (document != null) {
                val userDetails = document.toObject(ChatUser::class.java)
                userDetails?.let {
                    chatStatusList.add(Pair(Chat(user1Id = userId, user2Id = ""), it))
                    userStatusAdapter.notifyDataSetChanged()
                }
            }
        }.addOnFailureListener {
            Log.e("ChatFragment", "Failed to fetch user details: ${it.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? UserMainActivity)?.showBottomNavigation() // Show bottom navigation
        updateUserStatus("online")
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus("offline")
    }

    private fun updateUserStatus(status: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            firestore.collection("UserDetails").document(it)
                .update("onlineStatus", status)
                .addOnFailureListener { e ->
                    Log.e("UserDetailsFragment", "Error updating online status: ${e.message}")
                }
        }
    }
}
