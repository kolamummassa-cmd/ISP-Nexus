package com.example.ispnexus.models

data class Plan(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val speedMbps: Int = 0,
    val billingCycle: String = "Monthly",
    val description: String = "",
    val dataCapGb: Int = 0,
    val isUnlimited: Boolean = true,
    val status: String = "Active",
    val createdAt: Any? = null,
    val updatedAt: Any? = null
)