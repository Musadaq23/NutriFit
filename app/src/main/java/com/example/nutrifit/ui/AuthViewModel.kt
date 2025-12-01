package com.example.nutrifit.ui

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val msg: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()
    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> get() = _state

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            val result = repo.login(email, password)
            _state.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun register(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            val result = repo.register(email, password)
            _state.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun logout() = repo.logout()
}
