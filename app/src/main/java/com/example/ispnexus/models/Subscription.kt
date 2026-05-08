package com.example.ispnexus.models

data class Subscription(
    val id: String = "",
    val companyId: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val planId: String = "",
    val planName: String = "",
    val amountKsh: Double = 0.0,
    val billingCycle: String = "monthly",
    val status: String = "active",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val createdAt: Any? = null
)