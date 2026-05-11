package com.example.ispnexus.data

import com.example.ispnexus.models.Subscription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubscriptionsRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val companyId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun observeSubscriptions(companyId: String): Flow<List<Subscription>> = callbackFlow {
        val listener = db.collection("subscriptions")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Subscription>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by status ─────────────────────────────────
    fun observeSubscriptionsByStatus(companyId: String, status: String): Flow<List<Subscription>> = callbackFlow {
        val listener = db.collection("subscriptions")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Subscription>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by institution ─────────────────────────────
    fun observeSubscriptionsByInstitution(institutionId: String): Flow<List<Subscription>> = callbackFlow {
        val listener = db.collection("subscriptions")
            .whereEqualTo("institutionId", institutionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Subscription>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── One-time fetch ────────────────────────────────────────────────────────
    suspend fun getSubscriptions(companyId: String): Result<List<Subscription>> {
        return try {
            val snapshot = db.collection("subscriptions")
                .whereEqualTo("companyId", companyId)
                .get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Subscription>()?.copy(id = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Add subscription ──────────────────────────────────────────────────────
    suspend fun addSubscription(subscription: Subscription): Result<Unit> {
        return try {
            val data = hashMapOf(
                "companyId"       to subscription.companyId,
                "institutionId"   to subscription.institutionId,
                "institutionName" to subscription.institutionName.trim(),
                "planId"          to subscription.planId,
                "planName"        to subscription.planName.trim(),
                "amountKsh"       to subscription.amountKsh,
                "billingCycle"    to subscription.billingCycle,
                "status"          to subscription.status,
                "startDate"       to subscription.startDate,
                "endDate"         to subscription.endDate,
                "createdAt"       to System.currentTimeMillis()
            )
            db.collection("subscriptions").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update subscription ───────────────────────────────────────────────────
    suspend fun updateSubscription(subscription: Subscription): Result<Unit> {
        return try {
            val data = hashMapOf(
                "institutionId"   to subscription.institutionId,
                "institutionName" to subscription.institutionName.trim(),
                "planId"          to subscription.planId,
                "planName"        to subscription.planName.trim(),
                "amountKsh"       to subscription.amountKsh,
                "billingCycle"    to subscription.billingCycle,
                "status"          to subscription.status,
                "startDate"       to subscription.startDate,
                "endDate"         to subscription.endDate
            )
            db.collection("subscriptions").document(subscription.id)
                .update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Suspend subscription ──────────────────────────────────────────────────
    suspend fun suspendSubscription(subscriptionId: String): Result<Unit> {
        return try {
            db.collection("subscriptions").document(subscriptionId)
                .update("status", "suspended").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reactivate subscription ───────────────────────────────────────────────
    suspend fun reactivateSubscription(subscriptionId: String): Result<Unit> {
        return try {
            // ── Guard: verify completed payment exists ────────────────────────
            val paymentsSnapshot = db.collection("payments")
                .whereEqualTo("subscriptionId", subscriptionId)
                .whereEqualTo("status", "completed")
                .get()
                .await()

            if (paymentsSnapshot.isEmpty) {
                return Result.failure(
                    Exception("Cannot reactivate — no completed payment found. Record a payment first.")
                )
            }

            // ── Safe to reactivate ────────────────────────────────────────────
            db.collection("subscriptions").document(subscriptionId)
                .update("status", "active").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark as expired ───────────────────────────────────────────────────────
    suspend fun expireSubscription(subscriptionId: String): Result<Unit> {
        return try {
            db.collection("subscriptions").document(subscriptionId)
                .update("status", "expired").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delete subscription ───────────────────────────────────────────────────
    suspend fun deleteSubscription(subscriptionId: String): Result<Unit> {
        return try {
            db.collection("subscriptions").document(subscriptionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Renew subscription (extend endDate + create payment record) ───────────────
    suspend fun renewSubscription(subscription: Subscription, companyId: String): Result<Unit> {
        return try {
            val now   = System.currentTimeMillis()
            val batch = db.batch()

            // Calculate new endDate based on billing cycle
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = if (subscription.endDate > now) subscription.endDate else now
            when (subscription.billingCycle) {
                "yearly"  -> cal.add(java.util.Calendar.YEAR, 1)
                else      -> cal.add(java.util.Calendar.MONTH, 1)
            }
            val newEndDate = cal.timeInMillis

            // Update subscription
            val subRef = db.collection("subscriptions").document(subscription.id)
            batch.update(subRef, mapOf(
                "status"    to "active",
                "startDate" to now,
                "endDate"   to newEndDate
            ))

            // Auto-create payment record
            val paymentRef  = db.collection("payments").document()
            val invoiceRef  = db.collection("invoices").document()
            val invoiceNum  = "INV-${java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(now))}-${invoiceRef.id.take(4).uppercase()}"

            batch.set(paymentRef, hashMapOf(
                "companyId"       to companyId,
                "institutionId"   to subscription.institutionId,
                "institutionName" to subscription.institutionName,
                "subscriptionId"  to subscription.id,
                "invoiceId"       to invoiceRef.id,
                "amountKsh"       to subscription.amountKsh,
                "paymentMethod"   to "",
                "status"          to "pending",
                "notes"           to "Renewal payment",
                "paidAt"          to 0L,
                "createdAt"       to now
            ))

            batch.set(invoiceRef, hashMapOf(
                "invoiceNumber"   to invoiceNum,
                "companyId"       to companyId,
                "institutionId"   to subscription.institutionId,
                "institutionName" to subscription.institutionName,
                "subscriptionId"  to subscription.id,
                "paymentId"       to paymentRef.id,
                "amountKsh"       to subscription.amountKsh,
                "paymentMethod"   to "",
                "status"          to "pending",
                "notes"           to "Renewal invoice",
                "issuedAt"        to now,
                "paidAt"          to 0L,
                "createdAt"       to now
            ))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}