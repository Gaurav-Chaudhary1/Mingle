package com.example.soulmate.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soulmate.R
import com.example.soulmate.activities.MatchingActivity
import com.example.soulmate.activities.NextUserProfile
import com.example.soulmate.activities.PostDetailActivity
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.adapters.PostGridAdapter
import com.example.soulmate.adapters.UserMatchAdapter
import com.example.soulmate.databinding.FragmentSearchBinding
import com.example.soulmate.models.ChatUser
import com.example.soulmate.models.Post
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var gestureDetector: GestureDetector
    private var isSwiping: Boolean = false

    private lateinit var userMatchAdapter: UserMatchAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var adapter: PostGridAdapter
    private val postList = mutableListOf<Post>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        setupInterestChips()
        setupGestureDetection() // Set up swipe gesture detection

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_homeFragment)
        }

        binding.fab.setOnClickListener {
        startActivity(Intent(requireContext(), MatchingActivity::class.java))
            requireActivity().finish()
        }

        setupRecyclerView()
        fetchUsersForMatch() // Fetch users for the RecyclerView

        setupPostRecyclerView()
        fetchShuffledPosts()

        return binding.root
    }

    private fun setupPostRecyclerView() {
        adapter = PostGridAdapter(postList) { post ->
            // Navigate to post detail page when post is clicked
            navigateToPostDetail(post.postId)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter
    }


    private fun fetchShuffledPosts() {
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                postList.clear()
                for (document in snapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                postList.shuffle() // Shuffle posts for randomness
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("SearchFragment", "Error fetching posts: ${e.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        (activity as? UserMainActivity)?.hideBottomNavigation() // Hide bottom navigation
    }

    private fun setupRecyclerView() {
        userMatchAdapter = UserMatchAdapter(emptyList())
        binding.hotMatchesCarousel.apply {
            adapter = userMatchAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun fetchUsersForMatch() {
        if (currentUserUid == null) return

        // Get current user's gender to fetch opposite gender
        firestore.collection("UserDetails").document(currentUserUid).get()
            .addOnSuccessListener { document ->
                val currentUserGender = document.getString("gender") ?: return@addOnSuccessListener
                val oppositeGender = if (currentUserGender.equals("Male", ignoreCase = true)) "Female" else "Male"

                // Fetch opposite gender users
                firestore.collection("UserDetails")
                    .whereEqualTo("gender", oppositeGender)
                    .limit(20)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val users = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ChatUser::class.java)?.apply {
                                uid = doc.id
                            }
                        }
                        updateRecyclerView(users)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun updateRecyclerView(users: List<ChatUser>) {
        userMatchAdapter = UserMatchAdapter(users)
        binding.hotMatchesCarousel.adapter = userMatchAdapter

        userMatchAdapter.setOnItemClickListener { chatUser ->
            chatUser.uid?.let { uid ->
                navigateToUserProfile(uid)
            } ?: run {
                Toast.makeText(requireContext(), "User ID is missing.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToUserProfile(userId: String) {
        val intent = Intent(requireContext(), NextUserProfile::class.java)
        intent.putExtra("otherUserUID", userId)
        startActivity(intent)
    }

    // Sets up the onClick listeners for each chip in the interest carousel
    private fun setupInterestChips() {
        // Get the HorizontalScrollView
        val scrollView = binding.interestCarousel

        // Get the LinearLayout inside the ScrollView
        val linearLayout = scrollView.getChildAt(0) as? LinearLayout
        linearLayout?.let {
            for (i in 0 until linearLayout.childCount) {
                val chip = linearLayout.getChildAt(i) as? Chip
                chip?.setOnClickListener {
                    val selectedInterest = chip.text.toString()
                    Log.d("ChipClick", "Clicked on: $selectedInterest")
                    navigateToFilteredProfiles(selectedInterest)
                }
            }
        }
    }


    // Set up gesture detection for swiping
    private fun setupGestureDetection() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val deltaX = e2.x - e1.x
                    if (deltaX > 100) { // Right swipe threshold
                        initiateQuickMatch()
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                binding.quickMatchSection.performClick() // Call performClick for accessibility
                return true
            }
        })

        // Set touch listener on the quickMatchSection to detect swipe gestures
        binding.quickMatchSection.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    // Navigates to FilteredProfilesFragment, passing the selected interest as an argument
    private fun navigateToFilteredProfiles(interest: String) {
        val bundle = Bundle().apply {
            putString("interest", interest)
        }
        findNavController().navigate(R.id.action_searchFragment_to_filteredProfilesFragment, bundle)
    }

    private fun initiateQuickMatch() {
        if (isSwiping) return // Prevent multiple swipes
        isSwiping = true
        binding.swipeText.text = "Finding your match, please wait..."

        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Get current user's gender
        db.collection("UserDetails").document(currentUserId).get().addOnSuccessListener { document ->
            val currentUserGender = document.getString("gender") ?: return@addOnSuccessListener
            val oppositeGender = if (currentUserGender == "Male") "Female" else "Male" // Adjusted for case sensitivity

            // Query Firestore for users of the opposite gender
            db.collection("UserDetails")
                .whereEqualTo("gender", oppositeGender)
                .limit(1) // Retrieve one match for demonstration purposes
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("QuickMatch", "Match snapshot size: ${snapshot.size()}") // Log the size of the snapshot
                    if (snapshot != null && !snapshot.isEmpty) {
                        val matchUser = snapshot.documents[0]
                        showMatch(matchUser)
                    } else {
                        showNoMatchFound()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QuickMatch", "Error fetching match: ${e.message}")
                    showErrorFetchingMatch()
                }
                .addOnCompleteListener {
                    // Resetting the swipe state after operation
                    isSwiping = false
                }
        }.addOnFailureListener { e ->
            Log.e("QuickMatch", "Error fetching user gender: ${e.message}")
            showErrorFetchingMatch()
            isSwiping = false
        }
    }

    private fun showMatch(matchUser: DocumentSnapshot) {
        // Display the matched user information
        val matchName = matchUser.getString("fullName") ?: "Match found!"
        binding.swipeText.text = matchName
        binding.swipeText.setOnClickListener {
            // Navigate to the matched user's profile
            val userId = matchUser.id

            // Create an Intent to start the NextUserProfile activity
            val intent = Intent(requireContext(), NextUserProfile::class.java)
            // Put the userId as an extra in the Intent
            intent.putExtra("otherUserUID", userId)

            // Start the NextUserProfile activity
            startActivity(intent)
        }
    }

    private fun showNoMatchFound() {
        binding.swipeText.text = "No quick matches available."
    }

    private fun showErrorFetchingMatch() {
        Toast.makeText(requireContext(), "Error fetching match.", Toast.LENGTH_SHORT).show()
        binding.swipeText.text = "Swipe for Quick Match" // Reset text on error
    }

    private fun navigateToPostDetail(postId: String) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java)
        intent.putExtra("postId", postId)
        startActivity(intent)
    }
}
