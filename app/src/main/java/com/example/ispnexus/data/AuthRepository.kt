package com.example.ispnexus.data

import com.example.ispnexus.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // ── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Authentication failed: no user returned"))

            val document = db.collection("users").document(uid).get().await()

            if (!document.exists()) {
                return Result.failure(Exception("User profile not found in Firestore"))
            }

            val user = document.toObject<User>()
                ?: return Result.failure(Exception("Failed to parse user profile"))

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Register Company ──────────────────────────────────────────────────────

    suspend fun registerCompany(
        adminName: String,
        email: String,
        password: String,
        companyName: String,
        registrationNumber: String,
        taxPin: String,
        phoneNumber: String,
        logoUri: String? = null
    ): Result<Unit> {
        return try {
            // Step 1: Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Failed to create account"))

            // Step 2: Upload logo to Firebase Storage (if provided)
            val logoDownloadUrl: String? = if (logoUri != null) {
                try {
                    val logoRef = storage.reference
                        .child("company_logos/$uid.jpg")

                    // Upload the file from the local URI
                    val uploadTask = logoRef.putFile(android.net.Uri.parse(logoUri)).await()
                    uploadTask.storage.downloadUrl.await().toString()
                } catch (e: Exception) {
                    null // Logo upload failed — continue registration without it
                }
            } else null

            // Step 3: Save company to Firestore "companies" collection
            val companyData = hashMapOf(
                "adminName"          to adminName,
                "email"              to email,
                "companyName"        to companyName,
                "registrationNumber" to registrationNumber,
                "taxPin"             to taxPin,
                "phoneNumber"        to phoneNumber,
                "logoUrl"            to (logoDownloadUrl ?: ""),
                "status"             to "pending",      // super admin approves
                "adminUid"           to uid,
                "createdAt"          to System.currentTimeMillis()
            )

            db.collection("companies").document(uid).set(companyData).await()

            // Step 4: Save user profile to Firestore "users" collection
            val userData = hashMapOf(
                "uid"       to uid,
                "email"     to email,
                "name"      to adminName,
                "role"      to "admin",
                "companyId" to uid,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users").document(uid).set(userData).await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null
}