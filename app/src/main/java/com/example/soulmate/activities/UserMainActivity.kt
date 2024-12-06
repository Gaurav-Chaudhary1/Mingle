package com.example.soulmate.activities

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.soulmate.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.core.invite.advanced.ZegoCallInvitationInCallingConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserMainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    private var username: String? = null
    private var fullName: String? = null
    private var dateOfBirth: String? = null
    private var address: String? = null
    private var profileImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"

        setupZegoUIKit(currentUserId, currentUserName)

        // Mark onboarding as complete
        val prefs = getSharedPreferences("OnboardingPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("onboardingComplete", true)
            apply()
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView2) as NavHostFragment
        val navController = navHostFragment.navController

        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        loadUserData()
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(2000)

            username = intent.getStringExtra("username") ?: "N/A"
            fullName = intent.getStringExtra("fullName") ?: "N/A"
            dateOfBirth = intent.getStringExtra("dateOfBirth") ?: "01/01/1990"
            address = intent.getStringExtra("address") ?: "N/A"
            profileImage = intent.getStringExtra("profileImage") ?: ""

            withContext(Dispatchers.Main) {
                // Pass data to fragments or ViewModel as needed
            }
        }
    }

    fun hideBottomNavigation() {
        bottomNavigationView.visibility = View.GONE
    }

    fun showBottomNavigation() {
        bottomNavigationView.visibility = View.VISIBLE
    }

    private fun setupZegoUIKit(userId: String, userName: String){
        val application: Application = application
        val appId: Long = 1976266946
        val appSign: String = "d67394761ba6d9472ac2ca639c9599e1c56133321c9a646b095aa178aa199ebf"
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        ZegoUIKitPrebuiltCallService.init(application, appId, appSign, userId, userName, callInvitationConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallService.unInit()
    }
}
