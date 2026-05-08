package com.example.ispnexus.models

data class Payment(
    val id: String = "",
    val companyId: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val subscriptionId: String = "",
    val invoiceId: String = "",
    val amountKsh: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "pending",
    val notes: String = "",
    val paidAt: Any? = null,
    val createdAt: Any? = null
)
