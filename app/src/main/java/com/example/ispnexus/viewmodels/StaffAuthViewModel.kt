package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.StaffAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Navigation Destination ────────────────────────────────────────────────────

sealed class StaffDestination {
    object None : StaffDestination()
    object StaffDashboard : StaffDestination()
    object WaitingApproval : StaffDestination()
    object Rejected : StaffDestination()
    object AdminDashboard : StaffDestination()
    object SuperAdminDashboard : StaffDestination()
}

// ── Registration State ────────────────────────────────────────────────────────

sealed class StaffRegisterState {
    object Idle : StaffRegisterState()
    object Loading : StaffRegisterState()
    object Success : StaffRegisterState()
    data class Error(val message: String) : StaffRegisterState()
}

// ── Login State ───────────────────────────────────────────────────────────────

sealed class StaffLoginState {
    object Idle : StaffLoginState()
    object Loading : StaffLoginState()
    data class Success(val destination: StaffDestination) : StaffLoginState()
    data class Error(val message: String) : StaffLoginState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StaffAuthViewModel(
    private val repository: StaffAuthRepository = StaffAuthRepository()
) : ViewModel() {

    private val _registerState = MutableStateFlow<StaffRegisterState>(StaffRegisterState.Idle)
    val registerState: StateFlow<StaffRegisterState> = _registerState.asStateFlow()

    private val _loginState = MutableStateFlow<StaffLoginState>(StaffLoginState.Idle)
    val loginState: StateFlow<StaffLoginState> = _loginState.asStateFlow()

    // ── Staff Registration ────────────────────────────────────────────────────

    fun registerStaff(
        fullName: String,
        email: String,
        password: String,
        companyCode: String,
        position: String
    ) {
        when {
            fullName.isBlank()    -> { _registerState.value = StaffRegisterState.Error("Full name is required");     return }
            email.isBlank()       -> { _registerState.value = StaffRegisterState.Error("Email is required");         return }
            !email.contains("@") -> { _registerState.value = StaffRegisterState.Error("Enter a valid email");        return }
            password.length < 6  -> { _registerState.value = StaffRegisterState.Error("Password must be 6+ chars"); return }
            companyCode.isBlank() -> { _registerState.value = StaffRegisterState.Error("Company code is required");  return }
            position.isBlank()    -> { _registerState.value = StaffRegisterState.Error("Position is required");      return }
        }

        viewModelScope.launch {
            _registerState.value = StaffRegisterState.Loading
            repository.registerStaff(fullName, email, password, companyCode, position).fold(
                onSuccess = { _registerState.value = StaffRegisterState.Success },
                onFailure = { _registerState.value = StaffRegisterState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    // ── Login + Role/Status Navigation ────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = StaffLoginState.Error("Email and password required")
            return
        }

        viewModelScope.launch {
            _loginState.value = StaffLoginState.Loading
            repository.login(email.trim(), password).fold(
                onSuccess = { profile ->
                    val role   = profile["role"] as? String ?: ""
                    val status = profile["status"] as? String ?: ""

                    // ── Navigation logic based on role + status ───────────
                    val destination = when (role.lowercase()) {
                        "super_admin" -> StaffDestination.SuperAdminDashboard
                        "admin", "company_admin" -> StaffDestination.AdminDashboard
                        "staff" -> when (status.lowercase()) {
                            "active"   -> StaffDestination.StaffDashboard
                            "pending"  -> StaffDestination.WaitingApproval
                            "rejected" -> StaffDestination.Rejected
                            else       -> StaffDestination.WaitingApproval
                        }
                        else -> StaffDestination.WaitingApproval
                    }
                    _loginState.value = StaffLoginState.Success(destination)
                },
                onFailure = {
                    _loginState.value = StaffLoginState.Error(it.message ?: "Login failed")
                }
            )
        }
    }

    fun logout() = repository.logout()

    fun resetRegisterState() { _registerState.value = StaffRegisterState.Idle }
    fun resetLoginState()    { _loginState.value    = StaffLoginState.Idle    }
}