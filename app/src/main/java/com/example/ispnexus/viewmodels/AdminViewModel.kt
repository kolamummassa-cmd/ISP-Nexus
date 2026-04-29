package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar



// ─────────────────────────────────────────
// Dashboard Data Model
// ─────────────────────────────────────────

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

// ─────────────────────────────────────────
// Supporting Models
// ─────────────────────────────────────────

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
    val month: String = "",
    val amount: Double = 0.0
)

// ─────────────────────────────────────────
// UI State
// ─────────────────────────────────────────

sealed class AdminDashboardState {
    object Loading : AdminDashboardState()
    data class Success(val data: AdminDashboardData) : AdminDashboardState()
    data class Error(val message: String) : AdminDashboardState()
}

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _state =
        MutableStateFlow<AdminDashboardState>(AdminDashboardState.Loading)
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    fun loadDashboard(companyId: String) {

        viewModelScope.launch {
            _state.value = AdminDashboardState.Loading

            try {

                val months = listOf(
                    "Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec"
                )

                // ─────────────────────────────────────────
                // Institutions (FILTERED BY COMPANY)
                // ─────────────────────────────────────────

                val institutionsSnap = db.collection("institutions")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                val institutions = institutionsSnap.documents

                val totalInstitutions = institutions.size

                val suspendedInstitutions = institutions.count {
                    it.getString("status") == "suspended"
                }

                // ─────────────────────────────────────────
                // Subscriptions
                // ─────────────────────────────────────────

                val subscriptionsSnap = db.collection("subscriptions")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                val subscriptions = subscriptionsSnap.documents

                val activeSubscriptions = subscriptions.count {
                    it.getString("status") == "active"
                }

                // ─────────────────────────────────────────
                // Payments
                // ─────────────────────────────────────────

                val paymentsSnap = db.collection("payments")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                val payments = paymentsSnap.documents

                val collected = payments
                    .filter { it.getString("status") == "paid" }
                    .sumOf { it.getDouble("amount") ?: 0.0 }

                val outstanding = payments
                    .filter { it.getString("status") == "pending" }
                    .sumOf { it.getDouble("amount") ?: 0.0 }

                val pendingPayments = outstanding

                val totalExpected = collected + outstanding

                val collectionRate =
                    if (totalExpected > 0)
                        (collected / totalExpected * 100).toFloat()
                    else 0f

                // ─────────────────────────────────────────
                // Monthly Revenue (Current Month)
                // ─────────────────────────────────────────

                val currentMonth =
                    Calendar.getInstance().get(Calendar.MONTH)

                val monthlyRevenue = payments
                    .filter { it.getString("status") == "paid" }
                    .filter { doc ->
                        val createdAt = doc.get("createdAt")

                        val millis = when (createdAt) {
                            is Long -> createdAt
                            is Timestamp -> createdAt.toDate().time
                            else -> null
                        }

                        millis?.let {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it
                            cal.get(Calendar.MONTH) == currentMonth
                        } ?: false
                    }
                    .sumOf { it.getDouble("amount") ?: 0.0 }

                // ─────────────────────────────────────────
                // Revenue Trend (Last 6 Months)
                // ─────────────────────────────────────────

                val revenueMap = mutableMapOf<String, Double>()

                payments.filter {
                    it.getString("status") == "paid"
                }.forEach { doc ->

                    val createdAt = doc.get("createdAt")

                    val millis = when (createdAt) {
                        is Long -> createdAt
                        is Timestamp -> createdAt.toDate().time
                        else -> null
                    }

                    millis?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it
                        val month = months[cal.get(Calendar.MONTH)]

                        revenueMap[month] =
                            (revenueMap[month] ?: 0.0) +
                                    (doc.getDouble("amount") ?: 0.0)
                    }
                }

                val last6Months = (5 downTo 0).map { offset ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -offset)
                    months[cal.get(Calendar.MONTH)]
                }

                val monthlyRevenueTrend =
                    last6Months.map { month ->
                        RevenueStat(
                            month = month,
                            amount = revenueMap[month] ?: 0.0
                        )
                    }

                // ─────────────────────────────────────────
                // Most Popular Plan
                // ─────────────────────────────────────────

                val planCount = mutableMapOf<String, Int>()

                subscriptions.forEach { doc ->
                    val plan = doc.getString("planName") ?: "Unknown"
                    planCount[plan] = (planCount[plan] ?: 0) + 1
                }

                val topPlan = planCount.maxByOrNull { it.value }

                val mostPopularPlan = topPlan?.key ?: "N/A"
                val mostPopularPlanSubs = topPlan?.value ?: 0

                val mostPopularPlanPercent =
                    if (subscriptions.isNotEmpty())
                        mostPopularPlanSubs.toFloat() /
                                subscriptions.size * 100f
                    else 0f

                // ─────────────────────────────────────────
                // Defaulter Rate
                // ─────────────────────────────────────────

                val defaulters =
                    payments.count { it.getString("status") == "overdue" }

                val defaulterRate =
                    if (payments.isNotEmpty())
                        defaulters.toFloat() /
                                payments.size * 100f
                    else 0f

                // ─────────────────────────────────────────
                // Technicians
                // ─────────────────────────────────────────

                val techSnap = db.collection("technicians")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                val activeTechnicians =
                    techSnap.documents.count {
                        it.getString("status") == "active"
                    }

                // ─────────────────────────────────────────
                // Recent Institutions
                // ─────────────────────────────────────────

                val recentInstitutions =
                    institutions.takeLast(5).map { doc ->
                        RecentInstitution(
                            name = doc.getString("name") ?: "",
                            plan = doc.getString("planName") ?: "",
                            status = doc.getString("status") ?: ""
                        )
                    }

                // ─────────────────────────────────────────
                // Recent Payments
                // ─────────────────────────────────────────

                val recentPayments =
                    payments.takeLast(5).map { doc ->
                        RecentPayment(
                            institution = doc.getString("institutionName") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            status = doc.getString("status") ?: ""
                        )
                    }

                // ─────────────────────────────────────────
                // Emit Success
                // ─────────────────────────────────────────

                _state.value =
                    AdminDashboardState.Success(
                        AdminDashboardData(
                            companyName = "",
                            totalInstitutions = totalInstitutions,
                            activeSubscriptions = activeSubscriptions,
                            pendingPayments = pendingPayments,
                            monthlyRevenue = monthlyRevenue,
                            suspendedInstitutions = suspendedInstitutions,
                            activeTechnicians = activeTechnicians,
                            collectionRate = collectionRate,
                            collected = collected,
                            outstanding = outstanding,
                            mostPopularPlan = mostPopularPlan,
                            mostPopularPlanSubs = mostPopularPlanSubs,
                            mostPopularPlanPercent = mostPopularPlanPercent,
                            defaulterRate = defaulterRate,
                            recentInstitutions = recentInstitutions,
                            recentPayments = recentPayments,
                            monthlyRevenueTrend = monthlyRevenueTrend
                        )
                    )

            } catch (e: Exception) {

                _state.value =
                    AdminDashboardState.Error(
                        e.message ?: "Failed to load dashboard"
                    )
            }
        }
    }
}