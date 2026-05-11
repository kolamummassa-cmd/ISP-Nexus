package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.DefaultersRepository
import com.example.ispnexus.models.Institution
import com.example.ispnexus.models.Payment
import com.example.ispnexus.models.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

// ── Computed defaulter entry (not stored in Firestore) ────────────────────────

data class DefaulterEntry(
    val institution: Institution,
    val subscription: Subscription,
    val daysOverdue: Long,
    val amountOwed: Double,
    val lastPaymentDate: Long    // 0L if no payment ever made
)

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class DefaultersUiState {
    object Loading : DefaultersUiState()
    data class Success(val defaulters: List<DefaulterEntry>) : DefaultersUiState()
    data class Error(val message: String) : DefaultersUiState()
}

sealed class DefaulterActionState {
    object Idle : DefaulterActionState()
    object Loading : DefaulterActionState()
    object Success : DefaulterActionState()
    data class Error(val message: String) : DefaulterActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DefaultersViewModel : ViewModel() {

    private val repository = DefaultersRepository()
    private val db         = FirebaseFirestore.getInstance()
    private val auth       = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<DefaultersUiState>(DefaultersUiState.Loading)
    val state: StateFlow<DefaultersUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<DefaulterActionState>(DefaulterActionState.Idle)
    val actionState: StateFlow<DefaulterActionState> = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init { loadDefaulters() }

    // ── Load — combine all 3 streams, compute defaulters ─────────────────────
    fun loadDefaulters() {
        viewModelScope.launch {
            _state.value = DefaultersUiState.Loading
            try {
                val uid = auth.currentUser?.uid ?: run {
                    _state.value = DefaultersUiState.Error("Not authenticated")
                    return@launch
                }
                val userDoc   = db.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: run {
                    _state.value = DefaultersUiState.Error("Company not found")
                    return@launch
                }

                combine(
                    repository.observeInstitutions(companyId),
                    repository.observeSubscriptions(companyId),
                    repository.observePayments(companyId)
                ) { institutions, subscriptions, payments ->
                    computeDefaulters(institutions, subscriptions, payments)
                }
                    .catch { e -> _state.value = DefaultersUiState.Error(e.message ?: "Unknown error") }
                    .collect { defaulters -> _state.value = DefaultersUiState.Success(defaulters) }

            } catch (e: Exception) {
                _state.value = DefaultersUiState.Error(e.message ?: "Failed to load defaulters")
            }
        }
    }

    // ── Core computation ──────────────────────────────────────────────────────
    private fun computeDefaulters(
        institutions: List<Institution>,
        subscriptions: List<Subscription>,
        payments: List<Payment>
    ): List<DefaulterEntry> {
        val now = System.currentTimeMillis()
        val institutionMap = institutions.associateBy { institution: Institution -> institution.id }

        // Map institutionId -> last completed payment date
        val lastPaymentMap = payments
            .filter { it.status == "completed" }
            .groupBy { it.institutionId }
            .mapValues { (_, list) ->
                list.mapNotNull { p -> (p.paidAt as? Long) }.maxOrNull() ?: 0L
            }
        return subscriptions
            .filter { sub ->
                // Overdue = end date has passed and subscription is not active/resolved
                sub.endDate in 1..<now &&
                        sub.status != "active"
            }
            .mapNotNull { sub ->
                val institution = institutionMap[sub.institutionId] ?: return@mapNotNull null
                val daysOverdue = TimeUnit.MILLISECONDS.toDays(now - sub.endDate)
                DefaulterEntry(
                    institution     = institution,
                    subscription    = sub,
                    daysOverdue     = daysOverdue,
                    amountOwed      = sub.amountKsh,
                    lastPaymentDate = lastPaymentMap[sub.institutionId] ?: 0L
                )
            }
            .sortedByDescending { it.daysOverdue }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun filteredDefaulters(defaulters: List<DefaulterEntry>): List<DefaulterEntry> {
        val query = _searchQuery.value.trim().lowercase()
        return if (query.isEmpty()) defaulters
        else defaulters.filter { entry ->
            entry.institution.name.lowercase().contains(query) ||
                    entry.institution.contactPersonName.lowercase().contains(query)
        }
    }

    // ── Summary helpers ───────────────────────────────────────────────────────
    fun totalAmountOwed(defaulters: List<DefaulterEntry>): Double =
        defaulters.sumOf { it.amountOwed }

    fun criticalCount(defaulters: List<DefaulterEntry>): Int =
        defaulters.count { it.daysOverdue >= 30 }

    // ── Severity label ────────────────────────────────────────────────────────
    fun severity(daysOverdue: Long): String = when {
        daysOverdue >= 60 -> "Critical"
        daysOverdue >= 30 -> "High"
        daysOverdue >= 14 -> "Medium"
        else              -> "Low"
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun sendReminder(subscriptionId: String, note: String) {
        viewModelScope.launch {
            _actionState.value = DefaulterActionState.Loading
            val result = repository.addReminderNote(subscriptionId, note)
            _actionState.value = when {
                result.isSuccess -> DefaulterActionState.Success
                else -> DefaulterActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send reminder"
                )
            }
        }
    }

    fun markResolved(subscriptionId: String) {
        viewModelScope.launch {
            _actionState.value = DefaulterActionState.Loading
            try {
                // ── Guard: check completed payment exists first ────────────────
                val paymentsSnapshot = db.collection("payments")
                    .whereEqualTo("subscriptionId", subscriptionId)
                    .whereEqualTo("status", "completed")
                    .get()
                    .await()

                if (paymentsSnapshot.isEmpty) {
                    _actionState.value = DefaulterActionState.Error(
                        "Cannot resolve — no completed payment found for this subscription. " +
                                "Please record a payment first."
                    )
                    return@launch
                }

                // ── Payment exists — proceed to resolve ───────────────────────
                val result = repository.markResolved(subscriptionId)
                _actionState.value = when {
                    result.isSuccess -> DefaulterActionState.Success
                    else -> DefaulterActionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to mark as resolved"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = DefaulterActionState.Error(
                    e.message ?: "Failed to verify payment"
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = DefaulterActionState.Idle }
}