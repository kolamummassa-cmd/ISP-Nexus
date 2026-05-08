package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.PaymentsRepository
import com.example.ispnexus.models.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class PaymentsUiState {
    object Loading : PaymentsUiState()
    data class Success(val payments: List<Payment>) : PaymentsUiState()
    data class Error(val message: String) : PaymentsUiState()
}

sealed class PaymentActionState {
    object Idle : PaymentActionState()
    object Loading : PaymentActionState()
    object Success : PaymentActionState()
    data class Error(val message: String) : PaymentActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PaymentsViewModel : ViewModel() {

    private val repository = PaymentsRepository()
    private val db         = FirebaseFirestore.getInstance()
    private val auth       = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<PaymentsUiState>(PaymentsUiState.Loading)
    val state: StateFlow<PaymentsUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<PaymentActionState>(PaymentActionState.Idle)
    val actionState: StateFlow<PaymentActionState> = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("all")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _methodFilter = MutableStateFlow("all")
    val methodFilter: StateFlow<String> = _methodFilter.asStateFlow()

    init { loadPayments() }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadPayments() {
        viewModelScope.launch {
            _state.value = PaymentsUiState.Loading
            try {
                val uid = auth.currentUser?.uid ?: run {
                    _state.value = PaymentsUiState.Error("Not authenticated")
                    return@launch
                }
                val userDoc   = db.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: run {
                    _state.value = PaymentsUiState.Error("Company not found")
                    return@launch
                }
                repository.observePayments(companyId)
                    .catch { e -> _state.value = PaymentsUiState.Error(e.message ?: "Unknown error") }
                    .collect { list ->
                        _state.value = PaymentsUiState.Success(
                            list.sortedByDescending { (it.createdAt as? Long) ?: 0L }                        )
                    }
            } catch (e: Exception) {
                _state.value = PaymentsUiState.Error(e.message ?: "Failed to load payments")
            }
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String) { _statusFilter.value = status }
    fun onMethodFilterChange(method: String) { _methodFilter.value = method }

    fun filteredPayments(payments: List<Payment>): List<Payment> {
        val query  = _searchQuery.value.trim().lowercase()
        val status = _statusFilter.value
        val method = _methodFilter.value
        return payments.filter { p ->
            val matchesSearch = query.isEmpty() ||
                    p.institutionName.lowercase().contains(query) ||
                    p.invoiceId.lowercase().contains(query)
            val matchesStatus = status == "all" || p.status == status
            val matchesMethod = method == "all" || p.paymentMethod == method
            matchesSearch && matchesStatus && matchesMethod
        }
    }

    // ── Summary helpers ───────────────────────────────────────────────────────
    fun totalCompleted(payments: List<Payment>) =
        payments.count { it.status == "completed" }

    fun totalPending(payments: List<Payment>) =
        payments.count { it.status == "pending" }

    fun totalFailed(payments: List<Payment>) =
        payments.count { it.status == "failed" }

    fun totalCompletedAmount(payments: List<Payment>): Double =
        payments.filter { it.status == "completed" }.sumOf { it.amountKsh }

    fun totalPendingAmount(payments: List<Payment>): Double =
        payments.filter { it.status == "pending" }.sumOf { it.amountKsh }

    // ── CRUD Actions ──────────────────────────────────────────────────────────
    fun addPayment(payment: Payment) {
        viewModelScope.launch {
            _actionState.value = PaymentActionState.Loading
            val uid = auth.currentUser?.uid ?: run {
                _actionState.value = PaymentActionState.Error("Not authenticated")
                return@launch
            }
            val userDoc   = db.collection("users").document(uid).get().await()
            val companyId = userDoc.getString("companyId") ?: run {
                _actionState.value = PaymentActionState.Error("Company not found")
                return@launch
            }
            val result = repository.addPaymentWithInvoice(payment.copy(companyId = companyId))
            _actionState.value = when {
                result.isSuccess -> PaymentActionState.Success
                else -> PaymentActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to add payment"
                )
            }
        }
    }

    fun updatePayment(payment: Payment) {
        viewModelScope.launch {
            _actionState.value = PaymentActionState.Loading
            val result = repository.updatePayment(payment)
            _actionState.value = when {
                result.isSuccess -> PaymentActionState.Success
                else -> PaymentActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update payment"
                )
            }
        }
    }

    fun markCompleted(payment: Payment) {
        viewModelScope.launch {
            _actionState.value = PaymentActionState.Loading
            val result = repository.markCompleted(payment)
            _actionState.value = when {
                result.isSuccess -> PaymentActionState.Success
                else -> PaymentActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to mark as completed"
                )
            }
        }
    }

    fun markFailed(payment: Payment) {
        viewModelScope.launch {
            _actionState.value = PaymentActionState.Loading
            val result = repository.markFailed(payment.id, payment.invoiceId)
            _actionState.value = when {
                result.isSuccess -> PaymentActionState.Success
                else -> PaymentActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to mark as failed"
                )
            }
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            _actionState.value = PaymentActionState.Loading
            val result = repository.deletePayment(paymentId)
            _actionState.value = when {
                result.isSuccess -> PaymentActionState.Success
                else -> PaymentActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete payment"
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = PaymentActionState.Idle }
}
