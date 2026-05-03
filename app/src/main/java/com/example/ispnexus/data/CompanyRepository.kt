package com.example.ispnexus.data

import com.example.ispnexus.models.Company
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CompanyRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time listener for pending companies ───────────────────────────────
    // Updates automatically whenever Firestore changes — no need to rerun app
    fun observePendingCompanies(): Flow<List<Company>> = callbackFlow {
        val listener = db.collection("companies")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val companies = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Company>()?.copy(adminUid = doc.id)
                } ?: emptyList()
                trySend(companies)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener for approved companies ──────────────────────────────
    fun observeApprovedCompanies(): Flow<List<Company>> = callbackFlow {
        val listener = db.collection("companies")
            .whereEqualTo("status", "Approved")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val companies = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Company>()?.copy(adminUid = doc.id)
                } ?: emptyList()
                trySend(companies)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener for rejected companies ──────────────────────────────
    fun observeRejectedCompanies(): Flow<List<Company>> = callbackFlow {
        val listener = db.collection("companies")
            .whereEqualTo("status", "Rejected")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val companies = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Company>()?.copy(adminUid = doc.id)
                } ?: emptyList()
                trySend(companies)
            }
        awaitClose { listener.remove() }
    }

    // ── One-time fetch (kept for approve/reject actions) ──────────────────────

    suspend fun getPendingCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "Pending").get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApprovedCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "Approved").get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRejectedCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "Rejected").get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies").get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Approve ───────────────────────────────────────────────────────────────
    suspend fun approveCompany(companyId: String): Result<Unit> {
        return try {
            db.collection("companies").document(companyId)
                .update("status", "Approved").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reject ────────────────────────────────────────────────────────────────
    suspend fun rejectCompany(companyId: String): Result<Unit> {
        return try {
            db.collection("companies").document(companyId).delete().await()

            db.collection("users").document(companyId).delete().await()
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}