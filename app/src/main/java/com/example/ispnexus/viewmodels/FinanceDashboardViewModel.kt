package com.example.ispnexus.viewmodels



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    val officerName: String = "Finance Officer",
    val companyName: String = "",
    val notificationCount: Int = 3,

    // Stats
    val totalRevenue: Double = 28450.00,
    val totalRevenueChange: Double = 12.5,
    val pendingPayments: Double = 6320.00,
    val pendingInvoiceCount: Int = 12,
    val pendingPaymentsChange: Double = 8.3,
    val defaultersCount: Int = 8,
    val newDefaulters: Int = 3,
    val paidToday: Double = 2150.00,
    val paidTodayCount: Int = 5,
    val paidTodayChange: Double = 15.2,

    // Payment History
    val recentPayments: List<PaymentHistoryItem> = emptyList(),

    // Revenue Chart
    val revenuePoints: List<RevenuePoint> = emptyList(),

    // Defaulters
    val topDefaulters: List<DefaulterItem> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

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

                // Fetch company name
                val companyDoc  = db.collection("companies").document(companyId).get().await()
                val companyName = companyDoc.getString("companyName") ?: ""

                // Keep sample data for now — replace with real Firestore calls later
                val payments = listOf(
                    PaymentHistoryItem("1", "Greenfield School",   "INV-2024-128", 1200.00, "24 May 2024", "Paid",    "Bank Transfer"),
                    PaymentHistoryItem("2", "Sunrise Academy",     "INV-2024-127",  950.00, "24 May 2024", "Pending", ""),
                    PaymentHistoryItem("3", "City College",        "INV-2024-126", 1500.00, "23 May 2024", "Paid",    "Mobile Money"),
                    PaymentHistoryItem("4", "Blue Valley School",  "INV-2024-125",  800.00, "23 May 2024", "Pending", ""),
                    PaymentHistoryItem("5", "Bright Future Inst.", "INV-2024-124", 1100.00, "22 May 2024", "Paid",    "Bank Transfer"),
                )

                val revenuePoints = listOf(
                    RevenuePoint("Jan", 10000.0),
                    RevenuePoint("Feb", 18000.0),
                    RevenuePoint("Mar", 20000.0),
                    RevenuePoint("Apr", 26000.0),
                    RevenuePoint("May", 38000.0),
                    RevenuePoint("Jun", 22000.0),
                )

                val defaulters = listOf(
                    DefaulterItem(1, "Greenfield School", 1200.00, 45),
                    DefaulterItem(2, "City College",       950.00, 30),
                    DefaulterItem(3, "Sunrise Academy",    750.00, 25),
                )

                _uiState.value = _uiState.value.copy(
                    isLoading      = false,
                    officerName    = officerName,
                    companyName    = companyName,    // ← add this
                    recentPayments = payments,
                    revenuePoints  = revenuePoints,
                    topDefaulters  = defaulters
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}