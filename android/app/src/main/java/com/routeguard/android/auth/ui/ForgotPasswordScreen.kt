package com.routeguard.android.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.routeguard.android.R
import com.routeguard.android.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ForgotPasswordScreen : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
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
                    is AuthUiState.ForgotPasswordLoading -> showLoading(true)
                    is AuthUiState.ForgotPasswordSuccess -> onForgotPasswordSuccess(state.message)
                    is AuthUiState.ForgotPasswordError -> onForgotPasswordError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Basic email validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.forgotPassword(email)
        }

        binding.tvBackToLogin.setOnClickListener {
            // Navigate to login screen
            findNavController().popBackStack()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSendResetLink.isEnabled = !isLoading
        if (isLoading) {
            binding.btnSendResetLink.text = getString(R.string.sending)
        } else {
            binding.btnSendResetLink.text = getString(R.string.send_reset_link)
        }
    }

    private fun onForgotPasswordSuccess(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Show success message and navigate back to login
        findNavController().popBackStack()
    }

    private fun onForgotPasswordError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}