package com.example.soulmate.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.soulmate.R

class AccountStatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_status)

        val statusIcon: ImageView = findViewById(R.id.iv_account_status_icon)
        val statusMessage: TextView = findViewById(R.id.tv_account_status_message)
        val additionalMessage: TextView = findViewById(R.id.tv_additional_message)
        val backButton: Button = findViewById(R.id.btn_go_back)

        // Mockup logic to check user compliance
        val isFollowingGuidelines = checkUserCompliance()

        if (isFollowingGuidelines) {
            statusIcon.setImageResource(R.drawable.bc_guidelines)  // Use appropriate drawable
            statusMessage.text = "You are following all our community guidelines!"
            statusMessage.setTextColor(getColor(R.color.green)) // Use your color resource
            additionalMessage.text = "Thank you for being a responsible member of our community."
        } else {
            statusIcon.setImageResource(R.drawable.bc_warning)  // Use appropriate warning icon
            statusMessage.text = "You need to review our community guidelines."
            statusMessage.setTextColor(getColor(R.color.red)) // Use your color resource
            additionalMessage.text = "Please ensure your behavior aligns with our community rules."
        }

        // Handle back button click
        backButton.setOnClickListener {
            finish()  // Close the activity
        }
    }

    // Simulate a compliance check (replace with actual logic)
    private fun checkUserCompliance(): Boolean {
        // Example logic; replace this with real checks
        return true
    }
}
