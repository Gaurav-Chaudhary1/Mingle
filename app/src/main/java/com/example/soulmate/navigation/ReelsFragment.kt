package com.example.soulmate.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.adapters.ReelsAdapter
import com.example.soulmate.databinding.FragmentReelsBinding
import com.example.soulmate.models.Reel
import com.google.firebase.firestore.FirebaseFirestore

class ReelsFragment : Fragment() {

    private var _binding: FragmentReelsBinding? = null
    private val binding get() = _binding!!
    private lateinit var reelsAdapter: ReelsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        fetchReelsData()
        return binding.root
    }

    private fun setupRecyclerView() {
        reelsAdapter = ReelsAdapter(requireContext(), firestore)
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.reelsRecyclerView.apply {
            adapter = reelsAdapter
            layoutManager = this@ReelsFragment.layoutManager
            LinearSnapHelper().attachToRecyclerView(this)
        }

        binding.reelsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = getCenterView(layoutManager)
                    centerView?.let { view ->
                        val position = recyclerView.getChildAdapterPosition(view)
                        reelsAdapter.setCurrentlyPlayingPosition(position)
                    }
                }
            }
        })
    }

    private fun getCenterView(layoutManager: LinearLayoutManager): View? {
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        val recyclerCenterX = binding.reelsRecyclerView.width / 2
        var closestChild: View? = null
        var minDistance = Int.MAX_VALUE

        for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
            val child = layoutManager.findViewByPosition(i) ?: continue
            val childCenterX = child.left + child.width / 2
            val distance = Math.abs(childCenterX - recyclerCenterX)

            if (distance < minDistance) {
                minDistance = distance
                closestChild = child
            }
        }
        return closestChild
    }

    private fun fetchReelsData() {
        binding.loadingProgress.visibility = View.VISIBLE

        firestore.collection("reels")
            .get()
            .addOnSuccessListener { documents ->
                binding.loadingProgress.visibility = View.GONE
                if (documents.isEmpty) {
                    Log.d("ReelsFragment", "No reels found.")
                    return@addOnSuccessListener
                }
                val reelsList = documents.mapNotNull { it.toObject(Reel::class.java) }
                reelsAdapter.submitList(reelsList)
                Log.d("ReelsFragment", "Fetched ${reelsList.size} reels")

                // Start playback for the first reel
                if (reelsList.isNotEmpty()) {
                    reelsAdapter.setCurrentlyPlayingPosition(0)
                }
            }
            .addOnFailureListener { exception ->
                binding.loadingProgress.visibility = View.GONE
                Log.e("ReelsFragment", "Error fetching reels: ", exception)
                Toast.makeText(requireContext(), "Failed to load reels.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reelsAdapter.releasePlayers(binding.reelsRecyclerView)
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (activity as? UserMainActivity)?.hideBottomNavigation() // hide bottom navigation
    }

    override fun onPause() {
        super.onPause()
        reelsAdapter.releasePlayers(binding.reelsRecyclerView)
    }
}
