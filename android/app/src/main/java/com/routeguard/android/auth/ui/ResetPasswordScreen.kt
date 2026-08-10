package com.routeguard.android.auth.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.routeguard.android.R
import com.routeguard.android.databinding.FragmentResetPasswordBinding
import com.routeguard.android.ui.auth.AuthUiState
import com.routeguard.android.ui.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ResetPasswordScreen : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.uiState.collectLatest { state ->
                when (state) {
                    is AuthUiState.ResetPasswordLoading -> showLoading(true)
                    is AuthUiState.ResetPasswordSuccess -> onResetPasswordSuccess(state.message)
                    is AuthUiState.ResetPasswordError -> onResetPasswordError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnResetPassword.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val passwordConfirm = binding.etPasswordConfirm.text.toString()

            // Basic validation
            if (token.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.resetPassword(token, password)
        }

        binding.tvBackToLogin.setOnClickListener {
            // Navigate to login screen
            findNavController().popBackStack()
        }

        // Password toggle functionality
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.ivTogglePassword)
        }

        binding.ivTogglePasswordConfirm.setOnClickListener {
            togglePasswordVisibility(binding.etPasswordConfirm, binding.ivTogglePasswordConfirm)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnResetPassword.isEnabled = !isLoading
        if (isLoading) {
            binding.btnResetPassword.text = getString(R.string.resetting)
        } else {
            binding.btnResetPassword.text = getString(R.string.action_reset_password)
        }
    }

    private fun onResetPasswordSuccess(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        // Navigate to login screen after successful reset
        findNavController().popBackStack()
    }

    private fun onResetPasswordError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText, imageView: android.widget.ImageView) {
        val isPasswordVisible = editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        editText.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        // Move cursor to the end
        editText.setSelection(editText.text?.length ?: 0)
        // Toggle icon
        imageView.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
