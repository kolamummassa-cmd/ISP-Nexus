package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Analytics Data ────────────────────────────────────────────────────────────

data class AnalyticsData(
    val totalCompanies: Int = 0,
    val activeCompanies: Int = 0,
    val pendingCompanies: Int = 0,
    val suspendedCompanies: Int = 0,
    val totalInstitutions: Int = 0,
    val totalSubscriptions: Int = 0,
    val growthRate: Float = 0f,
    val monthlyGrowth: List<MonthStat> = emptyList()
)

data class MonthStat(
    val month: String,
    val count: Int
)

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class AnalyticsState {
    object Loading : AnalyticsState()
    data class Success(val data: AnalyticsData) : AnalyticsState()
    data class Error(val message: String) : AnalyticsState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AnalyticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<AnalyticsState>(AnalyticsState.Loading)
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init { loadAnalytics() }

    fun loadAnalytics() {
        viewModelScope.launch {
            _state.value = AnalyticsState.Loading
            try {
                // Fetch all companies
                val companiesSnapshot = db.collection("companies").get().await()
                val allCompanies      = companiesSnapshot.documents

                val total      = allCompanies.size
                val active     = allCompanies.count { it.getString("status") == "approved" }
                val pending    = allCompanies.count { it.getString("status") == "pending" }
                val suspended  = allCompanies.count { it.getString("status") == "suspended" }

                // Fetch institutions (separate collection — adjust if needed)
                val institutionsSnapshot = db.collection("institutions").get().await()
                val totalInstitutions    = institutionsSnapshot.size()

                // Fetch subscriptions
                val subscriptionsSnapshot = db.collection("subscriptions").get().await()
                val totalSubscriptions    = subscriptionsSnapshot.size()

                // Build monthly growth from createdAt timestamps
                val monthlyMap = mutableMapOf<String, Int>()
                val months     = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

                allCompanies.forEach { doc ->
                    val createdAt = doc.get("createdAt")
                    val millis: Long? = when (createdAt) {
                        is Long -> createdAt
                        is com.google.firebase.Timestamp -> createdAt.toDate().time
                        else -> null
                    }
                    millis?.let {
                        val cal   = java.util.Calendar.getInstance()
                        cal.timeInMillis = it
                        val month = months[cal.get(java.util.Calendar.MONTH)]
                        monthlyMap[month] = (monthlyMap[month] ?: 0) + 1
                    }
                }

                // Build last 6 months in order
                val cal         = java.util.Calendar.getInstance()
                val last6Months = (5 downTo 0).map { offset ->
                    val c = java.util.Calendar.getInstance()
                    c.timeInMillis = cal.timeInMillis
                    c.add(java.util.Calendar.MONTH, -offset)
                    months[c.get(java.util.Calendar.MONTH)]
                }

                val monthlyGrowth = last6Months.map { month ->
                    MonthStat(month, monthlyMap[month] ?: 0)
                }

                // Growth rate: compare last month vs previous month
                val lastMonthCount = monthlyGrowth.getOrNull(5)?.count ?: 0
                val prevMonthCount = monthlyGrowth.getOrNull(4)?.count ?: 0
                val growthRate     = if (prevMonthCount > 0)
                    ((lastMonthCount - prevMonthCount).toFloat() / prevMonthCount) * 100f
                else if (lastMonthCount > 0) 100f else 0f

                _state.value = AnalyticsState.Success(
                    AnalyticsData(
                        totalCompanies     = total,
                        activeCompanies    = active,
                        pendingCompanies   = pending,
                        suspendedCompanies = suspended,
                        totalInstitutions  = totalInstitutions,
                        totalSubscriptions = totalSubscriptions,
                        growthRate         = growthRate,
                        monthlyGrowth      = monthlyGrowth
                    )
                )
            } catch (e: Exception) {
                _state.value = AnalyticsState.Error(e.message ?: "Failed to load analytics")
            }
        }
    }
}
