package com.example.ispnexus.viewmodels



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── Data Models ─────────────────────────────────────────────────────────────

data class PaymentHistoryItem(
    val id: String,
    val institutionName: String,
    val invoiceNumber: String,
    val amount: Double,
    val date: String,
    val status: String,        // "Paid" | "Pending"
    val paymentMethod: String  // "Bank Transfer" | "Mobile Money" | "" (if pending)
)

data class DefaulterItem(
    val rank: Int,
    val institutionName: String,
    val amountOwed: Double,
    val daysOverdue: Int
)

data class RevenuePoint(
    val month: String,
    val amount: Double
)

data class FinanceDashboardUiState(
    val officerName: String = "",
    val companyName: String = "",
    val companyLogoUrl: String = "",
    val notificationCount: Int = 0,

    // Stats — all zeroed out
    val totalRevenue: Double = 0.0,
    val totalRevenueChange: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val pendingInvoiceCount: Int = 0,
    val pendingPaymentsChange: Double = 0.0,
    val defaultersCount: Int = 0,
    val newDefaulters: Int = 0,
    val paidToday: Double = 0.0,
    val paidTodayCount: Int = 0,
    val paidTodayChange: Double = 0.0,

    val recentPayments: List<PaymentHistoryItem> = emptyList(),
    val revenuePoints: List<RevenuePoint> = emptyList(),
    val topDefaulters: List<DefaulterItem> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
//    val companyLogoUrl: String = ""
}

class FinanceDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceDashboardUiState())
    val uiState: StateFlow<FinanceDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val uid     = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val db      = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Fetch officer profile
                val userDoc     = db.collection("users").document(uid).get().await()
                val officerName = userDoc.getString("fullName") ?: "Finance Officer"
                val companyId   = userDoc.getString("companyId") ?: return@launch

                android.util.Log.d("FinanceVM", "Current UID = $uid")
                android.util.Log.d("FinanceVM", "fullName = ${userDoc.getString("fullName")}")
                android.util.Log.d("FinanceVM", "companyId = ${userDoc.getString("companyId")}")

                // Fetch company name
                val companyDoc  = db.collection("companies").document(companyId).get().await()
                val companyLogoUrl = companyDoc.getString("logoUrl") ?: ""
                val companyName = companyDoc.getString("companyName") ?: ""

                // Keep sample data for now — replace with real Firestore calls later
                // ── Recent Payments (last 5) ──────────────────────────────────────
                val paymentsSnapshot = db.collection("payments")
                    .whereEqualTo("companyId", companyId)
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                val payments = paymentsSnapshot.documents.map { doc ->
                    PaymentHistoryItem(
                        id              = doc.id,
                        institutionName = doc.getString("institutionName") ?: "",
                        invoiceNumber   = doc.getString("invoiceNumber")   ?: "",
                        amount          = doc.getDouble("amount")          ?: 0.0,
                        date            = doc.getString("date")            ?: "",
                        status          = doc.getString("status")          ?: "Pending",
                        paymentMethod   = doc.getString("paymentMethod")   ?: ""
                    )
                }

// ── All Payments for stats ────────────────────────────────────────
                val allPaymentsSnapshot = db.collection("payments")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                val allDocs       = allPaymentsSnapshot.documents
                val totalRevenue  = allDocs.filter   { it.getString("status") == "Paid" }
                    .sumOf    { it.getDouble("amount") ?: 0.0 }
                val pendingAmount = allDocs.filter   { it.getString("status") == "Pending" }
                    .sumOf    { it.getDouble("amount") ?: 0.0 }
                val pendingCount  = allDocs.count    { it.getString("status") == "Pending" }

// ── Paid Today ────────────────────────────────────────────────────
                val todayStr       = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val paidTodayDocs  = allDocs.filter {
                    it.getString("status") == "Paid" &&
                            it.getString("date")   == todayStr
                }
                val paidTodayAmt   = paidTodayDocs.sumOf { it.getDouble("amount") ?: 0.0 }
                val paidTodayCount = paidTodayDocs.size

// ── Top Defaulters ────────────────────────────────────────────────
                val defaultersSnapshot = db.collection("payments")
                    .whereEqualTo("companyId", companyId)
                    .whereEqualTo("status", "Overdue")
                    .orderBy("amount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()

                val defaulters = defaultersSnapshot.documents.mapIndexed { index, doc ->
                    DefaulterItem(
                        rank            = index + 1,
                        institutionName = doc.getString("institutionName") ?: "",
                        amountOwed      = doc.getDouble("amount")          ?: 0.0,
                        daysOverdue     = doc.getLong("daysOverdue")?.toInt() ?: 0
                    )
                }

// ── Revenue Chart — group Paid payments by month ──────────────────
                val monthlyMap = mutableMapOf<String, Double>()
                val monthFmt   = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                val dateFmt    = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

                allDocs.filter { it.getString("status") == "Paid" }.forEach { doc ->
                    try {
                        val date  = dateFmt.parse(doc.getString("date") ?: "") ?: return@forEach
                        val month = monthFmt.format(date)
                        monthlyMap[month] = (monthlyMap[month] ?: 0.0) + (doc.getDouble("amount") ?: 0.0)
                    } catch (e: Exception) { /* skip malformed dates */ }
                }

                val revenuePoints = monthlyMap.map { RevenuePoint(it.key, it.value) }
                _uiState.value = _uiState.value.copy(
                    isLoading           = false,
                    officerName         = officerName,
                    companyName         = companyName,
                    companyLogoUrl = companyLogoUrl,
                    totalRevenue        = totalRevenue,
                    pendingPayments     = pendingAmount,
                    pendingInvoiceCount = pendingCount,
                    paidToday           = paidTodayAmt,
                    paidTodayCount      = paidTodayCount,
                    defaultersCount     = defaultersSnapshot.size(),
                    recentPayments      = payments,
                    revenuePoints       = revenuePoints,
                    topDefaulters       = defaulters
                )
            }  catch (e: Exception) {
            android.util.Log.e("FinanceVM", "Error: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}