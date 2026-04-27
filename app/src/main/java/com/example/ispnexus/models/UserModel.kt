package com.example.ispnexus.models

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",        // e.g. "super_admin", "admin", "user"
    val companyId: String = "",   // for ISP company association
    val createdAt: Timestamp? = null
)