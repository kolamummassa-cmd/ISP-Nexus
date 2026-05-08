package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ispnexus.data.ReportsRepository
import com.example.ispnexus.models.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ── Computed data classes (not stored in Firestore) ───────────────────────────

data class InstitutionRevenue(
    val institutionName: String,
    val totalPaid: Double,
    val paymentCount: Int
)

data class MonthlyData(
    val monthLabel: String,   // "Jan 2024"
    val monthKey: String,     // "2024-01" for sorting
    val collected: Double,
    val outstanding: Double
)

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class ReportsUiState {
    object Loading : ReportsUiState()
    data class Success(val payments: List<Payment>) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}

sealed class ReportExportState {
    object Idle : ReportExportState()
    object Loading : ReportExportState()
    data class Success(val filePath: String) : ReportExportState()
    data class Error(val message: String) : ReportExportState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ReportsViewModel : ViewModel() {

    private val repository = ReportsRepository()
    private val db         = FirebaseFirestore.getInstance()
    private val auth       = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private val _exportState = MutableStateFlow<ReportExportState>(ReportExportState.Idle)
    val exportState: StateFlow<ReportExportState> = _exportState.asStateFlow()

    init { loadReports() }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadReports() {
        viewModelScope.launch {
            _state.value = ReportsUiState.Loading
            try {
                val uid = auth.currentUser?.uid ?: run {
                    _state.value = ReportsUiState.Error("Not authenticated")
                    return@launch
                }
                val userDoc   = db.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: run {
                    _state.value = ReportsUiState.Error("Company not found")
                    return@launch
                }
                repository.observePayments(companyId)
                    .catch { e -> _state.value = ReportsUiState.Error(e.message ?: "Unknown error") }
                    .collect { list ->
                        _state.value = ReportsUiState.Success(
                            list.sortedByDescending { (it.createdAt as? Long) ?: 0L }
                        )
                    }
            } catch (e: Exception) {
                _state.value = ReportsUiState.Error(e.message ?: "Failed to load report data")
            }
        }
    }

    // ── Revenue summary ───────────────────────────────────────────────────────
    fun totalCollected(payments: List<Payment>): Double =
        payments.filter { it.status == "completed" }.sumOf { it.amountKsh }

    fun totalOutstanding(payments: List<Payment>): Double =
        payments.filter { it.status == "pending" }.sumOf { it.amountKsh }

    fun totalFailed(payments: List<Payment>): Double =
        payments.filter { it.status == "failed" }.sumOf { it.amountKsh }

    fun countByStatus(payments: List<Payment>, status: String): Int =
        payments.count { it.status == status }

    // ── Payment method breakdown ──────────────────────────────────────────────
    fun methodBreakdown(payments: List<Payment>): List<Triple<String, Double, Int>> {
        val methods = listOf("Bank Transfer", "Mobile Money", "Cash")
        return methods.map { method ->
            val completed = payments.filter {
                it.paymentMethod == method && it.status == "completed"
            }
            Triple(method, completed.sumOf { it.amountKsh }, completed.size)
        }
    }

    // ── Top paying institutions (by completed amount, top 5) ─────────────────
    fun topInstitutions(payments: List<Payment>): List<InstitutionRevenue> {
        return payments
            .filter { it.status == "completed" }
            .groupBy { it.institutionName }
            .map { (name, list) ->
                InstitutionRevenue(
                    institutionName = name,
                    totalPaid       = list.sumOf { it.amountKsh },
                    paymentCount    = list.size
                )
            }
            .sortedByDescending { it.totalPaid }
            .take(5)
    }


    fun monthlyTrend(payments: List<Payment>): List<MonthlyData> {
        val monthFmt  = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val keyFmt    = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal       = Calendar.getInstance()


        val months = (5 downTo 0).map { offset ->
            cal.time = Date()
            cal.add(Calendar.MONTH, -offset)
            val date = cal.time
            Pair(keyFmt.format(date), monthFmt.format(date))
        }

        return months.map { (key, label) ->
            val monthPayments = payments.filter { p ->
                keyFmt.format(Date((p.createdAt as? Long) ?: 0L)) == key            }
            MonthlyData(
                monthLabel  = label,
                monthKey    = key,
                collected   = monthPayments.filter { it.status == "completed" }.sumOf { it.amountKsh },
                outstanding = monthPayments.filter { it.status == "pending" }.sumOf { it.amountKsh }
            )
        }
    }

    // ── Export state reset ────────────────────────────────────────────────────
    fun resetExportState() { _exportState.value = ReportExportState.Idle }

    fun setExportLoading() { _exportState.value = ReportExportState.Loading }

    fun setExportSuccess(path: String) { _exportState.value = ReportExportState.Success(path) }

    fun setExportError(msg: String) { _exportState.value = ReportExportState.Error(msg) }
}