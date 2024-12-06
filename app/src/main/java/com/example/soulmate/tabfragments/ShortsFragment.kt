package com.example.soulmate.tabfragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.soulmate.activities.UserAllReelActivity
import com.example.soulmate.adapters.ShortsAdapter
import com.example.soulmate.databinding.FragmentShortsBinding
import com.example.soulmate.models.Reel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ShortsFragment(private val userUID: String) : Fragment(), ShortsAdapter.OnReelClickListener {

    private lateinit var binding: FragmentShortsBinding
    private lateinit var shortsAdapter: ShortsAdapter
    private val shorts = mutableListOf<Reel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShortsBinding.inflate(inflater, container, false)

        // Set up RecyclerView with grid layout
        binding.recyclerViewShorts.apply {
            layoutManager = GridLayoutManager(context, 3) // 3 columns for grid
            shortsAdapter = ShortsAdapter(requireContext(), shorts, this@ShortsFragment) // Pass the listener here
            adapter = shortsAdapter
        }

        // Fetch shorts data from Firebase
        fetchShorts()

        return binding.root
    }

    private fun fetchShorts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("reels")
            .whereEqualTo("userUID", userUID) // Use userUID here
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, exception ->
                // Check if fragment is still attached
                if (!isAdded) {
                    Log.d("ShortsFragment", "Fragment is not attached, skipping snapshot processing")
                    return@addSnapshotListener
                }

                if (exception != null) {
                    Log.e("ShortsFragment", "Error fetching shorts: ${exception.message}")
                    Toast.makeText(requireContext(), "Error fetching shorts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                shorts.clear()
                querySnapshot?.forEach { document ->
                    val shortItem = document.toObject(Reel::class.java)
                    shorts.add(shortItem)
                }

                // Check if fragment is still added before updating the UI
                if (isAdded) {
                    shortsAdapter.notifyDataSetChanged()
                }
            }
    }

    // Handle reel click and navigate to UserAllReelActivity
    override fun onReelClick(reel: Reel) {
        val intent = Intent(requireContext(), UserAllReelActivity::class.java)
        intent.putExtra("userUID", userUID)
        intent.putExtra("reelId", reel.reelId) // Make sure the Reel model has the 'id' field
        startActivity(intent)
    }
}
