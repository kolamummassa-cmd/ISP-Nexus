package com.example.ispnexus.data

import com.example.ispnexus.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject         // ← fixes .toObject red error
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Login user and fetch their Firestore profile.
     * Returns a Result so the caller can handle success/failure cleanly.
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Step 1: Authenticate with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Authentication failed: no user returned"))

            // Step 2: Fetch user document from Firestore
            val document = db
                .collection("users")   // ← fixes .collection red error
                .document(uid)
                .get()
                .await()

            // Step 3: Check document exists
            if (!document.exists()) {   // ← fixes .exists red error
                return Result.failure(Exception("User profile not found in Firestore"))
            }

            // Step 4: Convert to User model
            val user = document.toObject<User>()   // ← fixes .toObject / .java red error
                ?: return Result.failure(Exception("Failed to parse user profile"))

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout the current user.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Returns the UID of the currently logged-in user, or null if not logged in.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Returns true if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}