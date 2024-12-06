package com.example.soulmate.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.soulmate.R
import com.example.soulmate.databinding.ActivityAuthMainBinding
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.auth.FirebaseAuth

class AuthMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user has completed onboarding
        val prefs = getSharedPreferences("OnboardingPrefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboardingComplete", false)

        if (currentUser != null && onboardingComplete) {
            // Redirect to UserMainActivity and clear back stack
            val intent = Intent(this, UserMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return  // Early exit to prevent unnecessary UI setup
        }

        binding = ActivityAuthMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
    }
}
