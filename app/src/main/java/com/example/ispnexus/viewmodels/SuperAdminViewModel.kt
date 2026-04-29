package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.CompanyRepository
import com.example.ispnexus.models.Company
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI States ─────────────────────────────────────────────────────────────────

sealed class CompanyListState {
    object Loading : CompanyListState()
    data class Success(val companies: List<Company>) : CompanyListState()
    data class Error(val message: String) : CompanyListState()
    object Empty : CompanyListState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SuperAdminViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _pendingCompanies  = MutableStateFlow<CompanyListState>(CompanyListState.Loading)
    val pendingCompanies: StateFlow<CompanyListState> = _pendingCompanies.asStateFlow()

    private val _approvedCompanies = MutableStateFlow<CompanyListState>(CompanyListState.Loading)
    val approvedCompanies: StateFlow<CompanyListState> = _approvedCompanies.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _pendingCount  = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _approvedCount = MutableStateFlow(0)
    val approvedCount: StateFlow<Int> = _approvedCount.asStateFlow()

    private val _rejectedCount = MutableStateFlow(0)      // ← new
    val rejectedCount: StateFlow<Int> = _rejectedCount.asStateFlow()

    init {
        loadPendingCompanies()
        loadApprovedCompanies()
        loadRejectedCount()            // ← load on init
    }

    // ── Load pending ──────────────────────────────────────────────────────────

    fun loadPendingCompanies() {
        viewModelScope.launch {
            _pendingCompanies.value = CompanyListState.Loading
            repository.getPendingCompanies().fold(
                onSuccess = { companies ->
                    _pendingCount.value     = companies.size
                    _pendingCompanies.value = if (companies.isEmpty())
                        CompanyListState.Empty else CompanyListState.Success(companies)
                },
                onFailure = { error ->
                    _pendingCompanies.value = CompanyListState.Error(
                        error.message ?: "Failed to load pending companies"
                    )
                }
            )
        }
    }

    // ── Load approved ─────────────────────────────────────────────────────────

    fun loadApprovedCompanies() {
        viewModelScope.launch {
            _approvedCompanies.value = CompanyListState.Loading
            repository.getApprovedCompanies().fold(
                onSuccess = { companies ->
                    _approvedCount.value     = companies.size
                    _approvedCompanies.value = if (companies.isEmpty())
                        CompanyListState.Empty else CompanyListState.Success(companies)
                },
                onFailure = { error ->
                    _approvedCompanies.value = CompanyListState.Error(
                        error.message ?: "Failed to load approved companies"
                    )
                }
            )
        }
    }

    // ── Load rejected count ───────────────────────────────────────────────────

    fun loadRejectedCount() {
        viewModelScope.launch {
            repository.getRejectedCompanies().fold(
                onSuccess = { companies -> _rejectedCount.value = companies.size },
                onFailure = { }
            )
        }
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    fun approveCompany(companyId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.approveCompany(companyId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Company approved successfully")
                    loadPendingCompanies()
                    loadApprovedCompanies()
                    loadRejectedCount()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to approve company"
                    )
                }
            )
        }
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    fun rejectCompany(companyId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.rejectCompany(companyId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Company rejected")
                    loadPendingCompanies()
                    loadRejectedCount()   // ← refresh rejected count after rejection
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to reject company"
                    )
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}