package com.example.soulmate.authfragment

import DialogEmailVerification
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.soulmate.R
import com.example.soulmate.databinding.FragmentSignInBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSignInBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In Options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Use your web client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Facebook Callback Manager
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(layoutInflater)

        binding.googleLoginBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.facebookLoginBtn.setOnClickListener {
            signInWithFacebook()
        }

        binding.logInNumberBtn.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_loginPhoneFragment)
        }

        binding.logInEmailBtn.setOnClickListener {
            checkEmailVerificationAndNavigate()
        }

        binding.signUpTxt.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        return binding.root
    }

    // Check if email is verified and navigate to email login
    private fun checkEmailVerificationAndNavigate() {
        val user = mAuth.currentUser
        if (user != null) {
            // Check if email is verified
            if (user.isEmailVerified) {
                findNavController().navigate(R.id.action_signInFragment_to_loginEmailFragment)
            } else {
                showEmailVerificationDialog()
            }
        } else {
            // Handle case when user is null (not signed in)
            Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show()
        }
    }

    // Show email verification dialog
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

    // Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle Google Sign-In result
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("SignInFragment", "Google sign-in failed: ${e.message}")
            }
        }

        // Handle Facebook Sign-In result
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_signInFragment_to_userDetailsFragment)
                } else {
                    Toast.makeText(requireContext(), "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Facebook Sign-In
    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(requireContext(), "Facebook sign-in canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e("SignInFragment", "Facebook sign-in failed: ${error.message}")
                Toast.makeText(requireContext(), "Facebook sign-in failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Facebook Sign-In Successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_signInFragment_to_userDetailsFragment)
                } else {
                    Toast.makeText(requireContext(), "Facebook Sign-In Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
