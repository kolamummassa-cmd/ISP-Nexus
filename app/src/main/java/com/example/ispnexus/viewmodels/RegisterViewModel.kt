package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Register States ───────────────────────────────────────────────────────────

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun registerCompany(
        adminName: String,
        email: String,
        password: String,
        companyName: String,
        registrationNumber: String,
        taxPin: String,
        phoneNumber: String,
        logoUri: String? = null
    ) {
        // ── Validation ────────────────────────────────────────────────────────
        when {
            adminName.isBlank()          -> { _registerState.value = RegisterState.Error("Admin name is required");                return }
            email.isBlank()              -> { _registerState.value = RegisterState.Error("Email is required");                     return }
            !email.contains("@")         -> { _registerState.value = RegisterState.Error("Enter a valid email address");           return }
            password.length < 6          -> { _registerState.value = RegisterState.Error("Password must be at least 6 characters"); return }
            companyName.isBlank()        -> { _registerState.value = RegisterState.Error("Company name is required");              return }
            registrationNumber.isBlank() -> { _registerState.value = RegisterState.Error("Registration number is required");       return }
            taxPin.isBlank()             -> { _registerState.value = RegisterState.Error("Tax PIN is required");                   return }
            phoneNumber.isBlank()        -> { _registerState.value = RegisterState.Error("Phone number is required");              return }
        }

        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            repository.registerCompany(
                adminName          = adminName.trim(),
                email              = email.trim(),
                password           = password,
                companyName        = companyName.trim(),
                registrationNumber = registrationNumber.trim(),
                taxPin             = taxPin.trim(),
                phoneNumber        = phoneNumber.trim(),
                logoUri            = logoUri
            ).fold(
                onSuccess = {
                    _registerState.value = RegisterState.Success
                },
                onFailure = { error ->
                    _registerState.value = RegisterState.Error(
                        error.message ?: "Registration failed. Please try again."
                    )
                }
            )
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}