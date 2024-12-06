package com.example.soulmate.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.soulmate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BuyMembershipActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var welcomeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_membership)

        toolbar = findViewById(R.id.premium_toolbar)
        welcomeText = findViewById(R.id.welcome_text)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        showWelcomeText()
    }

    private fun showWelcomeText() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val userRef =
            FirebaseFirestore.getInstance().collection("UserDetails").document(currentUserId!!)

        userRef.get().addOnSuccessListener { document ->
            val fullName = document.getString("fullName") ?: "User"
            val firstName = fullName.split(" ")[0]
            welcomeText.text = "Hello, $firstName!"
        }.addOnFailureListener {
            welcomeText.text = "Hello, User!"
        }
    }


}