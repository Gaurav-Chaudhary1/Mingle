package com.example.soulmate.authfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.soulmate.databinding.FragmentLoginPhoneBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import java.util.concurrent.TimeUnit

class LoginPhoneFragment : Fragment() {
    private lateinit var binding: FragmentLoginPhoneBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginPhoneBinding.inflate(inflater, container, false)

        binding.btnContinue.setOnClickListener {
            val countryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()

            // Combine country code with the phone number
            val fullPhoneNumber = "$countryCode$phoneNumber"

            if (isValidPhoneNumber(fullPhoneNumber)) {
                sendVerificationCode(fullPhoneNumber)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // A more robust validation using regex for phone numbers
        val phonePattern = "^\\+[0-9]{10,14}$" // Ensures phone number starts with '+' and is 10-14 digits
        return phone.matches(phonePattern.toRegex())
    }

    private fun sendVerificationCode(phoneNumber: String) {
        // Show the progress bar while waiting for OTP
        binding.progressBar.visibility = View.VISIBLE

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())    // Activity for callback binding
            .setCallbacks(verificationCallbacks) // OnVerificationStateChangedCallbacks
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Hide the progress bar when verification is completed
            binding.progressBar.visibility = View.GONE

            val otp = credential.smsCode
            if (otp != null) {
                // If OTP is received automatically, send it to the next fragment along with verificationId
                navigateToOtpFragment(verificationId ?: "", otp, binding.etPhoneNumber.text.toString().trim())
            } else {
                Toast.makeText(requireContext(), "Verification completed but OTP not received", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // Hide the progress bar if verification failed
            binding.progressBar.visibility = View.GONE

            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
                is FirebaseTooManyRequestsException -> {
                    Toast.makeText(requireContext(), "Too many requests, try again later", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCodeSent(sentVerificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(sentVerificationId, token)
            // Save verification ID for later use
            verificationId = sentVerificationId
            // Hide the progress bar when the code is sent
            binding.progressBar.visibility = View.GONE
            // Navigate to OtpFragment with verificationId and phoneNumber
            navigateToOtpFragment(verificationId ?: "", null, binding.etPhoneNumber.text.toString().trim())
        }
    }

    private fun navigateToOtpFragment(verificationId: String, otp: String?, phoneNumber: String) {
        // Pass the phone number and verificationId (and OTP if available) to the OtpFragment
        val action = LoginPhoneFragmentDirections
            .actionLoginPhoneFragmentToOtpFragment(verificationId, phoneNumber)
        findNavController().navigate(action)
    }
}
