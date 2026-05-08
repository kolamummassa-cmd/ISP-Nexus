package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.InstitutionsRepository
import com.example.ispnexus.models.Institution
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class InstitutionsState {
    object Loading : InstitutionsState()
    data class Success(val institutions: List<Institution>) : InstitutionsState()
    data class Error(val message: String) : InstitutionsState()
}

sealed class InstitutionActionState {
    object Idle : InstitutionActionState()
    object Loading : InstitutionActionState()
    object Success : InstitutionActionState()
    data class Error(val message: String) : InstitutionActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class InstitutionsViewModel : ViewModel() {

    private val repository = InstitutionsRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<InstitutionsState>(InstitutionsState.Loading)
    val state: StateFlow<InstitutionsState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<InstitutionActionState>(InstitutionActionState.Idle)
    val actionState: StateFlow<InstitutionActionState> = _actionState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected status filter: "all", "active", "suspended"
    private val _statusFilter = MutableStateFlow("all")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private var companyId: String = ""

    init { loadInstitutions() }

    fun loadInstitutions() {
        viewModelScope.launch {
            _state.value = InstitutionsState.Loading
            try {
                fetchAndEmit()
            } catch (e: Exception) {
                _state.value = InstitutionsState.Error(
                    e.message ?: "Failed to load institutions"
                )
            }
        }
    }

    private suspend fun fetchAndEmit() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = InstitutionsState.Error("Not logged in")
            return
        }
        companyId = uid

        repository.observeInstitutions(companyId).collect { institutions ->
            _state.value = InstitutionsState.Success(institutions)
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    fun onStatusFilterChange(status: String) {
        _statusFilter.value = status
    }

    // ── Filtered list helper (call from UI) ───────────────────────────────────
    fun filteredInstitutions(institutions: List<Institution>): List<Institution> {
        val query = _searchQuery.value.trim().lowercase()
        val filter = _statusFilter.value

        return institutions
            .filter { inst ->
                filter == "all" || inst.status.lowercase() == filter.lowercase()
            }
            .filter { inst ->
                query.isEmpty() ||
                        inst.name.lowercase().contains(query) ||
                        inst.email.lowercase().contains(query) ||
                        inst.phoneNumber.contains(query)
            }
    }

    // ── Add ───────────────────────────────────────────────────────────────────
    fun addInstitution(institution: Institution) {
        viewModelScope.launch {
            _actionState.value = InstitutionActionState.Loading
            val result = repository.addInstitution(
                institution.copy(companyId = companyId)
            )
            _actionState.value = if (result.isSuccess) {
                InstitutionActionState.Success
            } else {
                InstitutionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to add institution"
                )
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────
    fun updateInstitution(institution: Institution) {
        viewModelScope.launch {
            _actionState.value = InstitutionActionState.Loading
            val result = repository.updateInstitution(institution)
            _actionState.value = if (result.isSuccess) {
                InstitutionActionState.Success
            } else {
                InstitutionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update institution"
                )
            }
        }
    }

    // ── Suspend ───────────────────────────────────────────────────────────────
    fun suspendInstitution(institutionId: String) {
        viewModelScope.launch {
            _actionState.value = InstitutionActionState.Loading
            val result = repository.suspendInstitution(institutionId)
            _actionState.value = if (result.isSuccess) {
                InstitutionActionState.Success
            } else {
                InstitutionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to suspend institution"
                )
            }
        }
    }

    // ── Reactivate ────────────────────────────────────────────────────────────
    fun reactivateInstitution(institutionId: String) {
        viewModelScope.launch {
            _actionState.value = InstitutionActionState.Loading
            val result = repository.reactivateInstitution(institutionId)
            _actionState.value = if (result.isSuccess) {
                InstitutionActionState.Success
            } else {
                InstitutionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to reactivate institution"
                )
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    fun deleteInstitution(institutionId: String) {
        viewModelScope.launch {
            _actionState.value = InstitutionActionState.Loading
            val result = repository.deleteInstitution(institutionId)
            _actionState.value = if (result.isSuccess) {
                InstitutionActionState.Success
            } else {
                InstitutionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete institution"
                )
            }
        }
    }

    // ── Reset action state (call after showing snackbar/dialog) ───────────────
    fun resetActionState() {
        _actionState.value = InstitutionActionState.Idle
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private fun resolveMillis(value: Any?): Long? {
        return when (value) {
            is Long      -> value
            is Timestamp -> value.toDate().time
            else         -> null
        }
    }
}
