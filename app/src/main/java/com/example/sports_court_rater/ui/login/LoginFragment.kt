package com.example.sports_court_rater.ui.login

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.databinding.FragmentLoginBinding
import com.example.sports_court_rater.ui.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                viewModel.loginUser(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToSignUpFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    binding.progressBar.isVisible = state is AuthState.Loading
                    binding.btnLogin.isEnabled = state !is AuthState.Loading

                    when (state) {
                        is AuthState.Success -> {
                            val action = LoginFragmentDirections.actionLoginFragmentToHomeFragment()
                            findNavController().navigate(action)
                        }
                        is AuthState.Error -> {
                            showError(translateError(state.message))
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun translateError(message: String): String {
        return when {
            message.contains("invalid-credential", ignoreCase = true) || 
            message.contains("wrong-password", ignoreCase = true) ||
            message.contains("user-not-found", ignoreCase = true) ||
            message.contains("no user", ignoreCase = true) ||
            message.contains("incorrect, malformed or has expired", ignoreCase = true) -> "אימייל או סיסמה לא נכונים"
            message.contains("network error", ignoreCase = true) -> "שגיאת רשת, אנא נסה שוב מאוחר יותר"
            message.contains("too many requests", ignoreCase = true) ||
            message.contains("blocked all requests", ignoreCase = true) ||
            message.contains("unusual activity", ignoreCase = true) -> "יותר מדי ניסיונות כושלים, אנא נסה שוב מאוחר יותר"
            else -> "ההתחברות נכשלה: $message"
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "אימייל נדרש"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "כתובת אימייל לא תקינה"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 6) {
            binding.tilPassword.error = "סיסמה חייבת להיות לפחות 6 תווים"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
