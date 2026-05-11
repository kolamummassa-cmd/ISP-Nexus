package com.example.ispnexus.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserSession(
    val uid: String = "",
    val role: String = "",
    val position: String = "",
    val companyId: String = ""
)

class UserSessionViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    init { loadSession() }

    fun loadSession() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val doc = db.collection("users").document(uid).get().await()
                _session.value = UserSession(
                    uid       = uid,
                    role      = doc.getString("role") ?: "",
                    position  = doc.getString("position") ?: "",
                    companyId = doc.getString("companyId") ?: ""
                )
            } catch (e: Exception) { }
        }
    }
}
