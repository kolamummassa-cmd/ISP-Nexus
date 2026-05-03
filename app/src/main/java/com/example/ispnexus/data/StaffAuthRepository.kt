package com.example.ispnexus.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StaffAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Verify company code and return companyId ───────────────────────────────
    suspend fun verifyCompanyCode(companyCode: String): Result<String> {
        return try {
            val snapshot = db.collection("companies")
                .whereEqualTo("companyCode", companyCode.trim())
                .whereEqualTo("status", "Approved")  // only approved companies
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.failure(Exception("Invalid company code. Please check and try again."))
            } else {
                val companyId = snapshot.documents.first().id
                Result.success(companyId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Staff self-registration ────────────────────────────────────────────────
    suspend fun registerStaff(
        fullName: String,
        email: String,
        password: String,
        companyCode: String,
        position: String
    ): Result<Unit> {
        return try {
            // Step 1: Verify company code
            val companyResult = verifyCompanyCode(companyCode)
            if (companyResult.isFailure) {
                return Result.failure(companyResult.exceptionOrNull()!!)
            }
            val companyId = companyResult.getOrNull()!!

            // Step 2: Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Failed to create account"))

            // Step 3: Save staff profile to Firestore users collection
            val staffData = hashMapOf(
                "fullName"  to fullName.trim(),
                "email"     to email.trim(),
                "role"      to "staff",
                "companyId" to companyId,
                "position"  to position.trim(),
                "status"    to "pending",           // ← pending until admin approves
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users").document(uid).set(staffData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Get staff profile after login ─────────────────────────────────────────
    suspend fun getStaffProfile(uid: String): Result<Map<String, Any?>> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                Result.failure(Exception("Profile not found"))
            } else {
                Result.success(doc.data ?: emptyMap())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<Map<String, Any?>> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Login failed"))

            getStaffProfile(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUid() = auth.currentUser?.uid
}