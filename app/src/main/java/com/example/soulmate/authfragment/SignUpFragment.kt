package com.example.soulmate.authfragment

import DialogEmailVerification
import VerificationEmailDialogFragment
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.soulmate.R
import com.example.soulmate.activities.UserMainActivity
import com.example.soulmate.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val handler = Handler(Looper.getMainLooper()) // To handle delay for debounce

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth instance
        firestore = FirebaseFirestore.getInstance() // Initialize Firestore instance
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Disable sign-up button initially
        binding.signUpBtn.isEnabled = false

        // Enable sign-up button when all fields are filled
        binding.tvEmailAddress.addTextChangedListener(inputWatcher)
        binding.tvPassword.addTextChangedListener(inputWatcher)
        binding.tvConfirmPassword.addTextChangedListener(inputWatcher)

        // Set up sign-up button listener
        binding.signUpBtn.setOnClickListener {
            signUpWithEmailPassword()
        }

        binding.signInTxt.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginEmailFragment)
        }

        binding.resendVerificationEmailBtn.setOnClickListener {
            val user = auth.currentUser
            user?.let { sendVerificationEmail(it) }
        }

        return binding.root
    }

    private val inputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val email = binding.tvEmailAddress.text.toString().trim()
            val password = binding.tvPassword.text.toString().trim()
            val confirmPassword = binding.tvConfirmPassword.text.toString().trim()

            binding.signUpBtn.isEnabled = email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun signUpWithEmailPassword() {
        val email = binding.tvEmailAddress.text.toString().trim() // Get email
        val password = binding.tvPassword.text.toString().trim() // Get password
        val confirmPassword = binding.tvConfirmPassword.text.toString().trim() // Get confirm password

        // Validate input fields
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Please fill all the fields") // Use string resource
            return
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match") // Use string resource
            return
        }

        // Proceed with email and password sign-up
        createUserWithEmail(email, password)
    }

    private fun createUserWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        sendVerificationEmail(it)
                        saveUserDetailsToFirestore(it)
                    }
                } else {
                    showToast("Sign up failed: ${task.exception?.message}")
                }
            }
    }

    // Send a verification email to the user
    private fun sendVerificationEmail(user: FirebaseUser) {
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showVerificationDialog() // Show custom verification dialog
            } else {
                showToast("Failed to send verification email")
            }
        }
    }

    // Save the user details to Firestore
    private fun saveUserDetailsToFirestore(user: FirebaseUser) {
        val userId = user.uid
        val userDetails = hashMapOf(
            "userId" to userId,
            "email" to user.email
        )

        firestore.collection("users").document(userId).set(userDetails)
            .addOnSuccessListener {
                showToast("User details saved")

                // Check email verification before navigating
                userEmailVerification()
            }
            .addOnFailureListener {
                showToast("Failed to save user details")
            }
    }

    // Show custom dialog when email verification is sent
    private fun showVerificationDialog() {
        val dialog = VerificationEmailDialogFragment()
        dialog.show(parentFragmentManager, "verification_dialog")
    }

    override fun onResume() {
        super.onResume()
        // Check user email verification only if user is signed in
        auth.currentUser?.let {
            userEmailVerification()
        }
    }

    // Check if the user has verified their email
    private fun userEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    showToast("Email verified!")
                    // Navigate to UserDetailsFragment
                    navigateToUserDetails()
                } else {
                    // Show email verification dialog if not verified
                    showEmailVerificationDialog()
                }
            }
        }
    }

    private fun navigateToUserDetails() {
        // Pass the email to UserDetailsFragment via Bundle
        val bundle = Bundle().apply {
            putString("email", binding.tvEmailAddress.text.toString().trim())
        }
        findNavController().navigate(R.id.action_signUpFragment_to_userDetailsFragment, bundle)
    }

    private fun showEmailVerificationDialog() {
        val dialog = DialogEmailVerification(requireContext())
        dialog.setMessage("You have not verified your email. Please verify and try again.")
        dialog.show()
        // Optional: Close the dialog when the OK button is clicked
        val okButton: Button = dialog.findViewById(R.id.btn_close)
        okButton.setOnClickListener {
            okButton.visibility = View.VISIBLE
            dialog.dismiss()  // Dismiss dialog when OK is clicked
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
