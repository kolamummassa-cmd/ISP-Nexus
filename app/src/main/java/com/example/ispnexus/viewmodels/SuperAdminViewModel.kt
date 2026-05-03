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

    // ── Company list states ───────────────────────────────────────────────────
    private val _pendingCompanies  = MutableStateFlow<CompanyListState>(CompanyListState.Loading)
    val pendingCompanies: StateFlow<CompanyListState> = _pendingCompanies.asStateFlow()

    private val _approvedCompanies = MutableStateFlow<CompanyListState>(CompanyListState.Loading)
    val approvedCompanies: StateFlow<CompanyListState> = _approvedCompanies.asStateFlow()

    // ── Action state ──────────────────────────────────────────────────────────
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // ── Live counts for dashboard stat strip ──────────────────────────────────
    private val _pendingCount  = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _approvedCount = MutableStateFlow(0)
    val approvedCount: StateFlow<Int> = _approvedCount.asStateFlow()

    private val _rejectedCount = MutableStateFlow(0)
    val rejectedCount: StateFlow<Int> = _rejectedCount.asStateFlow()

    init {
        // ── Start real-time listeners — updates happen automatically ──────────
        observePending()
        observeApproved()
        observeRejected()
    }

    // ── Real-time pending listener ────────────────────────────────────────────
    // Whenever super admin approves/rejects, this updates instantly — no rerun needed

    private fun observePending() {
        viewModelScope.launch {
            repository.observePendingCompanies().collect { companies ->
                _pendingCount.value     = companies.size
                _pendingCompanies.value = if (companies.isEmpty())
                    CompanyListState.Empty
                else
                    CompanyListState.Success(companies)
            }
        }
    }

    // ── Real-time approved listener ───────────────────────────────────────────

    private fun observeApproved() {
        viewModelScope.launch {
            repository.observeApprovedCompanies().collect { companies ->
                _approvedCount.value     = companies.size
                _approvedCompanies.value = if (companies.isEmpty())
                    CompanyListState.Empty
                else
                    CompanyListState.Success(companies)
            }
        }
    }

    // ── Real-time rejected listener ───────────────────────────────────────────

    private fun observeRejected() {
        viewModelScope.launch {
            repository.observeRejectedCompanies().collect { companies ->
                _rejectedCount.value = companies.size
            }
        }
    }

    // ── Manual refresh (for refresh button) ───────────────────────────────────
    // Real-time listeners handle updates automatically but refresh button still works

    fun loadPendingCompanies() {
        viewModelScope.launch {
            _pendingCompanies.value = CompanyListState.Loading
            repository.getPendingCompanies().fold(
                onSuccess = { companies ->
                    _pendingCount.value     = companies.size
                    _pendingCompanies.value = if (companies.isEmpty())
                        CompanyListState.Empty
                    else
                        CompanyListState.Success(companies)
                },
                onFailure = { error ->
                    _pendingCompanies.value = CompanyListState.Error(
                        error.message ?: "Failed to load pending companies"
                    )
                }
            )
        }
    }

    fun loadApprovedCompanies() {
        viewModelScope.launch {
            _approvedCompanies.value = CompanyListState.Loading
            repository.getApprovedCompanies().fold(
                onSuccess = { companies ->
                    _approvedCount.value     = companies.size
                    _approvedCompanies.value = if (companies.isEmpty())
                        CompanyListState.Empty
                    else
                        CompanyListState.Success(companies)
                },
                onFailure = { error ->
                    _approvedCompanies.value = CompanyListState.Error(
                        error.message ?: "Failed to load approved companies"
                    )
                }
            )
        }
    }

    fun loadRejectedCount() {
        viewModelScope.launch {
            repository.getRejectedCompanies().fold(
                onSuccess = { companies -> _rejectedCount.value = companies.size },
                onFailure = { }
            )
        }
    }

    // ── Approve ───────────────────────────────────────────────────────────────
    // Real-time listeners will auto-update all counts and lists after this

    fun approveCompany(companyId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.approveCompany(companyId).fold(
                onSuccess = {
                    // No need to manually reload — real-time listeners handle it
                    _actionState.value = ActionState.Success("Company approved successfully")
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
                    // No need to manually reload — real-time listeners handle it
                    _actionState.value = ActionState.Success("Company rejected")
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