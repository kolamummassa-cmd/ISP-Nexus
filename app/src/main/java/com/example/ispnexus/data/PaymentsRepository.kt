package com.example.ispnexus.data

import com.example.ispnexus.models.Invoice
import com.example.ispnexus.models.Payment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PaymentsRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time listener for all payments under a company ───────────────────
    fun observePayments(companyId: String): Flow<List<Payment>> = callbackFlow {
        val listener = db.collection("payments")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Payment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by status ─────────────────────────────────
    fun observePaymentsByStatus(companyId: String, status: String): Flow<List<Payment>> = callbackFlow {
        val listener = db.collection("payments")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Payment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by institution ────────────────────────────
    fun observePaymentsByInstitution(institutionId: String): Flow<List<Payment>> = callbackFlow {
        val listener = db.collection("payments")
            .whereEqualTo("institutionId", institutionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Payment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by subscription ───────────────────────────
    fun observePaymentsBySubscription(subscriptionId: String): Flow<List<Payment>> = callbackFlow {
        val listener = db.collection("payments")
            .whereEqualTo("subscriptionId", subscriptionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Payment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── One-time fetch ────────────────────────────────────────────────────────
    suspend fun getPayments(companyId: String): Result<List<Payment>> {
        return try {
            val snapshot = db.collection("payments")
                .whereEqualTo("companyId", companyId)
                .get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Payment>()?.copy(id = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Add payment + auto-create invoice (atomic batch) ─────────────────────
    suspend fun addPaymentWithInvoice(payment: Payment): Result<Unit> {
        return try {
            val batch       = db.batch()
            val paymentRef  = db.collection("payments").document()
            val invoiceRef  = db.collection("invoices").document()
            val now         = System.currentTimeMillis()

            // Build invoice number e.g. INV-2024-001
            val invoiceNumber = "INV-${
                java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(now))
            }-${invoiceRef.id.take(4).uppercase()}"

            val status = null
            val invoiceData = hashMapOf(
                "invoiceNumber"   to invoiceNumber,
                "companyId"       to payment.companyId,
                "institutionId"   to payment.institutionId,
                "institutionName" to payment.institutionName,
                "subscriptionId"  to payment.subscriptionId,
                "paymentId"       to paymentRef.id,
                "amountKsh"       to payment.amountKsh,
                "paymentMethod"   to payment.paymentMethod,
                "status"          to if (payment.status == "completed") "paid" else "pending",
                "issuedAt"        to now,
                "dueDate" to when (payment.billingCycle) {
                    "yearly" -> now + (365L * 24 * 60 * 60 * 1000)
                    else     -> now + (30L  * 24 * 60 * 60 * 1000)
                },
                "paidAt" to if (payment.status == "completed") payment.paidAt else 0L,
                "notes"           to payment.notes,
                "createdAt"       to now
            )

            val paymentData = hashMapOf(
                "companyId"       to payment.companyId,
                "institutionId"   to payment.institutionId,
                "institutionName" to payment.institutionName.trim(),
                "subscriptionId"  to payment.subscriptionId,
                "invoiceId"       to invoiceRef.id,
                "amountKsh"       to payment.amountKsh,
                "paymentMethod"   to payment.paymentMethod,
                "status"          to payment.status,
                "notes"           to payment.notes.trim(),
                "paidAt"          to payment.paidAt,
                "createdAt"       to now
            )

            batch.set(paymentRef, paymentData)
            batch.set(invoiceRef, invoiceData)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update payment ────────────────────────────────────────────────────────
    suspend fun updatePayment(payment: Payment): Result<Unit> {
        return try {
            val data = hashMapOf(
                "institutionId"   to payment.institutionId,
                "institutionName" to payment.institutionName.trim(),
                "subscriptionId"  to payment.subscriptionId,
                "amountKsh"       to payment.amountKsh,
                "paymentMethod"   to payment.paymentMethod,
                "status"          to payment.status,
                "notes"           to payment.notes.trim(),
                "paidAt"          to payment.paidAt
            )
            // Keep invoice status in sync
            if (payment.invoiceId.isNotEmpty()) {
                val batch      = db.batch()
                val paymentRef = db.collection("payments").document(payment.id)
                val invoiceRef = db.collection("invoices").document(payment.invoiceId)
                batch.update(paymentRef, data as Map<String, Any>)
                batch.update(
                    invoiceRef, mapOf(
                        "status"        to if (payment.status == "completed") "paid" else "pending",
                        "paymentMethod" to payment.paymentMethod,
                        "amountKsh"     to payment.amountKsh,
                        "paidAt"        to payment.paidAt
                    )
                )
                batch.commit().await()
            } else {
                db.collection("payments").document(payment.id)
                    .update(data as Map<String, Any>).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark as completed ─────────────────────────────────────────────────────
    suspend fun markCompleted(payment: Payment): Result<Unit> {
        return try {
            val now   = System.currentTimeMillis()
            val batch = db.batch()
            batch.update(
                db.collection("payments").document(payment.id),
                mapOf("status" to "completed", "paidAt" to now)
            )
            if (payment.invoiceId.isNotEmpty()) {
                batch.update(
                    db.collection("invoices").document(payment.invoiceId),
                    mapOf("status" to "paid", "paidAt" to now)
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark as failed ────────────────────────────────────────────────────────
    suspend fun markFailed(paymentId: String, invoiceId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("payments").document(paymentId),
                mapOf("status" to "failed")
            )
            if (invoiceId.isNotEmpty()) {
                batch.update(
                    db.collection("invoices").document(invoiceId),
                    mapOf("status" to "failed")
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delete payment ────────────────────────────────────────────────────────
    suspend fun deletePayment(paymentId: String): Result<Unit> {
        return try {
            db.collection("payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}