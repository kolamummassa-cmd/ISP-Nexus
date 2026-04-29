package com.example.ispnexus.data

import com.example.ispnexus.models.Company
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class CompanyRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Fetch pending ─────────────────────────────────────────────────────────

    suspend fun getPendingCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "pending")
                .get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch approved ────────────────────────────────────────────────────────

    suspend fun getApprovedCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "approved")
                .get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch rejected ────────────────────────────────────────────────────────

    suspend fun getRejectedCompanies(): Result<List<Company>> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("status", "rejected")
                .get().await()
            val companies = snapshot.documents.mapNotNull { doc ->
                doc.toObject<Company>()?.copy(adminUid = doc.id)
            }
            Result.success(companies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch all ─────────────────────────────────────────────────────────────

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
                .update("status", "approved").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    suspend fun rejectCompany(companyId: String): Result<Unit> {
        return try {
            db.collection("companies").document(companyId)
                .update("status", "rejected").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}