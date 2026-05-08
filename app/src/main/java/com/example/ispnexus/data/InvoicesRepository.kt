package com.example.ispnexus.data

import com.example.ispnexus.models.Invoice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InvoicesRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time listener for all invoices under a company ───────────────────
    fun observeInvoices(companyId: String): Flow<List<Invoice>> = callbackFlow {
        val listener = db.collection("invoices")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Invoice>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by status ─────────────────────────────────
    fun observeInvoicesByStatus(companyId: String, status: String): Flow<List<Invoice>> = callbackFlow {
        val listener = db.collection("invoices")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Invoice>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by institution ────────────────────────────
    fun observeInvoicesByInstitution(institutionId: String): Flow<List<Invoice>> = callbackFlow {
        val listener = db.collection("invoices")
            .whereEqualTo("institutionId", institutionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Invoice>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── One-time fetch ────────────────────────────────────────────────────────
    suspend fun getInvoices(companyId: String): Result<List<Invoice>> {
        return try {
            val snapshot = db.collection("invoices")
                .whereEqualTo("companyId", companyId)
                .get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Invoice>()?.copy(id = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── One-time fetch single invoice ─────────────────────────────────────────
    suspend fun getInvoiceById(invoiceId: String): Result<Invoice> {
        return try {
            val doc = db.collection("invoices").document(invoiceId).get().await()
            val invoice = doc.toObject<Invoice>()?.copy(id = doc.id)
                ?: return Result.failure(Exception("Invoice not found"))
            Result.success(invoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update invoice notes (only editable field — status mirrors payment) ───
    suspend fun updateInvoiceNotes(invoiceId: String, notes: String): Result<Unit> {
        return try {
            db.collection("invoices").document(invoiceId)
                .update("notes", notes.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Full update (called internally when payment is edited) ────────────────
    suspend fun updateInvoice(invoice: Invoice): Result<Unit> {
        return try {
            val data = hashMapOf(
                "institutionId"   to invoice.institutionId,
                "institutionName" to invoice.institutionName.trim(),
                "amountKsh"       to invoice.amountKsh,
                "paymentMethod"   to invoice.paymentMethod,
                "status"          to invoice.status,
                "notes"           to invoice.notes.trim(),
                "paidAt"          to invoice.paidAt
            )
            db.collection("invoices").document(invoice.id)
                .update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}