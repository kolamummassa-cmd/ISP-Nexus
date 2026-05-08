package com.example.ispnexus.ui.theme.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.InstitutionDropdownItem
import com.example.ispnexus.viewmodels.RecordPaymentViewModel

// ─── Colors ───────────────────────────────────────────────────────────────────

private val BgColor       = Color(0xFFF5F6FA)
private val CardColor     = Color.White
private val PrimaryBlue   = Color(0xFF1A73E8)
private val LightBlue     = Color(0xFFE8F0FE)
private val GreenActive   = Color(0xFF34A853)
private val LightGreen    = Color(0xFFE6F4EA)
private val RedColor      = Color(0xFFEA4335)
private val TextPrimary   = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)
private val DividerColor  = Color(0xFFF0F0F0)
private val BorderColor   = Color(0xFFE0E0E0)

// ─── Payment Methods ──────────────────────────────────────────────────────────

private data class PaymentMethodOption(
    val label : String,
    val icon  : ImageVector
)

private val paymentMethods = listOf(
    PaymentMethodOption("M-Pesa",        Icons.Default.PhoneAndroid),
    PaymentMethodOption("Bank Transfer", Icons.Default.AccountBalance),
    PaymentMethodOption("Cash",          Icons.Default.Payments)
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    viewModel : RecordPaymentViewModel = viewModel(),
    onBack    : () -> Unit = {},
    onSuccess : () -> Unit = {}
) {
    val uiState        by viewModel.uiState.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }
    val scrollState    = rememberScrollState()

    // ── Form State ────────────────────────────────────────────────────────────
    var selectedInstitution by remember { mutableStateOf<InstitutionDropdownItem?>(null) }
    var dropdownExpanded    by remember { mutableStateOf(false) }
    var amount              by remember { mutableStateOf("") }
    var selectedMethod      by remember { mutableStateOf("M-Pesa") }
    var referenceNumber     by remember { mutableStateOf("") }
    var periodMonth         by remember { mutableStateOf("") }
    var notes               by remember { mutableStateOf("") }

    // ── Validation errors ─────────────────────────────────────────────────────
    var institutionError    by remember { mutableStateOf(false) }
    var amountError         by remember { mutableStateOf(false) }
    var referenceError      by remember { mutableStateOf(false) }
    var periodError         by remember { mutableStateOf(false) }

    // ── Snackbar triggers ─────────────────────────────────────────────────────
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearMessages()
            onSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost   = { SnackbarHost(snackbarState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Record Payment",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Text(
                            text     = "Finance · Payment Entry",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section: Institution ──────────────────────────────────────────
            PaymentSectionCard(title = "Institution Details") {

                // Institution Dropdown
                Text(
                    text     = "Institution *",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ExposedDropdownMenuBox(
                    expanded        = dropdownExpanded,
                    onExpandedChange = {
                        if (!uiState.isLoadingInstitutions) dropdownExpanded = it
                    }
                ) {
                    OutlinedTextField(
                        value         = selectedInstitution?.name ?: "",
                        onValueChange = {},
                        readOnly      = true,
                        placeholder   = { Text("Select institution") },
                        trailingIcon  = {
                            if (uiState.isLoadingInstitutions) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color    = PrimaryBlue
                                )
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            }
                        },
                        isError       = institutionError,
                        supportingText = if (institutionError) {
                            { Text("Please select an institution") }
                        } else null,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    ExposedDropdownMenu(
                        expanded        = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        if (uiState.institutions.isEmpty()) {
                            DropdownMenuItem(
                                text    = { Text("No institutions found", color = TextSecondary) },
                                onClick = { dropdownExpanded = false }
                            )
                        } else {
                            uiState.institutions.forEach { institution ->
                                DropdownMenuItem(
                                    text    = {
                                        Text(
                                            text  = institution.name,
                                            color = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedInstitution = institution
                                        institutionError    = false
                                        dropdownExpanded    = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector        = Icons.Default.Business,
                                            contentDescription = null,
                                            tint               = PrimaryBlue,
                                            modifier           = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Period Month
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value         = periodMonth,
                    onValueChange = { periodMonth = it; periodError = false },
                    label         = { Text("Payment Period *") },
                    placeholder   = { Text("e.g. May 2026") },
                    isError       = periodError,
                    supportingText = if (periodError) {
                        { Text("Enter the payment period") }
                    } else null,
                    leadingIcon   = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            // ── Section: Payment Details ──────────────────────────────────────
            PaymentSectionCard(title = "Payment Details") {

                // Amount
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it; amountError = false },
                    label           = { Text("Amount (KES) *") },
                    isError         = amountError,
                    supportingText  = if (amountError) {
                        { Text("Enter a valid amount") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon     = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Selector
                Text(
                    text     = "Payment Method *",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = selectedMethod == method.label
                        Column(
                            modifier            = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) PrimaryBlue
                                    else BorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) LightBlue else CardColor)
                                .clickable { selectedMethod = method.label }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector        = method.icon,
                                contentDescription = method.label,
                                tint               = if (isSelected) PrimaryBlue
                                else TextSecondary,
                                modifier           = Modifier.size(22.dp)
                            )
                            Text(
                                text       = method.label,
                                fontSize   = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                color      = if (isSelected) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reference Number
                OutlinedTextField(
                    value           = referenceNumber,
                    onValueChange   = { referenceNumber = it; referenceError = false },
                    label           = {
                        Text(
                            when (selectedMethod) {
                                "M-Pesa"        -> "M-Pesa Transaction Code *"
                                "Bank Transfer" -> "Bank Reference Number *"
                                else            -> "Receipt Number *"
                            }
                        )
                    },
                    placeholder     = {
                        Text(
                            when (selectedMethod) {
                                "M-Pesa"        -> "e.g. QJK2X3ABCD"
                                "Bank Transfer" -> "e.g. TXN123456789"
                                else            -> "e.g. RCT-001"
                            }
                        )
                    },
                    isError         = referenceError,
                    supportingText  = if (referenceError) {
                        { Text("Reference number is required") }
                    } else null,
                    leadingIcon     = {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            // ── Section: Additional Info ───────────────────────────────────────
            PaymentSectionCard(title = "Additional Info") {
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notes (optional)") },
                    placeholder   = { Text("Any extra details about this payment...") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    maxLines      = 4,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            // ── Invoice Auto-generate Notice ──────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LightGreen)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint               = GreenActive,
                    modifier           = Modifier.size(20.dp)
                )
                Text(
                    text     = "An invoice will be auto-generated and linked to this payment.",
                    fontSize = 13.sp,
                    color    = GreenActive,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Submit Button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    // Validate all fields
                    institutionError = selectedInstitution == null
                    amountError      = amount.toDoubleOrNull() == null || amount.toDoubleOrNull()!! <= 0
                    referenceError   = referenceNumber.isBlank()
                    periodError      = periodMonth.isBlank()

                    if (!institutionError && !amountError && !referenceError && !periodError) {
                        viewModel.recordPayment(
                            institution    = selectedInstitution!!,
                            amount         = amount.toDouble(),
                            paymentMethod  = selectedMethod,
                            referenceNumber = referenceNumber.trim(),
                            periodMonth    = periodMonth.trim(),
                            notes          = notes.trim()
                        )
                    }
                },
                enabled  = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.6f)
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color       = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving...", fontSize = 16.sp, color = Color.White)
                } else {
                    Icon(
                        imageVector        = Icons.Default.Save,
                        contentDescription = null,
                        tint               = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = "Record Payment",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Section Card Wrapper ─────────────────────────────────────────────────────

@Composable
fun PaymentSectionCard(
    title   : String,
    content : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 10.dp),
                color     = DividerColor
            )
            content()
        }
    }
}