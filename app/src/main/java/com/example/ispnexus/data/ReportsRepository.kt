package com.example.ispnexus.data

import com.example.ispnexus.models.Payment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReportsRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time payments stream ─────────────────────────────────────────────
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

    // ── One-time fetch (used for PDF export) ──────────────────────────────────
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
}