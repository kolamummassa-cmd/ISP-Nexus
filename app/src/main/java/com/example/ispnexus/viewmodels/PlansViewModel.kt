package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.models.Plan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── UI State ─────────────────────────────────────────────────────────────────

data class PlansUiState(
    val plans: List<Plan> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class PlansViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    private var companyId: String = ""
    private var listener: ListenerRegistration? = null

    init {
        loadPlans()
    }

    // ─── Load Plans (real-time) ───────────────────────────────────────────────

    private fun loadPlans() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                    ?: return@launch emitError("Not logged in")

                val userDoc = db.collection("users").document(uid).get().await()
                companyId   = userDoc.getString("companyId")
                    ?: return@launch emitError("Company not found")

                listener = db.collection("plans")
                    .whereEqualTo("companyId", companyId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading    = false,
                                errorMessage = error.message
                            )
                            return@addSnapshotListener
                        }

                        val plans = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Plan::class.java)?.copy(id = doc.id)
                        } ?: emptyList()

                        _uiState.value = _uiState.value.copy(
                            plans     = plans,
                            isLoading = false
                        )
                    }

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to load plans")
            }
        }
    }

    // ─── Create Plan ──────────────────────────────────────────────────────────

    fun createPlan(
        name: String,
        price: Double,
        speedMbps: Int,
        billingCycle: String,
        description: String,
        isUnlimited: Boolean,
        dataCapGb: Int
    ) {
        viewModelScope.launch {
            try {
                val planData = hashMapOf(
                    "companyId"    to companyId,
                    "name"         to name,
                    "price"        to price,
                    "speedMbps"    to speedMbps,
                    "billingCycle" to billingCycle,
                    "description"  to description,
                    "isUnlimited"  to isUnlimited,
                    "dataCapGb"    to if (isUnlimited) 0 else dataCapGb,
                    "status"       to "Active",
                    "createdAt"    to System.currentTimeMillis(),
                    "updatedAt"    to System.currentTimeMillis()
                )

                db.collection("plans").add(planData).await()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Plan \"$name\" created successfully"
                )

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to create plan")
            }
        }
    }

    // ─── Edit Plan ────────────────────────────────────────────────────────────

    fun updatePlan(
        planId: String,
        name: String,
        price: Double,
        speedMbps: Int,
        billingCycle: String,
        description: String,
        isUnlimited: Boolean,
        dataCapGb: Int
    ) {
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "name"         to name,
                    "price"        to price,
                    "speedMbps"    to speedMbps,
                    "billingCycle" to billingCycle,
                    "description"  to description,
                    "isUnlimited"  to isUnlimited,
                    "dataCapGb"    to if (isUnlimited) 0 else dataCapGb,
                    "updatedAt"    to System.currentTimeMillis()
                )

                db.collection("plans").document(planId).update(updates).await()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Plan updated successfully"
                )

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to update plan")
            }
        }
    }

    // ─── Toggle Active / Inactive ─────────────────────────────────────────────

    fun togglePlanStatus(plan: Plan) {
        viewModelScope.launch {
            try {
                val newStatus = if (plan.status == "Active") "Inactive" else "Active"

                db.collection("plans").document(plan.id)
                    .update(
                        mapOf(
                            "status"    to newStatus,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Plan \"${plan.name}\" is now $newStatus"
                )

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to update plan status")
            }
        }
    }

    // ─── Delete Plan ──────────────────────────────────────────────────────────

    fun deletePlan(plan: Plan) {
        viewModelScope.launch {
            try {
                db.collection("plans").document(plan.id).delete().await()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Plan \"${plan.name}\" deleted"
                )

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to delete plan")
            }
        }
    }

    // ─── Clear messages ───────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage   = null
        )
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun emitError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading    = false,
            errorMessage = message
        )
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
