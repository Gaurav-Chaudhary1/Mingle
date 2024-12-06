package com.example.soulmate.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.soulmate.R
import com.example.soulmate.adapters.UserAllReelAdapter
import com.example.soulmate.models.Reel
import com.google.firebase.firestore.FirebaseFirestore

class UserAllReelActivity : AppCompatActivity() {

    private lateinit var reelsRecyclerView: RecyclerView
    private lateinit var userAllReelAdapter: UserAllReelAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val reelList = mutableListOf<Reel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_all_reel)

        reelsRecyclerView = findViewById(R.id.reelsRecyclerView2)
        userAllReelAdapter = UserAllReelAdapter(this, reelList)

        val userUID = intent.getStringExtra("userUID")
        val reelId = intent.getStringExtra("reelId")

        setupRecyclerView()
        fetchUserReels(userUID, reelId)
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        reelsRecyclerView.layoutManager = layoutManager
        reelsRecyclerView.adapter = userAllReelAdapter
    }

    private fun fetchUserReels(userUID: String?, reelId: String?) {
        if (userUID == null || reelId == null) {
            Toast.makeText(this, "Invalid reel data", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("reels")
            .whereEqualTo("userUID", userUID)
            .get()
            .addOnSuccessListener { documents ->
                val reelsList = documents.mapNotNull { it.toObject(Reel::class.java) }
                val firstReel = reelsList.find { it.reelId == reelId }

                if (firstReel != null) {
                    val firstReelIndex = reelsList.indexOf(firstReel)
                    val orderedReels = mutableListOf(firstReel)  // Add clicked reel at the start
                    orderedReels.addAll(reelsList.filter { it.reelId != reelId })  // Add the rest
                    userAllReelAdapter.submitList(orderedReels)
                    userAllReelAdapter.setCurrentlyPlayingPosition(firstReelIndex)  // Set position for video play
                } else {
                    userAllReelAdapter.submitList(reelsList)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserAllReelActivity", "Error fetching reels: ", exception)
                Toast.makeText(this, "Failed to load reels.", Toast.LENGTH_SHORT).show()
            }
    }
}
