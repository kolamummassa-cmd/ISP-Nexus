package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── Models ───────────────────────────────────────────────────────────────────

data class InstitutionDropdownItem(
    val id   : String = "",
    val name : String = ""
)

data class RecordPaymentUiState(
    val institutions    : List<InstitutionDropdownItem> = emptyList(),
    val isLoadingInstitutions : Boolean = true,
    val isSaving        : Boolean = false,
    val successMessage  : String? = null,
    val errorMessage    : String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class RecordPaymentViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    private var companyId: String = ""

    init {
        loadInstitutions()
    }

    // ─── Load institutions for dropdown ───────────────────────────────────────

    private fun loadInstitutions() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                    ?: return@launch emitError("Not logged in")

                val userDoc   = db.collection("users").document(uid).get().await()
                companyId     = userDoc.getString("companyId")
                    ?: return@launch emitError("Company not found")

                val snapshot  = db.collection("institutions")
                    .whereEqualTo("companyId", companyId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    InstitutionDropdownItem(id = doc.id, name = name)
                }

                _uiState.value = _uiState.value.copy(
                    institutions          = list,
                    isLoadingInstitutions = false
                )

            } catch (e: Exception) {
                emitError(e.message ?: "Failed to load institutions")
            }
        }
    }

    // ─── Record Payment ───────────────────────────────────────────────────────

    fun recordPayment(
        institution   : InstitutionDropdownItem,
        amount        : Double,
        paymentMethod : String,   // "M-Pesa" | "Bank Transfer" | "Cash"
        referenceNumber: String,  // transaction ref / slip number
        periodMonth   : String,   // e.g. "May 2026"
        notes         : String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val invoiceNumber = generateInvoiceNumber()
                val dateStr       = java.text.SimpleDateFormat(
                    "dd MMM yyyy", java.util.Locale.getDefault()
                ).format(java.util.Date())

                // ── Write Payment doc ─────────────────────────────────────────
                val paymentData = hashMapOf(
                    "companyId"       to companyId,
                    "institutionId"   to institution.id,
                    "institutionName" to institution.name,
                    "amount"          to amount,
                    "paymentMethod"   to paymentMethod,
                    "referenceNumber" to referenceNumber,
                    "invoiceNumber"   to invoiceNumber,
                    "periodMonth"     to periodMonth,
                    "notes"           to notes,
                    "status"          to "Paid",
                    "date"            to dateStr,
                    "createdAt"       to System.currentTimeMillis()
                )

                val paymentRef = db.collection("payments").add(paymentData).await()

                // ── Write Invoice doc (auto-generated) ────────────────────────
                val invoiceData = hashMapOf(
                    "companyId"       to companyId,
                    "institutionId"   to institution.id,
                    "institutionName" to institution.name,
                    "invoiceNumber"   to invoiceNumber,
                    "paymentId"       to paymentRef.id,
                    "amount"          to amount,
                    "paymentMethod"   to paymentMethod,
                    "periodMonth"     to periodMonth,
                    "status"          to "Paid",
                    "issuedDate"      to dateStr,
                    "createdAt"       to System.currentTimeMillis()
                )

                db.collection("invoices").add(invoiceData).await()

                _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    successMessage = "Payment of KES ${
                        String.format("%,.0f", amount)
                    } recorded. Invoice $invoiceNumber generated."
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving      = false,
                    errorMessage  = e.message ?: "Failed to record payment"
                )
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun generateInvoiceNumber(): String {
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        val random    = (100..999).random()
        return "INV-$timestamp-$random"
    }

    private fun emitError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoadingInstitutions = false,
            isSaving              = false,
            errorMessage          = message
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage   = null
        )
    }
}
