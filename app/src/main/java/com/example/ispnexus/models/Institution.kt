package com.example.ispnexus.models

data class Institution(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val contactPersonName: String = "",
    val contactPersonPhone: String = "",
    val companyId: String = "",
    val planId: String = "",
    val planName: String = "",
    val status: String = "active",
    val createdAt: Long = 0L
)