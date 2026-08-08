package com.routeguard.android.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.routeguard.android.R
import com.routeguard.android.databinding.FragmentResetPasswordBinding
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
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
            val isChecked = !binding.etPassword.isFocused
            binding.etPassword.isFocused = isChecked
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
            // Toggle password visibility
            binding.etPassword.transformationMethod = if (isChecked) {
                android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                android.text.method.PasswordTransformationMethod.getInstance()
            }
            // Toggle icon
            binding.ivTogglePassword.setImageResource(
                if (isChecked) R.drawable.ic_eye else R.drawable.ic_eye_off
            )
        }

        binding.ivTogglePasswordConfirm.setOnClickListener {
            val isChecked = !binding.etPasswordConfirm.isFocused
            binding.etPasswordConfirm.isFocused = isChecked
            binding.etPasswordConfirm.setSelection(binding.etPasswordConfirm.text?.length ?: 0)
            // Toggle password visibility
            binding.etPasswordConfirm.transformationMethod = if (isChecked) {
                android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                android.text.method.PasswordTransformationMethod.getInstance()
            }
            // Toggle icon
            binding.ivTogglePasswordConfirm.setImageResource(
                if (isChecked) R.drawable.ic_eye else R.drawable.ic_eye_off
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnResetPassword.isEnabled = !isLoading
        if (isLoading) {
            binding.btnResetPassword.text = getString(R.string.resetting)
        } else {
            binding.btnResetPassword.text = getString(R.string.reset_password)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}