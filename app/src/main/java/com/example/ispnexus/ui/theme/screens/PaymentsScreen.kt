package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.models.Institution
import com.example.ispnexus.models.Payment
import com.example.ispnexus.models.Subscription
import com.example.ispnexus.viewmodels.PaymentActionState
import com.example.ispnexus.viewmodels.PaymentsUiState
import com.example.ispnexus.viewmodels.PaymentsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue      = Color(0xFF1565C0)
private val LightBlue     = Color(0xFFE3F2FD)
private val PageBg        = Color(0xFFF4F6FB)
private val CardBg        = Color(0xFFFFFFFF)
private val BorderColor   = Color(0xFFE8EAF0)
private val TextPrimary   = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF90A4AE)
private val GreenText     = Color(0xFF2E7D32)
private val GreenBg       = Color(0xFFE8F5E9)
private val AmberText     = Color(0xFFF57F17)
private val AmberBg       = Color(0xFFFFF8E1)
private val RedText       = Color(0xFFC62828)
private val RedBg         = Color(0xFFFFEBEE)
private val GreyText      = Color(0xFF546E7A)
private val GreyBg        = Color(0xFFECEFF1)

private val paymentMethods = listOf("Bank Transfer", "Mobile Money", "Cash")

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return "Ksh ${fmt.format(amount)}"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "-"
    return try {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    } catch (e: Exception) { "-" }
}

private fun methodIcon(method: String): ImageVector = when (method) {
    "Bank Transfer" -> Icons.Outlined.AccountBalance
    "Mobile Money"  -> Icons.Outlined.PhoneAndroid
    "Cash"          -> Icons.Outlined.Payments
    else            -> Icons.Outlined.AttachMoney
}

// ── Screen ────────────────────────────────────────────────────────────────────
// NOTE: Only PaymentFormDialog is changed. All other composables
// (PaymentsScreen, PaymentCard, PayInfoChip, PaySummaryCard, PayFormField)
// remain exactly as your existing file. Paste only PaymentFormDialog
// replacing the old one.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentFormDialog(
    payment: Payment?,
    isLoading: Boolean,
    institutions: List<Institution>,
    institutionsLoading: Boolean,
    subscriptions: List<Subscription>,
    subscriptionsLoading: Boolean,
    onInstitutionSelected: (Institution) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Payment) -> Unit
) {
    // ── Institution picker state ───────────────────────────────────────────────
    var selectedInstitution  by remember {
        mutableStateOf<Institution?>(
            // pre-fill on edit
            if (payment != null && payment.institutionId.isNotEmpty())
                institutions.find { it.id == payment.institutionId }
            else null
        )
    }
    var institutionExpanded  by remember { mutableStateOf(false) }

    // ── Subscription picker state ─────────────────────────────────────────────
    var selectedSubscription by remember {
        mutableStateOf<Subscription?>(
            if (payment != null && payment.subscriptionId.isNotEmpty())
                subscriptions.find { it.id == payment.subscriptionId }
            else null
        )
    }
    var subscriptionExpanded by remember { mutableStateOf(false) }

    // ── Other form fields ─────────────────────────────────────────────────────
    var amountKsh     by remember {
        mutableStateOf(
            if (payment != null && payment.amountKsh > 0)
                payment.amountKsh.toInt().toString() else ""
        )
    }
    var paymentMethod by remember { mutableStateOf(payment?.paymentMethod ?: paymentMethods[0]) }
    var status        by remember { mutableStateOf(payment?.status ?: "pending") }
    var notes         by remember { mutableStateOf(payment?.notes ?: "") }

    var institutionError by remember { mutableStateOf(false) }
    var amountError      by remember { mutableStateOf(false) }

    // Auto-fill amount from subscription when one is selected
    LaunchedEffect(selectedSubscription) {
        selectedSubscription?.let { sub ->
            if (payment == null) {          // only auto-fill on add, not edit
                amountKsh = sub.amountKsh.toInt().toString()
            }
        }
    }

    val title = if (payment == null) "Record Payment" else "Edit Payment"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            LazyColumn(
                modifier            = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Header ────────────────────────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier         = Modifier.size(40.dp).clip(CircleShape)
                                    .background(LightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (payment == null) Icons.Default.Add else Icons.Default.Edit,
                                    null, tint = NavyBlue, modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = TextPrimary)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Close",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // ── Step 1: Institution Dropdown ──────────────────────────────
                item {
                    Text("Institution *", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded         = institutionExpanded,
                        onExpandedChange = {
                            institutionExpanded = it
                            institutionError    = false
                        }
                    ) {
                        OutlinedTextField(
                            value         = selectedInstitution?.name ?: "",
                            onValueChange = {},
                            readOnly      = true,
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder   = {
                                Text(
                                    if (institutionsLoading) "Loading institutions..."
                                    else "Select institution",
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon   = {
                                Icon(Icons.Outlined.Business, null,
                                    tint     = if (institutionError) RedText else TextSecondary,
                                    modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                if (institutionsLoading) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(16.dp),
                                        color       = NavyBlue,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = institutionExpanded)
                                }
                            },
                            isError        = institutionError,
                            supportingText = {
                                if (institutionError)
                                    Text("Please select an institution",
                                        fontSize = 10.sp, color = RedText)
                            },
                            singleLine = true,
                            shape      = RoundedCornerShape(10.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = NavyBlue,
                                unfocusedBorderColor    = BorderColor,
                                errorBorderColor        = RedText,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            )
                        )

                        ExposedDropdownMenu(
                            expanded         = institutionExpanded,
                            onDismissRequest = { institutionExpanded = false }
                        ) {
                            if (institutions.isEmpty() && !institutionsLoading) {
                                DropdownMenuItem(
                                    text    = {
                                        Text("No institutions found", fontSize = 12.sp,
                                            color = TextSecondary)
                                    },
                                    onClick = { institutionExpanded = false }
                                )
                            } else {
                                institutions.forEach { institution ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(institution.name, fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary)
                                                Text(institution.email, fontSize = 10.sp,
                                                    color = TextSecondary)
                                            }
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier         = Modifier.size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(LightBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    institution.name.take(1).uppercase(),
                                                    fontSize   = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color      = NavyBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedInstitution  = institution
                                            selectedSubscription = null   // reset subscription
                                            institutionExpanded  = false
                                            onInstitutionSelected(institution)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Step 2: Subscription Dropdown ─────────────────────────────
                item {
                    Text("Subscription", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded         = subscriptionExpanded,
                        onExpandedChange = {
                            if (selectedInstitution != null) subscriptionExpanded = it
                        }
                    ) {
                        OutlinedTextField(
                            value         = selectedSubscription?.let {
                                "${it.planName} — ${formatKsh(it.amountKsh)} / ${it.billingCycle}"
                            } ?: "",
                            onValueChange = {},
                            readOnly      = true,
                            enabled       = selectedInstitution != null,
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder   = {
                                Text(
                                    when {
                                        selectedInstitution == null  -> "Select institution first"
                                        subscriptionsLoading         -> "Loading subscriptions..."
                                        subscriptions.isEmpty()      -> "No subscriptions found"
                                        else                         -> "Select subscription"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon   = {
                                Icon(Icons.Outlined.Wifi, null,
                                    tint     = if (selectedInstitution == null) TextSecondary.copy(alpha = 0.4f)
                                    else TextSecondary,
                                    modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                if (subscriptionsLoading) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(16.dp),
                                        color       = NavyBlue,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = subscriptionExpanded)
                                }
                            },
                            singleLine = true,
                            shape      = RoundedCornerShape(10.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor        = NavyBlue,
                                unfocusedBorderColor      = BorderColor,
                                disabledBorderColor       = BorderColor,
                                disabledContainerColor    = CardBg,
                                focusedContainerColor     = CardBg,
                                unfocusedContainerColor   = CardBg
                            )
                        )

                        ExposedDropdownMenu(
                            expanded         = subscriptionExpanded,
                            onDismissRequest = { subscriptionExpanded = false }
                        ) {
                            if (subscriptions.isEmpty() && !subscriptionsLoading) {
                                DropdownMenuItem(
                                    text    = {
                                        Text("No subscriptions for this institution",
                                            fontSize = 12.sp, color = TextSecondary)
                                    },
                                    onClick = { subscriptionExpanded = false }
                                )
                            } else {
                                subscriptions.forEach { sub ->
                                    val (statusColor, statusBg) = when (sub.status) {
                                        "active"    -> GreenText to GreenBg
                                        "suspended" -> AmberText to AmberBg
                                        else        -> RedText   to RedBg
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier              = Modifier.fillMaxWidth(),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(sub.planName, fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary)
                                                    Text(
                                                        "${formatKsh(sub.amountKsh)} / ${sub.billingCycle}",
                                                        fontSize = 10.sp, color = TextSecondary
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        sub.status.replaceFirstChar { it.uppercase() },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedSubscription = sub
                                            subscriptionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Amount (auto-filled from subscription) ────────────────────
                item {
                    PayFormField(
                        value         = amountKsh,
                        onValueChange = { amountKsh = it; amountError = false },
                        label         = "Amount (Ksh) *",
                        icon          = Icons.Outlined.AttachMoney,
                        isError       = amountError,
                        errorMessage  = "Enter a valid amount"
                    )
                }

                // ── Payment Method ────────────────────────────────────────────
                item {
                    Text("Payment Method", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paymentMethods) { method ->
                            FilterChip(
                                selected    = paymentMethod == method,
                                onClick     = { paymentMethod = method },
                                leadingIcon = {
                                    Icon(methodIcon(method), null,
                                        modifier = Modifier.size(14.dp))
                                },
                                label  = { Text(method, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor   = NavyBlue,
                                    selectedLabelColor       = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }

                // ── Status ────────────────────────────────────────────────────
                item {
                    Text("Status", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("pending", "completed", "failed").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick  = { status = s },
                                label    = {
                                    Text(s.replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (s) {
                                        "completed" -> GreenText
                                        "pending"   -> AmberText
                                        else        -> RedText
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // ── Notes ─────────────────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value         = notes,
                        onValueChange = { notes = it },
                        modifier      = Modifier.fillMaxWidth(),
                        label         = { Text("Notes (optional)", fontSize = 12.sp) },
                        leadingIcon   = {
                            Icon(Icons.Outlined.Notes, null,
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape    = RoundedCornerShape(10.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = NavyBlue,
                            unfocusedBorderColor    = BorderColor,
                            focusedContainerColor   = CardBg,
                            unfocusedContainerColor = CardBg
                        )
                    )
                }

                // ── Invoice info banner ───────────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LightBlue)
                            .padding(10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Info, null,
                            tint = NavyBlue, modifier = Modifier.size(16.dp))
                        Text(
                            "An invoice will be automatically generated for this payment.",
                            fontSize = 11.sp, color = NavyBlue
                        )
                    }
                }

                // ── Buttons ───────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            border   = BorderStroke(1.dp, BorderColor)
                        ) { Text("Cancel", color = TextPrimary) }

                        Button(
                            onClick = {
                                val amount = amountKsh.trim().toDoubleOrNull()
                                if (selectedInstitution == null) {
                                    institutionError = true; return@Button
                                }
                                if (amount == null || amount <= 0) {
                                    amountError = true; return@Button
                                }
                                onConfirm(
                                    Payment(
                                        id              = payment?.id ?: "",
                                        companyId       = payment?.companyId ?: "",
                                        institutionId   = selectedInstitution!!.id,
                                        institutionName = selectedInstitution!!.name,
                                        subscriptionId  = selectedSubscription?.id ?: "",
                                        invoiceId       = payment?.invoiceId ?: "",
                                        amountKsh       = amount,
                                        paymentMethod   = paymentMethod,
                                        status          = status,
                                        notes           = notes.trim(),
                                        paidAt          = if (status == "completed")
                                            System.currentTimeMillis() else 0L,
                                        createdAt       = payment?.createdAt ?: 0L
                                    )
                                )
                            },
                            enabled  = !isLoading,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                    color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(if (payment == null) "Record" else "Save",
                                    color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}