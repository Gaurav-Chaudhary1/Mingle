package com.example.soulmate.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.soulmate.R
import com.example.soulmate.activities.PostActivity
import com.example.soulmate.activities.ReelsActivity
import com.example.soulmate.databinding.FragmentAddBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFragment : BottomSheetDialogFragment() {

    private lateinit var binding : FragmentAddBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBinding.inflate(inflater, container, false)

        binding.tvAdd.setOnClickListener {
            startActivity(Intent(requireContext(), PostActivity::class.java))
            requireActivity().finish()
        }

        binding.tvAddVideo.setOnClickListener {
            startActivity(Intent(requireContext(), ReelsActivity::class.java))
            requireActivity().finish()
        }
        return binding.root
    }
}