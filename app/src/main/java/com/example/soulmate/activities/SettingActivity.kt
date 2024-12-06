package com.example.soulmate.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.adapters.UserOptionsAdapter
import com.example.soulmate.models.OptionItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity() {

    private lateinit var userImage: CircleImageView
    private lateinit var userName: TextView
    private lateinit var userAddress: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userOptionsRecyclerView: RecyclerView
    private lateinit var settingButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var subscription: ImageButton
    private lateinit var currentPhotoPath: String
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Toolbar setup for back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (supportActionBar != null) {
            supportActionBar?.title = "";  // Clear the toolbar title
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize Views
        userImage = findViewById(R.id.user_image2)
        userName = findViewById(R.id.tv_user_name)
        userAddress = findViewById(R.id.tv_user_address)
        settingButton = findViewById(R.id.settings_image)
        cameraButton = findViewById(R.id.capture_image)
        subscription = findViewById(R.id.buy_premium_image)

        if (currentUserId != null) {
            fetchUserImage(currentUserId)
            fetchUserDetails(currentUserId)
        } else {
            Log.e("SettingActivity", "User not logged in")
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
        }

        userOptionsRecyclerView = findViewById(R.id.user_options_recycler_view)

        // Options with titles and icons
        val options = listOf(
            OptionItem("Privacy Policy", R.drawable.bc_privacy),
            OptionItem("Terms & Conditions", R.drawable.bc_terms),
            OptionItem("Cookies Policy", R.drawable.bc_cookie),
            OptionItem("Account Status", R.drawable.baseline_account_circle_24),
            OptionItem("Contact Us", R.drawable.bc_conditions),
            OptionItem("Log Out", R.drawable.bc_logout)
        )

        userOptionsRecyclerView.layoutManager = LinearLayoutManager(this)
        userOptionsRecyclerView.adapter = UserOptionsAdapter(options) { selectedOption ->
            handleOptionClick(selectedOption)
        }

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        settingButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        cameraButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        subscription.setOnClickListener {
            val intent = Intent(this, BuyMembershipActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchUserDetails(currentUser: String) {
        firestore.collection("UserDetails").document(currentUser)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    userName.text = documentSnapshot.getString("fullName") ?: "N/A"
                    userAddress.text = documentSnapshot.getString("address") ?: "N/A"
                } else {
                    Log.e("SettingActivity", "Document does not exist")
                    userName.text = "N/A"
                    userAddress.text = "N/A"
                }
            }
            .addOnFailureListener { e ->
                Log.e("SettingActivity", "Error loading user data: ${e.message}")
                userName.text = "Error"
                userAddress.text = "Error"
            }
    }

    private fun fetchUserImage(currentUser: String) {
        val storageRef =
            FirebaseStorage.getInstance().reference.child("profileImages/$currentUser.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.user_profile_svgrepo_com)
                    .into(userImage)
            }
            .addOnFailureListener { exception ->
                Log.e("SettingActivity", "Failed to load profile image: ${exception.message}")
            }
    }

    // Handle back button click
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun handleOptionClick(option: OptionItem) {
        when (option.title) {
            "Privacy Policy" -> openWebView(
                "Privacy Policy",
                "https://www.termsfeed.com/live/41dde5d3-2921-42d2-83a3-a0fce26e58c5"
            )

            "Terms & Conditions" -> openWebView(
                "Terms & Conditions",
                "https://www.termsfeed.com/live/1cfd228e-7405-4b2a-b0e8-90de77298298"
            )

            "Cookies Policy" -> openWebView(
                "Cookies Policy",
                "https://www.termsfeed.com/live/9a8579d5-c8a7-4ac1-968b-4404f3ef311f"
            )

            "Account Status" -> {
                val intent = Intent(this, AccountStatusActivity::class.java)
                startActivity(intent)
            }

            "Contact Us" -> openEmailApp("morphotailstore@gmail.com")
            "Log Out" -> { showDialog() }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openWebView(title: String, url: String) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("title", title)
            putExtra("url", url)
        }
        startActivity(intent)
    }

    private fun openEmailApp(email: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // MIME type for email
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Contact Us - App Inquiry")
            putExtra(Intent.EXTRA_TEXT, "Hi, I would like to inquire about...")
        }
        try {
            startActivity(Intent.createChooser(intent, "Choose an Email client"))
        } catch (e: Exception) {
            showToast("No email client found")
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Log Out")
        builder.setMessage("Are you sure you want to log out?")
        builder.setCancelable(false)

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
        }

        builder.setPositiveButton("Yes") { _, _ ->
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Show a toast message
            showToast("Logged out successfully")

            // Create an intent to navigate to AuthMainActivity and show SignInFragment
            val intent = Intent(this, AuthMainActivity::class.java)

            // Clear the back stack to prevent returning to the settings screen
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Start AuthMainActivity
            startActivity(intent)
            finish()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageUri = Uri.fromFile(File(currentPhotoPath))
            uploadImageToFirebase(imageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("profileImages/$currentUserId.jpg")

            val uploadTask = storageRef.putFile(imageUri)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    updateProfileImageInFirestore(downloadUri.toString())
                } else {
                    showToast("Image upload failed.")
                }
            }
        }
    }

    private fun updateProfileImageInFirestore(imageUrl: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("UserDetails").document(currentUserId)

            userRef.update("profileImage", imageUrl)
                .addOnSuccessListener {
                    showToast("Profile image updated successfully!")
                }
                .addOnFailureListener {
                    showToast("Failed to update profile image.")
                }
        }
    }

}
