package com.routeguard.android.auth.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.routeguard.android.R
import com.routeguard.android.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginScreen : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
                    is AuthUiState.LoginLoading -> showLoading(true)
                    is AuthUiState.LoginSuccess -> onLoginSuccess(state.user)
                    is AuthUiState.LoginError -> onLoginError(state.message)
                    is AuthUiState.TokenRefreshSuccess -> onTokenRefreshSuccess(state.user)
                    is AuthUiState.TokenRefreshError -> onTokenRefreshError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.login(email, password)
        }

        binding.btnRegister.setOnClickListener {
            // Navigate to register screen
            findNavController().navigate(R.id.action_loginScreen_to_registerScreen)
        }

        binding.btnForgotPassword.setOnClickListener {
            // Navigate to forgot password screen
            findNavController().navigate(R.id.action_loginScreen_to_forgotPasswordScreen)
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnLogin.isEnabled = !isLoading
        if (isLoading) {
            binding.btnLogin.text = getString(R.string.logging_in)
        } else {
            binding.btnLogin.text = getString(R.string.login)
        }
    }

    private fun onLoginSuccess(user: AuthUiState.User) {
        showLoading(false)
        Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
        // Navigate to home screen
        findNavController().navigate(R.id.action_loginScreen_to_homeScreen)
    }

    private fun onLoginError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun onTokenRefreshSuccess(user: AuthUiState.User) {
        // Token refreshed successfully, user remains logged in
    }

    private fun onTokenRefreshError(message: String) {
        // Token refresh failed, user needs to login again
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Clear tokens and navigate to login
        // This would be handled by the ViewModel or a separate session manager
    }

    private fun togglePasswordVisibility() {
        val isPasswordVisible = binding.etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        // Move cursor to the end
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        // Toggle icon
        binding.ivTogglePassword.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}