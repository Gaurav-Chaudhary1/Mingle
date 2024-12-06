package com.example.soulmate.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.soulmate.R
import com.example.soulmate.activities.NextUserProfile
import com.example.soulmate.databinding.FragmentFilteredProfilesBinding
import com.example.soulmate.adapters.UserGridAdapter
import com.example.soulmate.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FilteredProfilesFragment : Fragment() {

    private lateinit var binding: FragmentFilteredProfilesBinding
    private lateinit var adapter: UserGridAdapter
    private val userList = mutableListOf<UserProfile>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilteredProfilesBinding.inflate(inflater, container, false)

        binding.filterProfileToolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_filteredProfilesFragment_to_searchFragment)
        }

        val selectedInterest = arguments?.getString("interest") ?: ""
        binding.interestTextView.text = selectedInterest

        setupRecyclerView()
        fetchFilteredProfiles(selectedInterest)

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = UserGridAdapter(userList) { userId ->
            navigateToUserProfile(userId) // Handle navigation on click
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter
    }

    private fun fetchFilteredProfiles(interest: String) {
        db.collection("UserDetails")
            .limit(50) // Limit query to 50 users
            .get()
            .addOnSuccessListener { snapshot ->
                filterUsersByInterest(snapshot, interest)
            }
            .addOnFailureListener { e ->
                Log.e("FilteredProfilesFragment", "Error fetching profiles: ${e.message}")
            }
    }

    private fun filterUsersByInterest(snapshot: QuerySnapshot, interest: String) {
        userList.clear()

        for (document in snapshot.documents) {
            val userUID = document.id
            val userInterestsRef = db.collection("UserDetails").document(userUID).collection("Interests")
            userInterestsRef.get().addOnSuccessListener { interestsSnapshot ->
                for (interestDoc in interestsSnapshot) {
                    if (interestDoc.getString("interest") == interest) {
                        val userProfile = UserProfile(
                            uid = userUID,
                            fullName = document.getString("fullName") ?: "",
                            profileImage = document.getString("profileImage") ?: "",
                            address = document.getString("address") ?: "",
                            followersCount = document.getLong("followersCount")?.toInt() ?: 0,
                            mostLiked = document.getBoolean("mostLiked") ?: false
                        )
                        userList.add(userProfile)
                        break // Exit once match is found
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun navigateToUserProfile(userId: String) {
        val intent = Intent(requireContext(), NextUserProfile::class.java)
        intent.putExtra("otherUserUID", userId)
        startActivity(intent)
    }
}
