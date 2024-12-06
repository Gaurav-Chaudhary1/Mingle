package com.example.soulmate.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soulmate.R
import com.example.soulmate.activities.UserChatActivity
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.adapters.UserChatAdapter
import com.example.soulmate.adapters.UserStatusAdapter
import com.example.soulmate.databinding.FragmentChatBinding
import com.example.soulmate.models.Chat
import com.example.soulmate.models.ChatUser
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val chatList = mutableListOf<Pair<Chat, ChatUser?>>()
    private lateinit var adapter: UserChatAdapter
    private val originalChatList = mutableListOf<Pair<Chat, ChatUser?>>()
    private val chatStatusList = mutableListOf<Pair<Chat, ChatUser>>()
    private lateinit var userStatusAdapter: UserStatusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("ChatFragment", "Current user is not logged in.")
            return
        }

        setupRecyclerView()
        loadChats(currentUserId)

        setupStatusRecyclerView()  // Set up the horizontal RecyclerView
        loadUserStatus(currentUserId)  // Load users for status display

        binding.chatToolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_homeFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chats_bar_menu, menu)

        val searchItem = menu.findItem(R.id.searchUser)
        val searchView = searchItem.actionView as SearchView

        // Listener for query text changes
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard() // Hide keyboard after submitting
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChats(newText.orEmpty()) // Call filter function on text change
                return true
            }
        })

        // Reset the list when SearchView is closed
        searchView.setOnCloseListener {
            filterChats("") // Reset to the full list
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.action_chatFragment_to_homeFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filterChats(query: String) {
        if (query.isEmpty()) {
            chatList.clear()
            chatList.addAll(originalChatList)
        } else {
            val filteredList = originalChatList.filter { pair ->
                val userProfile = pair.second
                val chat = pair.first

                val matchesUserName = userProfile?.fullName?.contains(query, ignoreCase = true) == true
                val matchesMessage = chat.lastMessage.contains(query, ignoreCase = true)

                matchesUserName || matchesMessage
            }.sortedByDescending { pair ->
                val userProfile = pair.second
                val chat = pair.first
                val matchesUserName = userProfile?.fullName?.contains(query, ignoreCase = true) == true
                val matchesMessage = chat.lastMessage.contains(query, ignoreCase = true)

                when {
                    matchesUserName -> 2
                    matchesMessage -> 1
                    else -> 0
                }
            }

            chatList.clear()
            chatList.addAll(filteredList)
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
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
                Log.e("ChatFragment", "Error loading user status: ${task.exception?.message}")
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

    private fun setupRecyclerView() {
        adapter = UserChatAdapter(chatList)
        adapter.setOnClickListener(object : UserChatAdapter.OnClickListener {
            override fun onClick(chatId: String, otherUserId: String) {
                val intent = Intent(requireContext(), UserChatActivity::class.java).apply {
                    putExtra("chatWithUID", otherUserId)
                }
                startActivity(intent)
            }
        })
        binding.rvUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUserList.adapter = adapter
    }

    private fun loadChats(currentUserId: String) {
        val db = FirebaseFirestore.getInstance()
        val chatsRef = db.collection("chats")

        val query1 = chatsRef.whereEqualTo("user1Id", currentUserId)
        val query2 = chatsRef.whereEqualTo("user2Id", currentUserId)

        Tasks.whenAllSuccess<QuerySnapshot>(query1.get(), query2.get()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                chatList.clear()
                task.result.forEach { snapshot ->
                    snapshot.documents.forEach { document ->
                        val chat = document.toObject(Chat::class.java)
                        chat?.let {
                            chatList.add(Pair(it, null))
                        }
                    }
                }
                loadUserDetailsForChats(currentUserId) // Load user details for all chats
            } else {
                Log.e("ChatFragment", "Error loading chats: ${task.exception?.message}")
            }
        }
    }


    private fun loadUserDetailsForChats(currentUserId: String) {
        val db = FirebaseFirestore.getInstance()
        val fetchTasks = chatList.map { pair ->
            val chat = pair.first
            val otherUserId = if (chat.user1Id == currentUserId) chat.user2Id else chat.user1Id

            db.collection("UserDetails")
                .document(otherUserId)
                .get()
                .continueWith { task ->
                    if (task.isSuccessful && task.result != null) {
                        val userDetails = task.result?.toObject(ChatUser::class.java)?.apply {
                            uid = task.result?.id // Assign the document ID to uid
                        }
                        Pair(chat, userDetails)
                    } else {
                        Log.e("ChatFragment", "Error fetching user details for $otherUserId")
                        Pair(chat, null)
                    }
                }
        }

        Tasks.whenAllSuccess<Pair<Chat, ChatUser?>>(fetchTasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val results = task.result as List<Pair<Chat, ChatUser?>>
                chatList.clear()
                chatList.addAll(results)

                // Ensure originalChatList is populated only once
                if (originalChatList.isEmpty()) {
                    originalChatList.addAll(results)
                }

                adapter.notifyDataSetChanged()
                updateEmptyState()
            } else {
                Log.e("ChatFragment", "Error completing user detail fetch tasks")
            }
        }
    }


    private fun updateEmptyState() {
        // If there are no chats, show the "No Chats" state
        if (chatList.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.emptyStateText.visibility = View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("online")
        (activity as? UserMainActivity)?.hideBottomNavigation() // Hide bottom navigation
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
