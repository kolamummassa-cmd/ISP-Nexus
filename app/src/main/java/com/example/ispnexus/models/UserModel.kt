package com.example.ispnexus.models



data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",
    val companyId: String = "",

    val createdAt: Any? = null
)