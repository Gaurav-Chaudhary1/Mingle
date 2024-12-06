package com.example.soulmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.adapters.ProfileTabAdapter
import com.example.soulmate.databinding.ActivityNextUserProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator

class NextUserProfile : AppCompatActivity() {

    private lateinit var binding: ActivityNextUserProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var otherUserUID: String? = null // Store the other user's UID
    private var isFollowing = false
    private lateinit var followerListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNextUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get other user's UID from intent
        otherUserUID = intent.getStringExtra("otherUserUID")
        if (otherUserUID.isNullOrEmpty()) {
            showToast("Error: User not found")
            finish() // Close the activity if no UID is provided
            return
        }

        // Load user details from Firestore
        loadOtherUserDetailsFromFirestore()

        // Fetch and load profile image from Firebase Storage
        loadProfileImageFromFirebase()

        // Load user interests
        loadUserInterests()

        // Set up ViewPager and TabLayout
        setupViewPagerAndTabs()

        // Set up button listeners
        setupButtonListeners()

        // Set up a real-time listener for followers count
        setupFollowersCountListener(otherUserUID!!)
    }

    private fun setupViewPagerAndTabs() {
        val adapter = otherUserUID?.let { ProfileTabAdapter(this, it) }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.profileTabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Shorts"
                else -> "Tab $position"
            }
        }.attach()
    }

    private fun loadOtherUserDetailsFromFirestore() {
        otherUserUID?.let { uid ->
            firestore.collection("UserDetails").document(uid)
                .addSnapshotListener { documentSnapshot, e ->
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

                        // Show bio if available
                        val bio = documentSnapshot.getString("bio")
                        if (!bio.isNullOrEmpty()) {
                            binding.tvBio.text = bio
                            binding.tvBio.visibility = View.VISIBLE
                        } else {
                            binding.tvBio.visibility = View.GONE
                        }

                        // Check following status
                        checkFollowingStatus(uid)
                    } else {
                        showToast("User data not found.")
                    }
                }
        }
    }

    private fun checkFollowingStatus(targetUserUID: String) {
        val currentUserUID = auth.currentUser?.uid ?: return
        firestore.collection("Followers")
            .document(targetUserUID).collection("UserFollowers")
            .document(currentUserUID)
            .get()
            .addOnSuccessListener { document ->
                isFollowing = document.exists()
                updateFollowButtonState()
            }
            .addOnFailureListener { e ->
                showToast("Error checking follow status: ${e.message}")
            }
    }

    private fun updateFollowButtonState() {
        if (isFollowing) {
            binding.followButton.text = "Following"
            binding.followButton.setBackgroundColor(getColor(R.color.grey))
        } else {
            binding.followButton.text = "Follow"
            binding.followButton.setBackgroundColor(getColor(R.color.gray))
        }
    }

    private fun loadProfileImageFromFirebase() {
        otherUserUID?.let { uid ->
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
        otherUserUID?.let { uid ->
            firestore.collection("UserDetails").document(uid).collection("Interests")
                .limit(5) // Limit to 5 interests
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        binding.chipGroupInterests.visibility = View.GONE
                    } else {
                        binding.chipGroupInterests.removeAllViews() // Clear previous chips
                        for (document in documents) {
                            val interestName = document.getString("interest")
                            if (interestName != null) {
                                addChipToGroup(interestName)
                            }
                        }
                        binding.chipGroupInterests.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Error loading interests: ${e.message}")
                }
        }
    }

    private fun addChipToGroup(interest: String) {
        val chip = Chip(this).apply {
            text = interest
            isCloseIconVisible = false
        }
        binding.chipGroupInterests.addView(chip)
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            finish() // Close the activity and go back
        }

        binding.followButton.setOnClickListener {
            toggleFollowStatus()
        }

        binding.messageButton.setOnClickListener {
            val intent = Intent(this, UserChatActivity::class.java).apply {
                putExtra("chatWithUID", otherUserUID)
            }
            startActivity(intent)
        }

        binding.userSetting.setOnClickListener {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.item_profile_bar, null)

            val notInterestedBtn = view.findViewById<MaterialButton>(R.id.notInterested)
            val blockAccount = view.findViewById<MaterialButton>(R.id.blockAccount)
            val reportAccount = view.findViewById<MaterialButton>(R.id.reportAccount)

            // "Not Interested" button action
            notInterestedBtn.setOnClickListener {
                // Logic to hide this profile for the current user
                hideProfileForCurrentUser(otherUserUID!!)
                Toast.makeText(applicationContext, "You won't see this profile anymore.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, UserMainActivity::class.java))
                finish()
                bottomSheet.dismiss()
            }

            // "Block Account" button action
            blockAccount.setOnClickListener {
                // Logic to block the other user
                blockUser(otherUserUID!!)
                Toast.makeText(applicationContext, "This account has been blocked.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, UserMainActivity::class.java))
                finish()
                bottomSheet.dismiss()
            }

            // "Report Account" button action
            reportAccount.setOnClickListener {
                // Open a new bottom sheet dialog for report reasons
                openReportDialog(otherUserUID!!)
                bottomSheet.dismiss()
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()
        }
    }

    private fun hideProfileForCurrentUser(otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Add the other user's ID to the "hiddenProfiles" collection under the current user
        db.collection("UserDetails").document(currentUserId)
            .collection("hiddenProfiles").document(otherUserId)
            .set(mapOf("hidden" to true))
            .addOnSuccessListener {
                Log.d("ShortsFragment", "Profile hidden successfully")
            }
            .addOnFailureListener {
                Log.e("ShortsFragment", "Error hiding profile: ${it.message}")
            }
    }

    private fun blockUser(otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Block the other user by adding their ID to a "blockedUsers" collection
        db.collection("UserDetails").document(currentUserId)
            .collection("blockedUsers").document(otherUserId)
            .set(mapOf("blocked" to true))
            .addOnSuccessListener {
                Log.d("ShortsFragment", "User blocked successfully")

            }
            .addOnFailureListener {
                Log.e("ShortsFragment", "Error blocking user: ${it.message}")
            }
    }

    private fun openReportDialog(otherUserId: String) {
        val reportSheet = BottomSheetDialog(this)
        val reportView = layoutInflater.inflate(R.layout.item_report_reasons, null)

        val reasonGroup = reportView.findViewById<RadioGroup>(R.id.reasonGroup)
        val otherReasonInput = reportView.findViewById<EditText>(R.id.otherReasonInput)
        val reportButton = reportView.findViewById<Button>(R.id.reportButton)

        var selectedReason = ""

        // Show the input field if "Other" reason is selected
        reasonGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.reasonInappropriate -> {
                    selectedReason = "Inappropriate Content"
                    otherReasonInput.visibility = View.GONE
                }
                R.id.reasonSpam -> {
                    selectedReason = "Spam"
                    otherReasonInput.visibility = View.GONE
                }
                R.id.reasonHarassment -> {
                    selectedReason = "Harassment or Hate Speech"
                    otherReasonInput.visibility = View.GONE
                }
                R.id.reasonMisinformation -> {
                    selectedReason = "Misinformation"
                    otherReasonInput.visibility = View.GONE
                }
                R.id.reasonOther -> {
                    selectedReason = ""
                    otherReasonInput.visibility = View.VISIBLE
                }
            }
        }

        // Send the report email when the report button is clicked
        reportButton.setOnClickListener {
            // If "Other" reason is selected, use the text from otherReasonInput
            val finalReason = if (selectedReason.isEmpty() && otherReasonInput.visibility == View.VISIBLE) {
                otherReasonInput.text.toString().takeIf { it.isNotBlank() }
            } else {
                selectedReason
            }

            if (!finalReason.isNullOrEmpty()) {
                Toast.makeText(applicationContext, "Report sent successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, UserMainActivity::class.java))
                finish()
                reportSheet.dismiss()
            } else {
                Toast.makeText(applicationContext, "Please select a reason", Toast.LENGTH_SHORT).show()
            }
        }

        reportSheet.setContentView(reportView)
        reportSheet.show()
    }

    private fun sendReportEmail(otherUserId: String, reason: String) {
        val adminEmail = "gauravchaudhary.gc345@gmail.com"
        val subject = "User Report: $otherUserId"
        val message = "User $otherUserId has been reported for the following reason:\n\n$reason"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send email"))
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "No email app installed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupFollowersCountListener(targetUserUID: String) {
        val targetUserRef = firestore.collection("UserDetails").document(targetUserUID)
        followerListener = targetUserRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) {
                showToast("Error fetching followers count")
                return@addSnapshotListener
            }
            val followersCount = snapshot.getLong("followersCount") ?: 0
            binding.fansCount.text = followersCount.toString()
        }
    }

    private fun toggleFollowStatus() {
        val currentUserUID = auth.currentUser?.uid ?: return
        if (currentUserUID == otherUserUID || otherUserUID == null) {
            showToast("You cannot follow yourself.")
            return
        }
        otherUserUID?.let { targetUserUID ->
            val followButtonRef = firestore.collection("Followers")
                .document(targetUserUID).collection("UserFollowers")
                .document(currentUserUID)

            followButtonRef.get().addOnSuccessListener { document ->
                isFollowing = document.exists() // If document exists, user is already following

                if (isFollowing) {
                    unfollowUser(currentUserUID, targetUserUID)
                } else {
                    followUser(currentUserUID, targetUserUID)
                }
            }
        }
    }

    private fun followUser(currentUserUID: String, targetUserUID: String) {
        val currentUserRef = firestore.collection("UserDetails").document(currentUserUID)
        val targetUserRef = firestore.collection("UserDetails").document(targetUserUID)

        firestore.runTransaction { transaction ->
            // Read both users' data
            val targetUserSnapshot = transaction.get(targetUserRef)
            val currentUserSnapshot = transaction.get(currentUserRef)

            // Ensure the target user exists
            if (!targetUserSnapshot.exists()) {
                throw Exception("Target user does not exist.")
            }

            // Ensure the current user exists
            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user does not exist.")
            }

            // Update target user's followers count
            val currentFollowersCount = targetUserSnapshot.getLong("followersCount") ?: 0
            transaction.update(targetUserRef, "followersCount", currentFollowersCount + 1)

            // Update current user's following count
            val currentFollowingCount = currentUserSnapshot.getLong("followingCount") ?: 0
            transaction.update(currentUserRef, "followingCount", currentFollowingCount + 1)

            // Add to target user's followers list
            val targetUserFollowersRef = firestore.collection("Followers")
                .document(targetUserUID).collection("UserFollowers")
                .document(currentUserUID)
            transaction.set(targetUserFollowersRef, mapOf("followedAt" to System.currentTimeMillis()))

            // Add to current user's following list
            val currentUserFollowingRef = firestore.collection("Following")
                .document(currentUserUID).collection("UserFollowing")
                .document(targetUserUID)
            transaction.set(currentUserFollowingRef, mapOf("followedAt" to System.currentTimeMillis()))
        }.addOnSuccessListener {
            binding.followButton.text = "Following"
            binding.followButton.setBackgroundColor(getColor(R.color.grey))
            isFollowing = true
            showToast("You are now following this user.")
        }.addOnFailureListener { e ->
            showToast("Failed to follow user: ${e.message}")
        }
    }

    private fun unfollowUser(currentUserUID: String, targetUserUID: String) {
        val currentUserRef = firestore.collection("UserDetails").document(currentUserUID)
        val targetUserRef = firestore.collection("UserDetails").document(targetUserUID)

        firestore.runTransaction { transaction ->
            // Read both users' data
            val targetUserSnapshot = transaction.get(targetUserRef)
            val currentUserSnapshot = transaction.get(currentUserRef)

            // Ensure the target user exists
            if (!targetUserSnapshot.exists()) {
                throw Exception("Target user does not exist.")
            }

            // Ensure the current user exists
            if (!currentUserSnapshot.exists()) {
                throw Exception("Current user does not exist.")
            }

            // Get current follower and following counts
            val currentFollowersCount = targetUserSnapshot.getLong("followersCount") ?: 0
            val currentFollowingCount = currentUserSnapshot.getLong("followingCount") ?: 0

            // Debugging log
            Log.d("UnfollowDebug", "Current Followers Count: $currentFollowersCount")
            Log.d("UnfollowDebug", "Current Following Count: $currentFollowingCount")

            // Check if there's something to decrement
            if (currentFollowersCount > 0) {
                transaction.update(targetUserRef, "followersCount", currentFollowersCount - 1)
            } else {
                throw Exception("Cannot unfollow user, no followers to decrement.")
            }

            if (currentFollowingCount > 0) {
                transaction.update(currentUserRef, "followingCount", currentFollowingCount - 1)
            } else {
                throw Exception("Cannot unfollow user, no following to decrement.")
            }

            // Remove from target user's followers list
            val targetUserFollowersRef = firestore.collection("Followers")
                .document(targetUserUID).collection("UserFollowers")
                .document(currentUserUID)
            transaction.delete(targetUserFollowersRef)

            // Remove from current user's following list
            val currentUserFollowingRef = firestore.collection("Following")
                .document(currentUserUID).collection("UserFollowing")
                .document(targetUserUID)
            transaction.delete(currentUserFollowingRef)
        }.addOnSuccessListener {
            binding.followButton.text = "Follow"
            isFollowing = false
            showToast("You have unfollowed this user.")
        }.addOnFailureListener { e ->
            showToast("Failed to unfollow user: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener to prevent memory leaks
        followerListener.remove()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
