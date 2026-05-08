package com.example.ispnexus.data

import com.example.ispnexus.models.Institution
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InstitutionsRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Real-time listener for all institutions under a company ───────────────
    fun observeInstitutions(companyId: String): Flow<List<Institution>> = callbackFlow {
        val listener = db.collection("institutions")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val institutions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Institution>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(institutions)
            }
        awaitClose { listener.remove() }
    }

    // ── Real-time listener filtered by status ─────────────────────────────────
    fun observeInstitutionsByStatus(companyId: String, status: String): Flow<List<Institution>> = callbackFlow {
        val listener = db.collection("institutions")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val institutions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Institution>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(institutions)
            }
        awaitClose { listener.remove() }
    }

    // ── One-time fetch ────────────────────────────────────────────────────────
    suspend fun getInstitutions(companyId: String): Result<List<Institution>> {
        return try {
            val snapshot = db.collection("institutions")
                .whereEqualTo("companyId", companyId)
                .get().await()
            val institutions = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Institution>()?.copy(id = doc.id)
            }
            Result.success(institutions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Add institution ───────────────────────────────────────────────────────
    suspend fun addInstitution(institution: Institution): Result<Unit> {
        return try {
            val data = hashMapOf(
                "name"                to institution.name.trim(),
                "email"               to institution.email.trim(),
                "phoneNumber"         to institution.phoneNumber.trim(),
                "address"             to institution.address.trim(),
                "contactPersonName"   to institution.contactPersonName.trim(),
                "contactPersonPhone"  to institution.contactPersonPhone.trim(),
                "companyId"           to institution.companyId,
                "planId"              to institution.planId,
                "planName"            to institution.planName,
                "status"              to institution.status,
                "createdAt"           to System.currentTimeMillis()
            )
            db.collection("institutions").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update institution ────────────────────────────────────────────────────
    suspend fun updateInstitution(institution: Institution): Result<Unit> {
        return try {
            val data = hashMapOf(
                "name"                to institution.name.trim(),
                "email"               to institution.email.trim(),
                "phoneNumber"         to institution.phoneNumber.trim(),
                "address"             to institution.address.trim(),
                "contactPersonName"   to institution.contactPersonName.trim(),
                "contactPersonPhone"  to institution.contactPersonPhone.trim(),
                "planId"              to institution.planId,
                "planName"            to institution.planName,
                "status"              to institution.status
            )
            db.collection("institutions").document(institution.id)
                .update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Suspend institution ───────────────────────────────────────────────────
    suspend fun suspendInstitution(institutionId: String): Result<Unit> {
        return try {
            db.collection("institutions").document(institutionId)
                .update("status", "suspended").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reactivate institution ────────────────────────────────────────────────
    suspend fun reactivateInstitution(institutionId: String): Result<Unit> {
        return try {
            db.collection("institutions").document(institutionId)
                .update("status", "active").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delete institution ────────────────────────────────────────────────────
    suspend fun deleteInstitution(institutionId: String): Result<Unit> {
        return try {
            db.collection("institutions").document(institutionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}