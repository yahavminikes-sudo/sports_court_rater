package com.example.sports_court_rater.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.AuthRepository
import com.example.sports_court_rater.User
import com.example.sports_court_rater.data.CourtRepository
import com.example.sports_court_rater.ui.AuthState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courtRepository: CourtRepository
) : ViewModel() {

    private val _signUpState = MutableStateFlow<AuthState<FirebaseUser>>(AuthState.Idle)
    val signUpState: StateFlow<AuthState<FirebaseUser>> = _signUpState.asStateFlow()

    fun signUpUser(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _signUpState.value = AuthState.Loading
            val result = authRepository.registerUser(email, password)
            _signUpState.value = result.fold(
                onSuccess = { user ->
                    try {
                        val profileUpdates = userProfileChangeRequest {
                            this.displayName = displayName
                        }
                        user.updateProfile(profileUpdates).await()
                        
                        val newUser = User(
                            userId = user.uid,
                            displayName = displayName,
                            profilePictureUrl = ""
                        )
                        courtRepository.saveUser(newUser)

                        AuthState.Success(user)
                    } catch (e: Exception) {
                        AuthState.Error(e.message ?: "Failed to update profile")
                    }
                },
                onFailure = { AuthState.Error(it.message ?: "Sign up failed") }
            )
        }
    }
}
