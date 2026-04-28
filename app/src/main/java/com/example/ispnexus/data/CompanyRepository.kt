package com.example.ispnexus.data

import com.example.ispnexus.models.Company
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class CompanyRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun getPendingCompanies(): Result<List<Company>> = try {
        val snapshot = db.collection("companies")
            .whereEqualTo("status", "PENDING")
            .get().await()
        val companies = snapshot.documents.mapNotNull { doc ->
            doc.toObject<Company>()?.copy(id = doc.id, adminUid = doc.id)
        }
        Result.success(companies)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getApprovedCompanies(): Result<List<Company>> = try {
        val snapshot = db.collection("companies")
            .whereEqualTo("status", "APPROVED")
            .get().await()
        val companies = snapshot.documents.mapNotNull { doc ->
            doc.toObject<Company>()?.copy(id = doc.id, adminUid = doc.id)
        }
        Result.success(companies)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun approveCompany(companyId: String): Result<Unit> = try {
        db.collection("companies").document(companyId)
            .update("status", "APPROVED").await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun rejectCompany(companyId: String): Result<Unit> = try {
        db.collection("companies").document(companyId)
            .update("status", "REJECTED").await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}