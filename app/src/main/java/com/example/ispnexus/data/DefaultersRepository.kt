package com.example.ispnexus.data

import com.example.ispnexus.models.Institution
import com.example.ispnexus.models.Subscription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class DefaultersRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time institutions stream ─────────────────────────────────────────
    fun observeInstitutions(companyId: String): Flow<List<Institution>> = callbackFlow {
        val listener = db.collection("institutions")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Institution>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time subscriptions stream ────────────────────────────────────────
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

    // ── Real-time payments stream (for last payment date) ─────────────────────
    fun observePayments(companyId: String): Flow<List<com.example.ispnexus.models.Payment>> = callbackFlow {
        val listener = db.collection("payments")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<com.example.ispnexus.models.Payment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Add reminder note on subscription ─────────────────────────────────────
    suspend fun addReminderNote(subscriptionId: String, note: String): Result<Unit> {
        return try {
            val data = mapOf(
                "lastReminderNote" to note.trim(),
                "lastReminderAt"   to System.currentTimeMillis()
            )
            db.collection("subscriptions").document(subscriptionId)
                .update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark subscription as resolved (reactivate) ────────────────────────────
    suspend fun markResolved(subscriptionId: String): Result<Unit> {
        return try {
            db.collection("subscriptions").document(subscriptionId)
                .update("status", "active").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}