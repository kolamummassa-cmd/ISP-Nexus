package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.SubscriptionsRepository
import com.example.ispnexus.models.Institution
import com.example.ispnexus.models.Plan
import com.example.ispnexus.models.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class SubscriptionsState {
    object Loading : SubscriptionsState()
    data class Success(val subscriptions: List<Subscription>) : SubscriptionsState()
    data class Error(val message: String) : SubscriptionsState()
}

sealed class SubscriptionActionState {
    object Idle : SubscriptionActionState()
    object Loading : SubscriptionActionState()
    object Success : SubscriptionActionState()
    data class Error(val message: String) : SubscriptionActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SubscriptionsViewModel : ViewModel() {

    private val repository = SubscriptionsRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<SubscriptionsState>(SubscriptionsState.Loading)
    val state: StateFlow<SubscriptionsState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<SubscriptionActionState>(SubscriptionActionState.Idle)
    val actionState: StateFlow<SubscriptionActionState> = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("all")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _billingCycleFilter = MutableStateFlow("all")
    val billingCycleFilter: StateFlow<String> = _billingCycleFilter.asStateFlow()
    // Add these two StateFlows to SubscriptionsViewModel
    private val _institutions = MutableStateFlow<List<Institution>>(emptyList())
    val institutions: StateFlow<List<Institution>> = _institutions.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    // Load them in init
    init {
        loadSubscriptions()
        loadInstitutions()
        loadPlans()
    }

    private fun loadInstitutions() {
        viewModelScope.launch {
            val uid       = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val db        = FirebaseFirestore.getInstance()
            val userDoc   = db.collection("users").document(uid).get().await()
            val companyId = userDoc.getString("companyId") ?: return@launch

            db.collection("institutions")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", "Active")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Institution::class.java)?.copy(id = it.id) }
                .also { _institutions.value = it }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            val uid       = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val db        = FirebaseFirestore.getInstance()
            val userDoc   = db.collection("users").document(uid).get().await()
            val companyId = userDoc.getString("companyId") ?: return@launch

            db.collection("plans")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", "Active")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Plan::class.java)?.copy(id = it.id) }
                .also { _plans.value = it }
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadSubscriptions() {
        viewModelScope.launch {
            _state.value = SubscriptionsState.Loading
            try {
                val uid = auth.currentUser?.uid ?: run {
                    _state.value = SubscriptionsState.Error("Not authenticated")
                    return@launch
                }
                val userDoc = db.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: run {
                    _state.value = SubscriptionsState.Error("Company not found")
                    return@launch
                }
                repository.observeSubscriptions(companyId)
                    .catch { e -> _state.value = SubscriptionsState.Error(e.message ?: "Unknown error") }
                    .collect { list -> _state.value = SubscriptionsState.Success(list) }
            } catch (e: Exception) {
                _state.value = SubscriptionsState.Error(e.message ?: "Failed to load subscriptions")
            }
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String) { _statusFilter.value = status }
    fun onBillingCycleFilterChange(cycle: String) { _billingCycleFilter.value = cycle }

    fun filteredSubscriptions(subscriptions: List<Subscription>): List<Subscription> {
        val query = _searchQuery.value.trim().lowercase()
        val status = _statusFilter.value
        val cycle = _billingCycleFilter.value
        return subscriptions.filter { sub ->
            val matchesSearch = query.isEmpty() ||
                    sub.institutionName.lowercase().contains(query) ||
                    sub.planName.lowercase().contains(query)
            val matchesStatus = status == "all" || sub.status == status
            val matchesCycle = cycle == "all" || sub.billingCycle == cycle
            matchesSearch && matchesStatus && matchesCycle
        }
    }

    // ── Summary helpers ───────────────────────────────────────────────────────
    fun totalActive(subscriptions: List<Subscription>) =
        subscriptions.count { it.status == "active" }

    fun totalSuspended(subscriptions: List<Subscription>) =
        subscriptions.count { it.status == "suspended" }

    fun totalExpired(subscriptions: List<Subscription>) =
        subscriptions.count { it.status == "expired" }

    fun totalMonthlyRevenue(subscriptions: List<Subscription>): Double =
        subscriptions.filter { it.status == "active" }.sumOf { sub ->
            when (sub.billingCycle) {
                "yearly"  -> sub.amountKsh / 12.0
                else      -> sub.amountKsh
            }
        }

    // ── CRUD Actions ──────────────────────────────────────────────────────────
    fun addSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val uid = auth.currentUser?.uid ?: run {
                _actionState.value = SubscriptionActionState.Error("Not authenticated")
                return@launch
            }
            val userDoc = db.collection("users").document(uid).get().await()
            val companyId = userDoc.getString("companyId") ?: run {
                _actionState.value = SubscriptionActionState.Error("Company not found")
                return@launch
            }
            val result = repository.addSubscription(subscription.copy(companyId = companyId))
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to add subscription"
                )
            }
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val result = repository.updateSubscription(subscription)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update subscription"
                )
            }
        }
    }

    fun suspendSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val result = repository.suspendSubscription(subscriptionId)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to suspend subscription"
                )
            }
        }
    }

    fun reactivateSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val result = repository.reactivateSubscription(subscriptionId)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to reactivate subscription"
                )
            }
        }
    }

    fun expireSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val result = repository.expireSubscription(subscriptionId)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to expire subscription"
                )
            }
        }
    }

    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val result = repository.deleteSubscription(subscriptionId)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete subscription"
                )
            }
        }
    }


    fun renewSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _actionState.value = SubscriptionActionState.Loading
            val uid = auth.currentUser?.uid ?: run {
                _actionState.value = SubscriptionActionState.Error("Not authenticated")
                return@launch
            }
            val userDoc   = db.collection("users").document(uid).get().await()
            val companyId = userDoc.getString("companyId") ?: run {
                _actionState.value = SubscriptionActionState.Error("Company not found")
                return@launch
            }
            val result = repository.renewSubscription(subscription, companyId)
            _actionState.value = when {
                result.isSuccess -> SubscriptionActionState.Success
                else -> SubscriptionActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to renew subscription"
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = SubscriptionActionState.Idle }
}