package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.InvoicesRepository
import com.example.ispnexus.models.Invoice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class InvoicesUiState {
    object Loading : InvoicesUiState()
    data class Success(val invoices: List<Invoice>) : InvoicesUiState()
    data class Error(val message: String) : InvoicesUiState()
}

sealed class InvoiceActionState {
    object Idle : InvoiceActionState()
    object Loading : InvoiceActionState()
    object Success : InvoiceActionState()
    data class Error(val message: String) : InvoiceActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class InvoicesViewModel : ViewModel() {

    private val repository = InvoicesRepository()
    private val db         = FirebaseFirestore.getInstance()
    private val auth       = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<InvoicesUiState>(InvoicesUiState.Loading)
    val state: StateFlow<InvoicesUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<InvoiceActionState>(InvoiceActionState.Idle)
    val actionState: StateFlow<InvoiceActionState> = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("all")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Invoice selected for detail / edit / PDF preview
    private val _selectedInvoice = MutableStateFlow<Invoice?>(null)
    val selectedInvoice: StateFlow<Invoice?> = _selectedInvoice.asStateFlow()

    init { loadInvoices() }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = InvoicesUiState.Loading
            try {
                val uid = auth.currentUser?.uid ?: run {
                    _state.value = InvoicesUiState.Error("Not authenticated")
                    return@launch
                }
                val userDoc   = db.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: run {
                    _state.value = InvoicesUiState.Error("Company not found")
                    return@launch
                }
                repository.observeInvoices(companyId)
                    .catch { e -> _state.value = InvoicesUiState.Error(e.message ?: "Unknown error") }
                    .collect { list ->
                        _state.value = InvoicesUiState.Success(
                            list.sortedByDescending { (it.createdAt as? Long) ?: 0L }                        )
                    }
            } catch (e: Exception) {
                _state.value = InvoicesUiState.Error(e.message ?: "Failed to load invoices")
            }
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String) { _statusFilter.value = status }
    fun selectInvoice(invoice: Invoice) { _selectedInvoice.value = invoice }
    fun clearSelectedInvoice() { _selectedInvoice.value = null }

    fun filteredInvoices(invoices: List<Invoice>): List<Invoice> {
        val query  = _searchQuery.value.trim().lowercase()
        val status = _statusFilter.value
        return invoices.filter { inv ->
            val matchesSearch = query.isEmpty() ||
                    inv.institutionName.lowercase().contains(query) ||
                    inv.invoiceNumber.lowercase().contains(query)
            val matchesStatus = status == "all" || inv.status == status
            matchesSearch && matchesStatus
        }
    }

    // ── Status label helper — mirrors payment status ───────────────────────────
    // payment "completed" → invoice "paid"
    // payment "pending"   → invoice "pending"
    // payment "failed"    → invoice "failed"
    fun displayStatus(invoice: Invoice): String = when (invoice.status) {
        "paid", "completed" -> "Paid"
        "pending"           -> "Pending"
        "failed"            -> "Failed"
        else                -> invoice.status.replaceFirstChar { it.uppercase() }
    }

    // ── Summary helpers ───────────────────────────────────────────────────────
    fun totalPaid(invoices: List<Invoice>) =
        invoices.count { it.status == "paid" || it.status == "completed" }

    fun totalPending(invoices: List<Invoice>) =
        invoices.count { it.status == "pending" }

    fun totalFailed(invoices: List<Invoice>) =
        invoices.count { it.status == "failed" }

    fun totalPaidAmount(invoices: List<Invoice>): Double =
        invoices.filter { it.status == "paid" || it.status == "completed" }
            .sumOf { it.amountKsh }

    fun totalOutstandingAmount(invoices: List<Invoice>): Double =
        invoices.filter { it.status == "pending" }.sumOf { it.amountKsh }

    // ── Edit notes ────────────────────────────────────────────────────────────
    fun updateNotes(invoiceId: String, notes: String) {
        viewModelScope.launch {
            _actionState.value = InvoiceActionState.Loading
            val result = repository.updateInvoiceNotes(invoiceId, notes)
            _actionState.value = when {
                result.isSuccess -> InvoiceActionState.Success
                else -> InvoiceActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update notes"
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = InvoiceActionState.Idle }
}