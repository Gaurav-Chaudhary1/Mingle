package com.example.soulmate.authfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.soulmate.R
import com.example.soulmate.databinding.FragmentOtpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit

class OtpFragment : Fragment() {

    private lateinit var binding: FragmentOtpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var phoneNumber: String
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            verificationId = it.getString("verificationId", "")
            phoneNumber = it.getString("phoneNumber", "")
            forceResendingToken = it.getParcelable("forceResendingToken")
        }
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtpBinding.inflate(inflater, container, false)

        binding.otpLoginBtn.setOnClickListener {
            val otp = getOtpFromFields()
            if (isValidOtp(otp)) {
                binding.progressBar.visibility = View.VISIBLE
                binding.otpLoginBtn.isEnabled = false
                verifyOtp(otp)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.resendOTP.setOnClickListener {
            resendVerificationCode()
        }

        return binding.root
    }

    private fun getOtpFromFields(): String {
        return binding.etOtp1.text.toString() +
                binding.etOtp2.text.toString() +
                binding.etOtp3.text.toString() +
                binding.etOtp4.text.toString() +
                binding.etOtp5.text.toString() +
                binding.etOtp6.text.toString()
    }

    private fun isValidOtp(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() } // Check for 6-digit numeric OTP
    }

    private fun verifyOtp(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar.visibility = View.GONE
                binding.otpLoginBtn.isEnabled = true

                if (task.isSuccessful) {
                    navigateToUserScreen()
                } else {
                    val errorMessage = task.exception?.message ?: "Verification failed"
                    Toast.makeText(requireContext(), "Verification failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resendVerificationCode() {
        binding.progressBar.visibility = View.VISIBLE

        if (forceResendingToken != null) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(verificationCallbacks)
                .setForceResendingToken(forceResendingToken!!)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            Toast.makeText(requireContext(), "Resending OTP is not available", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val otp = credential.smsCode
            if (otp != null) {
                fillOtpFields(otp)
                verifyOtp(otp)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            binding.progressBar.visibility = View.GONE
            this@OtpFragment.verificationId = verificationId
            this@OtpFragment.forceResendingToken = token
            Toast.makeText(requireContext(), "OTP has been resent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fillOtpFields(otp: String) {
        if (otp.length == 6) {
            binding.etOtp1.setText(otp[0].toString())
            binding.etOtp2.setText(otp[1].toString())
            binding.etOtp3.setText(otp[2].toString())
            binding.etOtp4.setText(otp[3].toString())
            binding.etOtp5.setText(otp[4].toString())
            binding.etOtp6.setText(otp[5].toString())
        }
    }

    private fun navigateToUserScreen() {
        findNavController().navigate(R.id.action_otpFragment_to_userDetailsFragment)
    }
}
