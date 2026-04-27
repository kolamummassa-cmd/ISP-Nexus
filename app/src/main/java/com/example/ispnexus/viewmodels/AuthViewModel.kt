package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Auth States ───────────────────────────────────────────────────────────────

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    // ── Login ─────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("Email and password required")
            return
        }

        _loginState.value = AuthState.Loading

        viewModelScope.launch {
            repository.login(email.trim(), password).fold(
                onSuccess = { user ->
                    _loginState.value = AuthState.Success(user.role)
                },
                onFailure = { error ->
                    _loginState.value = AuthState.Error(
                        error.message ?: "Login failed. Please try again."
                    )
                }
            )
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        repository.logout()
        _loginState.value = AuthState.Idle
    }

    // ── Reset state ───────────────────────────────────────────────────────────

    fun resetState() {
        _loginState.value = AuthState.Idle
    }
}