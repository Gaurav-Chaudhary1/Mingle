package com.example.soulmate.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.databinding.ActivityEditProfileBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditProfileBinding.inflate(layoutInflater) }
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var selectedImageUri: Uri? = null
    private val selectedInterests = mutableListOf<String>() // List to store selected interests
    private val maxInterests = 5 // Maximum number of interests

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Load user details to display
        loadUserDetails()

        // Set up the toolbar's back button
        binding.edtToolbar.setNavigationOnClickListener { onBackPressed() }

        // Set Date Picker for DOB
        binding.edtDate.setOnClickListener { openDatePicker() }

        // Set Update button click listener
        binding.edtBtn.setOnClickListener { updateUserDetails() }

        // Set Profile Image click listener
        binding.edtProfile.setOnClickListener { openImagePicker() }

        // Set up interest selection logic
        setupInterestSelection()
    }

    private fun setupInterestSelection() {
        loadInterestsFromFirestore()

        // Predefined chips click listener
        binding.chipGroupPredefinedInterests.setOnCheckedStateChangeListener { group, checkedIds ->
            checkedIds.forEach { id ->
                val chip = group.findViewById<Chip>(id)
                chip?.let {
                    val interestText = it.text.toString()

                    // Add only if not already added and within the limit
                    if (!selectedInterests.contains(interestText) && selectedInterests.size < maxInterests) {
                        selectedInterests.add(interestText)
                        addChipToSelectedGroup(interestText)
                        Log.d("EditProfileActivity", "Added predefined interest: $interestText")
                    } else if (selectedInterests.size >= maxInterests) {
                        Toast.makeText(this, "You can only select $maxInterests interests.", Toast.LENGTH_SHORT).show()
                        it.isChecked = false // Uncheck the chip
                    } else {
                        // Interest already added, uncheck the chip
                        it.isChecked = false
                        Toast.makeText(this, "Interest already added.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Save interests to Firestore after selection
            saveInterestsToFirestore()
        }

        // Handle chip removal from selected interests
        binding.chipGroupSelectedInterests.setOnCheckedStateChangeListener { group, checkedIds ->
            val removedInterests = selectedInterests.toMutableList()

            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as Chip
                if (!chip.isChecked && selectedInterests.contains(chip.text.toString())) {
                    removedInterests.remove(chip.text.toString())
                    removeInterestFromFirestore(chip.text.toString()) // Remove from Firestore
                    binding.chipGroupSelectedInterests.removeView(chip) // Remove from UI
                    Log.d("EditProfileActivity", "Removed interest: ${chip.text} from UI and Firestore")
                }
            }

            // Update the selectedInterests list to reflect current state
            selectedInterests.clear()
            selectedInterests.addAll(removedInterests)

            // Save the current state of selected interests to Firestore
            saveInterestsToFirestore()
        }

        // Handle manual input for new interests
        binding.edtInterestInput.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val interest = textView.text.toString().trim()
                if (interest.isNotEmpty()) {
                    if (!selectedInterests.contains(interest) && selectedInterests.size < maxInterests) {
                        selectedInterests.add(interest)
                        addChipToSelectedGroup(interest)
                        textView.text = "" // Clear input
                        Log.d("EditProfileActivity", "Custom interest added: $interest")
                    } else if (selectedInterests.size >= maxInterests) {
                        Toast.makeText(this, "You can only have $maxInterests interests.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Interest already added.", Toast.LENGTH_SHORT).show()
                    }
                }
                // Save interests to Firestore
                saveInterestsToFirestore()
                true
            } else {
                false
            }
        }
    }

    private fun removeInterestFromFirestore(interest: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val userRef = firestore.collection("UserDetails").document(userId).collection("Interests")
            userRef.whereEqualTo("interest", interest).get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        userRef.document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d("EditProfileActivity", "Removed interest: $interest from Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditProfileActivity", "Error removing interest: $e")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EditProfileActivity", "Error fetching interest for removal: $e")
                }
        }
    }

    private fun addChipToSelectedGroup(interest: String) {
        val chip = Chip(this).apply {
            text = interest
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedInterests.remove(interest)
                removeInterestFromFirestore(interest) // Remove from Firestore
                binding.chipGroupSelectedInterests.removeView(this)
            }
        }
        binding.chipGroupSelectedInterests.addView(chip)
    }

    private fun loadUserDetails() {
        firestore.collection("UserDetails").document(userUID).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Set fields with Firestore data
                    binding.edtUsername.setText(document.getString("username"))
                    binding.edtFullName.setText(document.getString("fullName"))
                    binding.edtBio.setText(document.getString("bio"))
                    binding.edtDate.setText(document.getString("dateOfBirth"))
                    binding.edtAddress.setText(document.getString("address"))

                    // Set gender radio button
                    val gender = document.getString("gender")
                    when (gender) {
                        "Male" -> binding.genderRadioGroup.check(R.id.radio_male)
                        "Female" -> binding.genderRadioGroup.check(R.id.radio_female)
                        "Other" -> binding.genderRadioGroup.check(R.id.radio_other)
                    }

                    // Load profile image from Firebase Storage
                    val profileImageRef = storage.reference.child("profileImages/$userUID.jpg")
                    profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(this).load(uri)
                            .placeholder(R.drawable.user_pro)
                            .into(binding.edtProfile)
                    }
                } else {
                    Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadInterestsFromFirestore() {
        val interestsRef = firestore.collection("UserDetails").document(userUID).collection("Interests")
        interestsRef.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                val interest = document.getString("interest")
                interest?.let {
                    if (!selectedInterests.contains(it)) { // Check for duplicates
                        selectedInterests.add(it) // Add to selected interests
                        addChipToSelectedGroup(it) // Add to chip group for display
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("EditProfileActivity", "Error fetching interests: ${e.message}")
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)

            if (isUserAtLeast18(selectedDate)) {
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.edtDate.setText(date)
            } else {
                Toast.makeText(this, "You must be at least 18 years old.", Toast.LENGTH_SHORT).show()
            }
        }, year, month, day)

        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun isUserAtLeast18(birthDate: Calendar): Boolean {
        val today = Calendar.getInstance()
        val age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))
        ) {
            return age - 1 >= 18
        }
        return age >= 18
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this).load(uri).into(binding.edtProfile)
            }
        }
    }

    private fun updateUserDetails() {
        val username = binding.edtUsername.text.toString()
        val fullName = binding.edtFullName.text.toString()
        val address = binding.edtAddress.text.toString()
        val bio = binding.edtBio.text.toString()
        val dob = binding.edtDate.text.toString()
        val gender = findViewById<RadioButton>(binding.genderRadioGroup.checkedRadioButtonId)?.text.toString()

        if (username.isNotEmpty() && fullName.isNotEmpty() && address.isNotEmpty()) {
            // Fetch the existing user details to preserve non-updated fields
            firestore.collection("UserDetails").document(userUID).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val currentFollowersCount = document.getLong("followersCount") ?: 0
                        val currentFollowingCount = document.getLong("followingCount") ?: 0
                        val currentPostsCount = document.getLong("postsCount") ?: 0
                        val currentShortsCount = document.getLong("shortsCount") ?: 0
                        val currentMostLiked = document.getBoolean("mostLiked") ?: false

                        // Create an updated map including existing fields
                        val userDetails = hashMapOf(
                            "username" to username,
                            "fullName" to fullName,
                            "address" to address,
                            "bio" to bio,
                            "dateOfBirth" to dob,
                            "gender" to gender,
                            "profileImage" to document.getString("profileImage"), // Retain the existing profile image
                            "followersCount" to currentFollowersCount,
                            "followingCount" to currentFollowingCount,
                            "postsCount" to currentPostsCount,
                            "shortsCount" to currentShortsCount,
                            "mostLiked" to currentMostLiked,
                            "onlineStatus" to "offline" // Default to "offline"
                        )

                        // Update the user details in Firestore
                        firestore.collection("UserDetails").document(userUID).set(userDetails)
                            .addOnSuccessListener {
                                // Update profile image if selected
                                selectedImageUri?.let { uri ->
                                    uploadProfileImage(uri)
                                } ?: run {
                                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    finish() // Go back after update
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching existing user details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveInterestsToFirestore() {
        val interestsRef = firestore.collection("UserDetails").document(userUID).collection("Interests")

        // Clear existing interests before saving
        interestsRef.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                interestsRef.document(document.id).delete()
            }

            // Add new interests to Firestore
            for (interest in selectedInterests) {
                val interestData = hashMapOf("interest" to interest)
                interestsRef.add(interestData).addOnSuccessListener {
                    Log.d("EditProfileActivity", "Saved interest: $interest to Firestore")
                }.addOnFailureListener { e ->
                    Log.e("EditProfileActivity", "Error saving interest: $e")
                }
            }
        }.addOnFailureListener { e ->
            Log.e("EditProfileActivity", "Error fetching interests for saving: $e")
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val profileImageRef = storage.reference.child("profileImages/$userUID.jpg")
        profileImageRef.putFile(imageUri)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back after update
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
