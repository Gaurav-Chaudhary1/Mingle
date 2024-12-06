package com.example.soulmate.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.databinding.ItemCardBinding
import com.example.soulmate.models.Card
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.CardStackView

class CardStackAdapter(
    private val currentUserGender: String,
    private val cardStackView: CardStackView
) : RecyclerView.Adapter<CardStackAdapter.CardViewHolder>() {

    private val cardList = mutableListOf<Card>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val likedUserIds = mutableSetOf<String>() // To keep track of liked users
    private val dislikedUserIds = mutableSetOf<String>() // To keep track of disliked users

    inner class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: Card) {
            binding.titleTextView.text = card.fullName
            binding.bioTextView.text = shortenBio(card.bio)

            Glide.with(binding.root.context)
                .load(card.profileImage)
                .placeholder(com.facebook.login.R.drawable.com_facebook_profile_picture_blank_portrait)
                .into(binding.imageView)

            // Handle Like button click
            binding.likeButton.setOnClickListener {
                onLike(card)
            }

            // Handle Dislike button click
            binding.dislikeButton.setOnClickListener {
                onDislike(card)
            }

            // Handle Refresh button click
            binding.refreshButton.setOnClickListener {
                onRefresh() // Fetch 10 new users on refresh
            }
        }

        private fun onDislike(card: Card) {
            // Simulate swipe left (dislike)
            Toast.makeText(binding.root.context, "Disliked ${card.fullName}", Toast.LENGTH_SHORT).show()
            dislikedUserIds.add(card.userId) // Add to disliked users
            cardList.removeAt(adapterPosition) // Remove from list
            notifyItemRemoved(adapterPosition)
            storeSwipeAction(card.userId, "dislike") // Store action in Firestore
        }

        private fun onLike(card: Card) {
            // Simulate swipe right (like)
            Toast.makeText(binding.root.context, "Liked ${card.fullName}! It's a match!", Toast.LENGTH_SHORT).show()
            likedUserIds.add(card.userId) // Add to liked users
            cardList.removeAt(adapterPosition) // Remove from list
            notifyItemRemoved(adapterPosition)
            storeSwipeAction(card.userId, "like") // Store action in Firestore
        }

        private fun shortenBio(bio: String): String {
            return if (bio.length > 50) bio.take(50) + "..." else bio
        }

        // Store swipe actions (like/dislike) in Firestore
        private fun storeSwipeAction(swipedUserId: String, action: String) {
            val swipeData = mapOf(
                "currentUserId" to currentUserId,
                "swipedUserId" to swipedUserId,
                "action" to action,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("Swipes")
                .add(swipeData)
                .addOnSuccessListener {
                    Log.d("CardStackAdapter", "Swipe action ($action) stored successfully")

                    // Check for mutual like
                    if (action == "like") {
                        checkForMatch(swipedUserId)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("CardStackAdapter", "Error storing swipe action: ${exception.message}")
                }
        }

        // New method to check for mutual matches
        private fun checkForMatch(swipedUserId: String) {
            db.collection("Swipes")
                .whereEqualTo("currentUserId", swipedUserId)
                .whereEqualTo("swipedUserId", currentUserId)
                .whereEqualTo("action", "like")
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Log.d("CardStackAdapter", "It's a match!")
                        Toast.makeText(cardStackView.context, "It's a match!", Toast.LENGTH_SHORT).show()
                        // Handle the match event here (e.g., notify both users)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("CardStackAdapter", "Error checking for match: ${exception.message}")
                }
        }

        // Handle refresh action to fetch 10 more users
        private fun onRefresh() {
            Toast.makeText(binding.root.context, "Fetching 10 more users...", Toast.LENGTH_SHORT).show()
            fetchMoreUsers(10) // Fetch 10 new users
        }
    }

    // Fetch profiles based on the user's opposite gender who haven't been swiped on yet
    fun fetchOppositeGenderProfiles() {
        val oppositeGender = if (currentUserGender == "Male") "Female" else "Male"

        // Fetch previous swipe actions to exclude them
        db.collection("Swipes")
            .whereEqualTo("currentUserId", currentUserId)
            .get()
            .addOnSuccessListener { swipeDocuments ->
                val alreadySwipedUserIds = swipeDocuments.map { it.getString("swipedUserId")!! }.toSet()

                fetchUsers(oppositeGender, alreadySwipedUserIds, 20) // Fetch 20 users initially
            }
            .addOnFailureListener { exception ->
                Log.e("CardAdapter", "Error fetching swipes: ${exception.message}")
            }
    }

    // Fetch a limited number of users from Firestore (used for both initial fetch and refresh)
    private fun fetchUsers(gender: String, excludeUserIds: Set<String>, limit: Long) {
        db.collection("UserDetails")
            .whereEqualTo("gender", gender)
            .limit(limit)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userId = document.id
                    // Exclude users who have already been liked or disliked
                    if (!excludeUserIds.contains(userId)) {
                        val card = Card(
                            userId = userId,
                            fullName = document.getString("fullName") ?: "Unknown",
                            bio = document.getString("bio") ?: "No bio available",
                            profileImage = document.getString("profileImage") ?: "https://example.com/default.jpg"
                        )
                        cardList.add(card)
                    }
                }
                notifyDataSetChanged() // Notify the adapter that data has changed
            }
            .addOnFailureListener { exception ->
                Log.e("CardAdapter", "Error fetching profiles: ${exception.message}")
            }
    }

    // Fetch 10 more users on refresh (excluding already swiped users)
    fun fetchMoreUsers(limit: Long) {
        val oppositeGender = if (currentUserGender == "Male") "Female" else "Male"

        db.collection("Swipes")
            .whereEqualTo("currentUserId", currentUserId)
            .get()
            .addOnSuccessListener { swipeDocuments ->
                val alreadySwipedUserIds = swipeDocuments.map { it.getString("swipedUserId")!! }.toSet()

                fetchUsers(oppositeGender, alreadySwipedUserIds, limit) // Fetch limited users (e.g., 10)
            }
            .addOnFailureListener { exception ->
                Log.e("CardAdapter", "Error fetching swipes: ${exception.message}")
            }
    }

    // New method: Get the card at a specific position
    fun getCardAt(position: Int): Card {
        return cardList[position]
    }

    // New method: Handle swiped cards in activity (right for like, left for dislike)
    fun onCardSwiped(isRightSwipe: Boolean, card: Card) {
        if (isRightSwipe) {
            // Handle like action
            likedUserIds.add(card.userId)
            storeSwipeAction(card.userId, "like")
        } else {
            // Handle dislike action
            dislikedUserIds.add(card.userId)
            storeSwipeAction(card.userId, "dislike")
        }

        // Remove the swiped card from the list and notify the adapter
        val position = cardList.indexOf(card)
        if (position >= 0) {
            cardList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun storeSwipeAction(swipedUserId: String, action: String) {
        val swipeData = mapOf(
            "currentUserId" to currentUserId,
            "swipedUserId" to swipedUserId,
            "action" to action,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("Swipes")
            .add(swipeData)
            .addOnSuccessListener {
                Log.d("CardStackAdapter", "Swipe action ($action) stored successfully")

                // Check for mutual like
                if (action == "like") {
                    checkForMatch(swipedUserId)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CardStackAdapter", "Error storing swipe action: ${exception.message}")
            }
    }

    // New method to check for mutual matches
    private fun checkForMatch(swipedUserId: String) {
        db.collection("Swipes")
            .whereEqualTo("currentUserId", swipedUserId)
            .whereEqualTo("swipedUserId", currentUserId)
            .whereEqualTo("action", "like")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Log.d("CardStackAdapter", "It's a match!")
                    Toast.makeText(cardStackView.context, "It's a match!", Toast.LENGTH_SHORT).show()
                    // Handle the match event here (e.g., notify both users)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CardStackAdapter", "Error checking for match: ${exception.message}")
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = cardList.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cardList[position]
        holder.bind(card)
    }
}
