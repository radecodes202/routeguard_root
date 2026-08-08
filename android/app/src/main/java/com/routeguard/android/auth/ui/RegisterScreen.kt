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
import com.routeguard.android.databinding.FragmentRegisterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterScreen : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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
                    is AuthUiState.RegisterLoading -> showLoading(true)
                    is AuthUiState.RegisterSuccess -> onRegisterSuccess(state.message)
                    is AuthUiState.RegisterError -> onRegisterError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val fullName = binding.etFullName.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val passwordConfirm = binding.etPasswordConfirm.text.toString()

            // Basic validation
            if (email.isEmpty() || fullName.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.register(
                email = email,
                phoneNumber = if (phone.isEmpty()) null else phone,
                fullName = fullName,
                password = password,
                passwordConfirmation = passwordConfirm
            )
        }

        binding.btnLogin.setOnClickListener {
            // Navigate to login screen
            findNavController().navigate(R.id.action_registerScreen_to_loginScreen)
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.ivTogglePassword)
        }

        binding.ivTogglePasswordConfirm.setOnClickListener {
            togglePasswordVisibility(binding.etPasswordConfirm, binding.ivTogglePasswordConfirm)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnRegister.isEnabled = !isLoading
        if (isLoading) {
            binding.btnRegister.text = getString(R.string.registering)
        } else {
            binding.btnRegister.text = getString(R.string.register)
        }
    }

    private fun onRegisterSuccess(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        // Navigate to login screen after successful registration
        findNavController().navigate(R.id.action_registerScreen_to_loginScreen)
    }

    private fun onRegisterError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText, imageView: android.widget.ImageView) {
        val isPasswordVisible = editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        editText.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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