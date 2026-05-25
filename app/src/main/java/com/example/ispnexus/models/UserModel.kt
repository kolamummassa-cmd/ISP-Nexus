package com.example.ispnexus.models



data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val position: String = "",
    val companyId: String = "",
    val status: String = "",
    val createdAt: Any? = null
)