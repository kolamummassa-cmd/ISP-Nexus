package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Data Models ───────────────────────────────────────────────────────────────

data class AdminDashboardData(
    val companyName: String = "",
    val totalInstitutions: Int = 0,
    val activeSubscriptions: Int = 0,
    val pendingPayments: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val suspendedInstitutions: Int = 0,
    val activeTechnicians: Int = 0,
    val collectionRate: Float = 0f,
    val collected: Double = 0.0,
    val outstanding: Double = 0.0,
    val mostPopularPlan: String = "",
    val mostPopularPlanSubs: Int = 0,
    val mostPopularPlanPercent: Float = 0f,
    val defaulterRate: Float = 0f,
    val recentInstitutions: List<RecentInstitution> = emptyList(),
    val recentPayments: List<RecentPayment> = emptyList(),
    val monthlyRevenueTrend: List<RevenueStat> = emptyList()
)

data class RecentInstitution(
    val name: String = "",
    val plan: String = "",
    val status: String = ""
)

data class RecentPayment(
    val institution: String = "",
    val amount: Double = 0.0,
    val status: String = ""
)

data class RevenueStat(
    val month: String = "",package com.example.ispnexus.viewmodels

val amount: Double = 0.0
)

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class AdminDashboardState {
    object Loading : AdminDashboardState()
    data class Success(val data: AdminDashboardData) : AdminDashboardState()
    data class Error(val message: String) : AdminDashboardState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<AdminDashboardState>(AdminDashboardState.Loading)
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard(companyId: String = "") {
        viewModelScope.launch {
            _state.value = AdminDashboardState.Loading
            try {
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

                // ── Institutions ──────────────────────────────────────────
                val institutionsSnap = db.collection("institutions").get().await()
                val allInstitutions  = institutionsSnap.documents
                val totalInstitutions    = allInstitutions.size
                val suspendedInstitutions = allInstitutions.count {
                    it.getString("status") == "suspended"
                }

                // ── Subscriptions ─────────────────────────────────────────
                val subscriptionsSnap = db.collection("subscriptions").get().await()
                val allSubscriptions  = subscriptionsSnap.documents
                val activeSubscriptions = allSubscriptions.count {
                    it.getString("status") == "active"
                }

                // ── Payments ──────────────────────────────────────────────
                val paymentsSnap = db.collection("payments").get().await()
                val allPayments  = paymentsSnap.documents
                val collected    = allPayments
                    .filter { it.getString("status") == "paid" }
                    .sumOf { it.getDouble("amount") ?: 0.0 }
                val outstanding  = allPayments
                    .filter { it.getString("status") == "pending" }
                    .sumOf { it.getDouble("amount") ?: 0.0 }
                val totalExpected = collected + outstanding
                val collectionRate = if (totalExpected > 0)
                    (collected / totalExpected * 100).toFloat() else 0f

                // Monthly revenue (current month)
                val cal = java.util.Calendar.getInstance()
                val currentMonth = cal.get(java.util.Calendar.MONTH)
                val monthlyRevenue = allPayments
                    .filter { doc ->
                        val createdAt = doc.get("createdAt")
                        val millis: Long? = when (createdAt) {
                            is Long -> createdAt
                            is com.google.firebase.Timestamp -> createdAt.toDate().time
                            else -> null
                        }
                        millis?.let {
                            val c = java.util.Calendar.getInstance()
                            c.timeInMillis = it
                            c.get(java.util.Calendar.MONTH) == currentMonth
                        } ?: false
                    }
                    .sumOf { it.getDouble("amount") ?: 0.0 }

                val pendingPayments = outstanding

                // ── Monthly revenue trend (last 6 months) ─────────────────
                val revenueMap = mutableMapOf<String, Double>()
                allPayments.filter { it.getString("status") == "paid" }.forEach { doc ->
                    val createdAt = doc.get("createdAt")
                    val millis: Long? = when (createdAt) {
                        is Long -> createdAt
                        is com.google.firebase.Timestamp -> createdAt.toDate().time
                        else -> null
                    }
                    millis?.let {
                        val c = java.util.Calendar.getInstance()
                        c.timeInMillis = it
                        val month = months[c.get(java.util.Calendar.MONTH)]
                        revenueMap[month] = (revenueMap[month] ?: 0.0) + (doc.getDouble("amount") ?: 0.0)
                    }
                }
                val last6Months = (5 downTo 0).map { offset ->
                    val c = java.util.Calendar.getInstance()
                    c.add(java.util.Calendar.MONTH, -offset)
                    months[c.get(java.util.Calendar.MONTH)]
                }
                val monthlyRevenueTrend = last6Months.map { month ->
                    RevenueStat(month, revenueMap[month] ?: 0.0)
                }

                // ── Most popular plan ─────────────────────────────────────
                val planCount = mutableMapOf<String, Int>()
                allSubscriptions.forEach { doc ->
                    val plan = doc.getString("planName") ?: "Unknown"
                    planCount[plan] = (planCount[plan] ?: 0) + 1
                }
                val topPlan     = planCount.maxByOrNull { it.value }
                val topPlanName = topPlan?.key ?: "N/A"
                val topPlanSubs = topPlan?.value ?: 0
                val topPlanPct  = if (allSubscriptions.isNotEmpty())
                    topPlanSubs.toFloat() / allSubscriptions.size * 100f else 0f

                // ── Defaulter rate ────────────────────────────────────────
                val defaulters    = allPayments.count { it.getString("status") == "overdue" }
                val defaulterRate = if (allPayments.isNotEmpty())
                    defaulters.toFloat() / allPayments.size * 100f else 0f

                // ── Technicians ───────────────────────────────────────────
                val techSnap = db.collection("technicians").get().await()
                val activeTechnicians = techSnap.documents.count {
                    it.getString("status") == "active"
                }

                // ── Recent institutions ───────────────────────────────────
                val recentInstitutions = allInstitutions.takeLast(5).map { doc ->
                    RecentInstitution(
                        name   = doc.getString("name") ?: "",
                        plan   = doc.getString("planName") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                }

                // ── Recent payments ───────────────────────────────────────
                val recentPayments = allPayments.takeLast(5).map { doc ->
                    RecentPayment(
                        institution = doc.getString("institutionName") ?: "",
                        amount      = doc.getDouble("amount") ?: 0.0,
                        status      = doc.getString("status") ?: ""
                    )
                }

                // ── Company name ──────────────────────────────────────────
                val companySnap = if (companyId.isNotBlank())
                    db.collection("companies").document(companyId).get().await()
                else null
                val companyName = companySnap?.getString("companyName") ?: "My ISP"

                _state.value = AdminDashboardState.Success(
                    AdminDashboardData(
                        companyName            = companyName,
                        totalInstitutions      = totalInstitutions,
                        activeSubscriptions    = activeSubscriptions,
                        pendingPayments        = pendingPayments,
                        monthlyRevenue         = monthlyRevenue,
                        suspendedInstitutions  = suspendedInstitutions,
                        activeTechnicians      = activeTechnicians,
                        collectionRate         = collectionRate,
                        collected              = collected,
                        outstanding            = outstanding,
                        mostPopularPlan        = topPlanName,
                        mostPopularPlanSubs    = topPlanSubs,
                        mostPopularPlanPercent = topPlanPct,
                        defaulterRate          = defaulterRate,
                        recentInstitutions     = recentInstitutions,
                        recentPayments         = recentPayments,
                        monthlyRevenueTrend    = monthlyRevenueTrend
                    )
                )
            } catch (e: Exception) {
                _state.value = AdminDashboardState.Error(e.message ?: "Failed to load dashboard")
            }
        }
    }
}
