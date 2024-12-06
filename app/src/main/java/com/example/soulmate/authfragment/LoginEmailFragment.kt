package com.example.soulmate.authfragment

import DialogEmailVerification
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.soulmate.R
import com.example.soulmate.databinding.FragmentLoginEmailBinding
import com.example.soulmate.utils.ProgressDialogUtil // Import ProgressDialogUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginEmailFragment : Fragment() {

    private lateinit var binding: FragmentLoginEmailBinding
    private lateinit var auth: FirebaseAuth // Firebase Authentication instance
    private lateinit var firestore: FirebaseFirestore // Firestore instance
    private lateinit var progressDialogUtil: ProgressDialogUtil // Progress dialog utility

    // Constants for messages
    private companion object {
        const val EMPTY_FIELDS_MESSAGE = "Please enter email and password"
        const val LOGIN_SUCCESS_MESSAGE = "Login successful"
        const val EMAIL_NOT_VERIFIED_MESSAGE = "Please verify your email first"
        const val NEW_USER_MESSAGE = "New user? Please sign up first"
        const val PASSWORD_RESET_MESSAGE = "Password reset email sent. Check your email."
        const val RESET_EMAIL_FAILED_MESSAGE = "Failed to send reset email: "
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth instance
        firestore = FirebaseFirestore.getInstance() // Initialize Firestore instance
        progressDialogUtil = ProgressDialogUtil() // Initialize progress dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginEmailBinding.inflate(inflater, container, false)

        setupListeners()
        return binding.root
    }

    private fun setupListeners() {
        // Handle login button click
        binding.loginBtn.setOnClickListener {
            val email = binding.loginWithEmail.text.toString().trim()
            val password = binding.loginWithPassword.text.toString().trim()

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), EMPTY_FIELDS_MESSAGE, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show the progress dialog while logging in
            progressDialogUtil.showProgressDialog(requireContext(),"Logging in...")

            // Sign in user using Firebase Authentication
            loginUser(email, password)
        }

        // Handle forget password text click
        binding.tvForgetPassword.setOnClickListener {
            val email = binding.loginWithEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                resetPassword(email)
            } else {
                Toast.makeText(requireContext(), "Please enter your registered email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Dismiss progress dialog when the task completes
                progressDialogUtil.hideProgressDialog()

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        if (user.isEmailVerified) {
                            // User is authenticated and email is verified
                            saveUserDataToFirestore(user.uid, email)
                            Toast.makeText(requireContext(), LOGIN_SUCCESS_MESSAGE, Toast.LENGTH_SHORT).show()
                            navigateToUserApp()
                        } else {
                            // Email is not verified, show toast
                            Toast.makeText(requireContext(), EMAIL_NOT_VERIFIED_MESSAGE, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun handleLoginError(exception: Exception?) {
        progressDialogUtil.hideProgressDialog() // Hide the dialog in case of an error
        val exceptionMessage = exception?.message
        if (exceptionMessage?.contains("There is no user record") == true) {
            Toast.makeText(requireContext(), NEW_USER_MESSAGE, Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_loginEmailFragment_to_signUpFragment)
        } else {
            Toast.makeText(requireContext(), "Login failed: $exceptionMessage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserDataToFirestore(userId: String, email: String) {
        val userData = hashMapOf(
            "userId" to userId,
            "email" to email,
            // Add other user data fields as necessary
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                // Data saved successfully
                Toast.makeText(requireContext(), "User data saved to Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Failed to save data
                Toast.makeText(requireContext(), "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Navigate to Main App if login is successful and email is verified
    private fun navigateToUserApp() {
        val bundle = Bundle()
        bundle.putString("email", binding.loginWithEmail.text.toString().trim())
        findNavController().navigate(R.id.action_loginEmailFragment_to_userDetailsFragment, bundle)
    }

    // Reset password if user forgot the password
    private fun resetPassword(email: String) {
        progressDialogUtil.showProgressDialog(requireContext(),"Sending reset email...")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressDialogUtil.hideProgressDialog()

                if (task.isSuccessful) {
                    showPasswordResetDialog()
                } else {
                    Toast.makeText(requireContext(), "$RESET_EMAIL_FAILED_MESSAGE${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showPasswordResetDialog() {
        val dialog = DialogEmailVerification(requireContext())
        dialog.setMessage(PASSWORD_RESET_MESSAGE)
        dialog.show()

        // Optional: Close the dialog when the OK button is clicked
        val okButton: Button = dialog.findViewById(R.id.btn_close)
        okButton.setOnClickListener {
            okButton.visibility = View.VISIBLE
            dialog.dismiss()  // Dismiss dialog when OK is clicked
        }
    }
}
