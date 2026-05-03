package com.example.ispnexus.models

data class Company(
    val id: String = "", // This will hold the Firestore document ID
    val adminName: String = "",
    val email: String = "",
    val companyName: String = "",
    val registrationNumber: String = "",
    val taxPin: String = "",
    val phoneNumber: String = "",
    val logoUrl: String = "",
    val status: String = "Pending", // Default to uppercase
    val adminUid: String = "",
    val createdAt: Long = 0L
)
