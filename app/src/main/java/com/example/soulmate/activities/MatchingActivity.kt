package com.example.soulmate.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.soulmate.adapters.CardStackAdapter
import com.example.soulmate.databinding.ActivityMatchingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MatchingActivity : AppCompatActivity(), CardStackListener {

    private val binding by lazy {
        ActivityMatchingBinding.inflate(layoutInflater)
    }
    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: CardStackAdapter

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Fetch the current user UID
        val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch the current user's gender from Firestore
        lifecycleScope.launch {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("UserDetails")
                    .document(userUID)
                    .get()
                    .await()

                val currentUserGender = document.getString("gender")

                if (currentUserGender != null) {
                    runOnUiThread {
                        initializeCardStackView(currentUserGender)
                    }
                } else {
                    Log.e("MatchingActivity", "Gender field is missing in the document")
                }
            } catch (e: Exception) {
                Log.e("MatchingActivity", "Error fetching user gender", e)
            }
        }
    }

    private fun initializeCardStackView(currentUserGender: String) {
        manager = CardStackLayoutManager(this, this).apply {
            setSwipeThreshold(0.2f) // Lower the threshold slightly for quicker swipe response
            setDirections(Direction.HORIZONTAL) // Keep horizontal swipes
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setStackFrom(StackFrom.None)
            setVisibleCount(3)
            setTranslationInterval(12.0f) // Slightly increase distance between cards for more clarity
            setScaleInterval(0.90f) // Smaller cards at the back to make the next card more visible
            setSwipeAnimationSetting(
                SwipeAnimationSetting.Builder()
                    .setDuration(Duration.Slow.duration) // Set slow to medium animation speed
                    .build()
            )
        }

        adapter = CardStackAdapter(currentUserGender, binding.cardStackView)

        binding.cardStackView.layoutManager = manager
        binding.cardStackView.adapter = adapter

        adapter.fetchOppositeGenderProfiles()
    }

    // Override the necessary functions to handle card swipes
    override fun onCardSwiped(direction: Direction?) {
        val topPosition = manager.topPosition - 1

        if (topPosition < adapter.itemCount) {
            val swipedCard = adapter.getCardAt(topPosition)

            when (direction) {
                Direction.Right -> adapter.onCardSwiped(isRightSwipe = true, card = swipedCard)
                Direction.Left -> adapter.onCardSwiped(isRightSwipe = false, card = swipedCard)
                else -> {}
            }
        } else {
            // Refresh the list when all cards are swiped
            Toast.makeText(this, "Fetching more profiles...", Toast.LENGTH_SHORT).show()
            adapter.fetchMoreUsers(10) // Fetch more users on refresh
        }
    }


    // Required but unused overrides for CardStackListener
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}

    override fun onCardAppeared(view: android.view.View?, position: Int) {
        Log.d("CardStack", "Card appeared at position: $position")
    }

    override fun onCardDisappeared(view: android.view.View?, position: Int) {
        Log.d("CardStack", "Card disappeared at position: $position")
    }
}
