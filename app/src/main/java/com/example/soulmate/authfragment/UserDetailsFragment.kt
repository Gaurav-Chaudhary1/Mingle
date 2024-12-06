package com.example.soulmate.authfragment

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.soulmate.R
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.databinding.FragmentUserDetailsBinding
import com.example.soulmate.utils.ProgressDialogUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UserDetailsFragment : Fragment() {

    private var _binding: FragmentUserDetailsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var profileImageUri: Uri? = null

    // Progress Dialog Utility
    private lateinit var progressDialogUtil: ProgressDialogUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailsBinding.inflate(inflater, container, false)
        setupToolbar()

        // Initialize ProgressDialogUtil
        progressDialogUtil = ProgressDialogUtil()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user details if needed
        loadUserDetails()

        // Set profile image click listener
        binding.profileImage.setOnClickListener {
            openGalleryForImage()
        }

        // Date of Birth click listener
        binding.tvDate.setOnClickListener {
            showDatePicker()
        }

        // Submit button listener
        binding.submitUserDetailsBtn.setOnClickListener {
            submitUserDetails()
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        toolbar.title = "My Profile Details"
        toolbar.inflateMenu(R.menu.user_details_menu)
        toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_userDetailsFragment_to_signInFragment)
        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.skip_button -> {
                    startActivity(Intent(context, UserMainActivity::class.java))
                    requireActivity().finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserDetails() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            firestore.collection("UserDetails").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Fill all the details
                        binding.tvUsername.setText(document.getString("username"))
                        binding.tvFullName.setText(document.getString("fullName"))
                        binding.tvDate.setText(document.getString("dateOfBirth"))
                        binding.tvAddress.setText(document.getString("address"))

                        // Handle gender
                        val gender = document.getString("gender")
                        when (gender) {
                            "Male" -> binding.radioMale.isChecked = true
                            "Female" -> binding.radioFemale.isChecked = true
                            "Other" -> binding.radioOther.isChecked = true
                        }

                        // Load profile image from Firebase Storage
                        loadProfileImageFromFirebase()
                    } else {
                        Toast.makeText(context, "No user details found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching user details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProfileImageFromFirebase() {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userUID.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.user_profile_svgrepo_com)
                    .into(binding.profileImage)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            profileImageUri = data?.data
            binding.profileImage.setImageURI(profileImageUri)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val dateString = "$selectedDay/${selectedMonth + 1}/$selectedYear"

            // Check if the selected date makes the user at least 18 years old
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

            val age = calendar.get(Calendar.YEAR) - selectedCalendar.get(Calendar.YEAR)
            if (age > 18 || (age == 18 && calendar.get(Calendar.DAY_OF_YEAR) >= selectedCalendar.get(Calendar.DAY_OF_YEAR))) {
                binding.tvDate.setText(dateString)
            } else {
                Toast.makeText(context, "You must be at least 18 years old.", Toast.LENGTH_SHORT).show()
            }
        }, year, month, day)

        datePickerDialog.show()
    }


    private fun uploadProfileImage(userId: String, callback: (String) -> Unit) {
        profileImageUri?.let { uri ->
            val storageRef = storage.reference.child("profileImages/$userId.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { imageUri ->
                        callback(imageUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    callback("") // Return empty URL if failed
                }
        } ?: run {
            callback("") // No image selected, return empty URL
        }
    }

    private fun submitUserDetails() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val username = binding.tvUsername.text.toString().trim()
            val fullName = binding.tvFullName.text.toString().trim()
            val dateOfBirth = binding.tvDate.text.toString().trim()
            val address = binding.tvAddress.text.toString().trim()

            val selectedGenderId = binding.genderRadioGroup.checkedRadioButtonId
            val gender = if (selectedGenderId != -1) {
                val selectedRadioButton = view?.findViewById<RadioButton>(selectedGenderId)
                selectedRadioButton?.text.toString()
            } else {
                Toast.makeText(context, "Please select a gender", Toast.LENGTH_SHORT).show()
                return
            }

            if (!isUsernameValid(username)) {
                Toast.makeText(context, "Username must start with '@' and contain at least one period.", Toast.LENGTH_SHORT).show()
                return
            }

            // Show progress dialog
            progressDialogUtil.showProgressDialog(requireContext(), "Saving details...")

            // Check if a new profile image is selected and upload if necessary
            if (profileImageUri != null) {
                uploadProfileImage(userId) { imageUrl ->
                    updateUserDetails(userId, username, fullName, dateOfBirth, address, gender, imageUrl)
                }
            } else {
                // If no new image, retrieve existing profile image
                firestore.collection("UserDetails").document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val existingImageUrl = document.getString("profileImage") ?: ""
                        updateUserDetails(userId, username, fullName, dateOfBirth, address, gender, existingImageUrl)
                    }
                    .addOnFailureListener { e ->
                        progressDialogUtil.hideProgressDialog()
                        Toast.makeText(context, "Error retrieving existing details: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateUserDetails(
        userId: String,
        username: String,
        fullName: String,
        dateOfBirth: String,
        address: String,
        gender: String,
        imageUrl: String
    ) {
        val userDetails = hashMapOf(
            "username" to username,
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "address" to address,
            "gender" to gender,
            "profileImage" to imageUrl,
            "bio" to "", // Initialize bio
            "followersCount" to 0, // Initialize followers count
            "followingCount" to 0, // Initialize following count
            "postsCount" to 0, // Initialize posts count
            "shortsCount" to 0, // Initialize shorts count
            "mostLiked" to false, // Initialize most liked
            "onlineStatus" to "offline" // Initialize online status
        )

        firestore.collection("UserDetails").document(userId)
            .set(userDetails)
            .addOnSuccessListener {
                // Initialize an empty Interests subcollection
                initializeInterestsSubcollection(userId)

                progressDialogUtil.hideProgressDialog()
                Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(context, UserMainActivity::class.java))
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                progressDialogUtil.hideProgressDialog()
                Toast.makeText(context, "Error saving user details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeInterestsSubcollection(userId: String) {
        val interestsRef = firestore.collection("UserDetails").document(userId).collection("Interests")

        // Optionally, you can check if the subcollection exists or create a placeholder
        // This function will simply create the subcollection by referencing it
        interestsRef.get()
            .addOnSuccessListener { querySnapshot ->
                // If the subcollection is empty or doesn't exist, you can log it
                if (querySnapshot.isEmpty) {
                    Log.d("UserDetailsFragment", "Interests subcollection is ready for use.")
                } else {
                    Log.d("UserDetailsFragment", "Interests subcollection already exists.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserDetailsFragment", "Error checking interests subcollection: ${e.message}")
            }
    }


    private fun isUsernameValid(username: String): Boolean {
        return username.startsWith("@") && username.contains(".")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("online")
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus("offline")
    }

    private fun updateUserStatus(status: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            firestore.collection("UserDetails").document(it)
                .update("onlineStatus", status)
                .addOnFailureListener { e ->
                    Log.e("UserDetailsFragment", "Error updating online status: ${e.message}")
                }
        }
    }

    companion object {
        private const val IMAGE_PICK_REQUEST = 1
    }
}
