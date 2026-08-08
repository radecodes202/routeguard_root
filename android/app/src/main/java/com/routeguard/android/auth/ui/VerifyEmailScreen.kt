package com.routeguard.android.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.routeguard.android.R
import com.routeguard.android.databinding.FragmentVerifyEmailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VerifyEmailScreen : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.uiState.collectLatest { state ->
                when (state) {
                    is AuthUiState.EmailVerificationLoading -> showLoading(true)
                    is AuthUiState.EmailVerificationSuccess -> onEmailVerificationSuccess(state.message)
                    is AuthUiState.EmailVerificationError -> onEmailVerificationError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnVerify.setOnClickListener {
            val token = binding.etCode.text.toString().trim()
            if (token.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the verification token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.verifyEmail(token)
        }

        binding.tvResend.setOnClickListener {
            // In a real app, we would resend the verification email
            Toast.makeText(requireContext(), "Verification email resent", Toast.LENGTH_SHORT).show()
        }

        binding.tvBackToLogin.setOnClickListener {
            // Navigate to login screen
            findNavController().popBackStack()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnVerify.isEnabled = !isLoading
        if (isLoading) {
            binding.btnVerify.text = getString(R.string.verifying)
        } else {
            binding.btnVerify.text = getString(R.string.verify)
        }
    }

    private fun onEmailVerificationSuccess(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        // Navigate to login screen after successful verification
        findNavController().popBackStack()
    }

    private fun onEmailVerificationError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}