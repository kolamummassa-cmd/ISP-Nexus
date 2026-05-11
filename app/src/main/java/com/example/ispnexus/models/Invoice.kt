package com.example.ispnexus.models

data class Invoice(
    val id: String = "",
    val invoiceNumber: String = "",
    val companyId: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val subscriptionId: String = "",
    val paymentId: String = "",
    val amountKsh: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "pending",
    val notes: String = "",
    val issuedAt: Long = 0L,
    val dueDate: Long = 0L,
    val paidAt: Long = 0L,
    val createdAt: Any? = null
)