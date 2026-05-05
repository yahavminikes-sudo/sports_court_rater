package com.example.sports_court_rater.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.AuthRepository
import com.example.sports_court_rater.data.CourtRepository
import com.example.sports_court_rater.ui.AuthState
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courtRepository: CourtRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState<FirebaseUser>>(AuthState.Idle)
    val loginState: StateFlow<AuthState<FirebaseUser>> = _loginState.asStateFlow()

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = authRepository.loginUser(email, password)
            _loginState.value = result.fold(
                onSuccess = { user ->
                    courtRepository.getUserById(user.uid)
                    AuthState.Success(user)
                },
                onFailure = { AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }
}
