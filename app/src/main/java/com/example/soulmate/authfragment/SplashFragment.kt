package com.example.soulmate.authfragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.soulmate.R
import com.example.soulmate.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)  // Simulate splash delay
            if (isAdded) {
                Log.d("SplashFragment", "Attempting to navigate to SignInFragment")
                findNavController().navigate(R.id.action_splashFragment_to_signInFragment)
            } else {
                Log.e("SplashFragment", "Fragment is not added, skipping navigation.")
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clear binding to avoid memory leaks
    }
}
