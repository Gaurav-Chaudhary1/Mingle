package com.example.soulmate.navigation

import com.example.soulmate.adapters.ProfileTabAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.activities.EditProfileActivity
import com.example.soulmate.activities.PostActivity
import com.example.soulmate.activities.SettingActivity
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.databinding.FragmentUserProfileBinding
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class UserProfileFragment : Fragment() {

    private lateinit var binding: FragmentUserProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var userUID: String? = null
    private var userDetailsListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)

        // Get current user's UID
        userUID = FirebaseAuth.getInstance().currentUser?.uid

        if (userUID == null) {
            showToast("User not authenticated.")
            return binding.root
        }

        // Set up the ViewPager and TabLayout
        setupViewPagerAndTabs(userUID!!)

        // Load user details from Firestore
        loadUserDetailsFromFirestore()

        // Fetch and load profile image from Firebase Storage
        loadProfileImageFromFirebase()

        // Load user interests
        loadUserInterests()

        // Set up button listeners
        setupButtonListeners()

        return binding.root
    }

    private fun setupViewPagerAndTabs(userUID: String) {
        // Set up the adapter for ViewPager2 and pass the userUID to the adapter
        val adapter = ProfileTabAdapter(this, userUID)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.profileTabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Shorts"
                else -> "Tab $position"
            }
        }.attach()
    }

    private fun loadUserDetailsFromFirestore() {
        userUID?.let { uid ->
            userDetailsListenerRegistration = firestore.collection("UserDetails").document(uid)
                .addSnapshotListener { documentSnapshot, e ->

                    if (!isAdded) return@addSnapshotListener

                    if (e != null) {
                        showToast("Error loading user data: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        binding.userName.text = documentSnapshot.getString("fullName") ?: "N/A"
                        binding.userLocation.text = documentSnapshot.getString("address") ?: "N/A"
                        binding.userUsername.text = documentSnapshot.getString("username") ?: "N/A"

                        // Update post count
                        val postsCount = documentSnapshot.getLong("postsCount") ?: 0
                        binding.postsCount.text = postsCount.toString()

                        // Update shorts count
                        val shortsCount = documentSnapshot.getLong("shortsCount") ?: 0
                        binding.shortsCount.text = shortsCount.toString()

                        // Update following count
                        val followingCount = documentSnapshot.getLong("followingCount") ?: 0
                        binding.followingCount.text = followingCount.toString() // Assuming you have a TextView for following count

                        // Show bio if available
                        val bio = documentSnapshot.getString("bio")
                        if (!bio.isNullOrEmpty()) {
                            binding.tvBio.text = bio
                            binding.tvBio.visibility = View.VISIBLE
                        } else {
                            binding.tvBio.visibility = View.GONE
                        }
                    } else {
                        showToast("User data not found.")
                    }
                }
        }
    }

    private fun loadProfileImageFromFirebase() {
        userUID?.let { uid ->
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg")

            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.user_profile_svgrepo_com)
                        .into(binding.profileImage)
                }
                .addOnFailureListener { exception ->
                    showToast("Failed to load profile image: ${exception.message}")
                }
        }
    }

    private fun loadUserInterests() {
        userUID?.let { uid ->
            firestore.collection("UserDetails").document(uid).collection("Interests")
                .limit(5) // Limit to 5 interests
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        binding.chipGroupInterests.visibility = View.GONE
                    } else {
                        binding.chipGroupInterests.removeAllViews() // Clear previous chips
                        for (document in documents) {
                            val interestName = document.getString("interest") // Replace with your field name
                            if (interestName != null) {
                                addChipToGroup(interestName)
                            }
                        }
                        binding.chipGroupInterests.visibility = View.VISIBLE
                        Log.d("UserProfileFragment", "Fetched ${documents.size()} interests.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Error loading interests: ${e.message}")
                }
        }
    }

    private fun addChipToGroup(interest: String) {
        val chip = Chip(requireContext()).apply {
            text = interest
            isCloseIconVisible = false // Set to true if you want to show the close icon
        }
        binding.chipGroupInterests.addView(chip)
    }

    private fun setupButtonListeners() {
        binding.userSetting.setOnClickListener {
            startActivity(Intent(requireContext(), SettingActivity::class.java))
        }

        binding.addPost.setOnClickListener {
            findNavController().navigate(R.id.action_userProfileFragment_to_addFragment)
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_userProfileFragment_to_homeFragment)
        }

        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
    }

    private fun showToast(message: String) {
        // Check if the fragment is attached to an activity
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? UserMainActivity)?.showBottomNavigation() // Show bottom navigation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userDetailsListenerRegistration?.remove()
        userDetailsListenerRegistration = null
    }
}
