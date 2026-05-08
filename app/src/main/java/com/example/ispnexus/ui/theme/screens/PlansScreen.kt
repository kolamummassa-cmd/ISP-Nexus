package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.models.Plan
import com.example.ispnexus.viewmodels.PlansViewModel

// ─── Colors (matching your Finance Dashboard style) ───────────────────────────

private val BgColor        = Color(0xFFF5F6FA)
private val CardColor      = Color.White
private val PrimaryBlue    = Color(0xFF1A73E8)
private val LightBlue      = Color(0xFFE8F0FE)
private val GreenActive    = Color(0xFF34A853)
private val LightGreen     = Color(0xFFE6F4EA)
private val RedInactive    = Color(0xFFEA4335)
private val LightRed       = Color(0xFFFCE8E6)
private val OrangeAccent   = Color(0xFFF9AB00)
private val LightOrange    = Color(0xFFFEF3CD)
private val TextPrimary    = Color(0xFF1A1A2E)
private val TextSecondary  = Color(0xFF6B7280)
private val DividerColor   = Color(0xFFF0F0F0)

// ─── Plans Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: PlansViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var selectedPlan      by remember { mutableStateOf<Plan?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Internet Plans",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Text(
                            text     = "${uiState.plans.size} plans configured",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = {
                    selectedPlan     = null
                    showAddEditDialog = true
                },
                containerColor   = PrimaryBlue,
                contentColor     = Color.White,
                shape            = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Plan")
            }
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier          = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment  = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.plans.isEmpty()) {
            EmptyPlansView(
                modifier          = Modifier.fillMaxSize().padding(paddingValues),
                onAddClick        = {
                    selectedPlan      = null
                    showAddEditDialog  = true
                }
            )
        } else {
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Row
                item {
                    PlansSummaryRow(plans = uiState.plans)
                }

                // Plan Cards
                items(uiState.plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan      = plan,
                        onEdit    = {
                            selectedPlan      = plan
                            showAddEditDialog  = true
                        },
                        onDelete  = {
                            selectedPlan     = plan
                            showDeleteDialog  = true
                        },
                        onToggle  = { viewModel.togglePlanStatus(plan) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ── Add / Edit Dialog ─────────────────────────────────────────────────────
    if (showAddEditDialog) {
        AddEditPlanDialog(
            existingPlan = selectedPlan,
            onDismiss    = { showAddEditDialog = false },
            onConfirm    = { name, price, speed, cycle, desc, unlimited, cap ->
                if (selectedPlan == null) {
                    viewModel.createPlan(name, price, speed, cycle, desc, unlimited, cap)
                } else {
                    viewModel.updatePlan(selectedPlan!!.id, name, price, speed, cycle, desc, unlimited, cap)
                }
                showAddEditDialog = false
            }
        )
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    if (showDeleteDialog && selectedPlan != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete Plan", fontWeight = FontWeight.Bold) },
            text             = {
                Text(
                    "Are you sure you want to delete \"${selectedPlan!!.name}\"? " +
                            "This cannot be undone."
                )
            },
            confirmButton    = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(selectedPlan!!)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = RedInactive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Summary Row ──────────────────────────────────────────────────────────────

@Composable
fun PlansSummaryRow(plans: List<Plan>) {
    val activeCount   = plans.count { it.status == "Active" }
    val inactiveCount = plans.count { it.status == "Inactive" }
    val monthlyCount  = plans.count { it.billingCycle == "Monthly" }

    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlanSummaryCard(
            modifier    = Modifier.weight(1f),
            label       = "Total Plans",
            value       = plans.size.toString(),
            bgColor     = LightBlue,
            valueColor  = PrimaryBlue
        )
        PlanSummaryCard(
            modifier    = Modifier.weight(1f),
            label       = "Active",
            value       = activeCount.toString(),
            bgColor     = LightGreen,
            valueColor  = GreenActive
        )
        PlanSummaryCard(
            modifier    = Modifier.weight(1f),
            label       = "Inactive",
            value       = inactiveCount.toString(),
            bgColor     = LightRed,
            valueColor  = RedInactive
        )
    }
}

@Composable
fun PlanSummaryCard(
    modifier   : Modifier,
    label      : String,
    value      : String,
    bgColor    : Color,
    valueColor : Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = value,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = valueColor
            )
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = TextSecondary
            )
        }
    }
}

// ─── Plan Card ────────────────────────────────────────────────────────────────

@Composable
fun PlanCard(
    plan     : Plan,
    onEdit   : () -> Unit,
    onDelete : () -> Unit,
    onToggle : () -> Unit
) {
    val isActive = plan.status == "Active"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Top Row: Name + Status Badge ──────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = plan.name,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        text     = plan.description.ifBlank { "No description" },
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isActive) LightGreen else LightRed,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = plan.status,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isActive) GreenActive else RedInactive
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(12.dp))

            // ── Info Grid ─────────────────────────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlanInfoChip(
                    icon  = Icons.Default.AttachMoney,
                    label = "Price",
                    value = "KES ${String.format("%,.0f", plan.price)} / ${
                        if (plan.billingCycle == "Monthly") "mo" else "yr"
                    }"
                )
                PlanInfoChip(
                    icon  = Icons.Default.Speed,
                    label = "Speed",
                    value = "${plan.speedMbps} Mbps"
                )
                PlanInfoChip(
                    icon  = Icons.Default.DataUsage,
                    label = "Data",
                    value = if (plan.isUnlimited) "Unlimited" else "${plan.dataCapGb} GB"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            // ── Actions Row ───────────────────────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                // Toggle Active/Inactive
                TextButton(onClick = onToggle) {
                    Icon(
                        imageVector  = if (isActive) Icons.Default.PauseCircle
                        else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint         = if (isActive) OrangeAccent else GreenActive,
                        modifier     = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = if (isActive) "Deactivate" else "Activate",
                        color = if (isActive) OrangeAccent else GreenActive,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                // Edit
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = null,
                        tint               = PrimaryBlue,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = PrimaryBlue, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))

                // Delete
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = null,
                        tint               = RedInactive,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = RedInactive, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Plan Info Chip ───────────────────────────────────────────────────────────

@Composable
fun PlanInfoChip(
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    label : String,
    value : String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = PrimaryBlue,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyPlansView(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Wifi,
            contentDescription = null,
            tint               = PrimaryBlue.copy(alpha = 0.4f),
            modifier           = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text       = "No Plans Yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text     = "Create your first internet plan\nfor your customers.",
            fontSize = 14.sp,
            color    = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape   = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Plan")
        }
    }
}

// ─── Add / Edit Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlanDialog(
    existingPlan : Plan?,
    onDismiss    : () -> Unit,
    onConfirm    : (
        name        : String,
        price       : Double,
        speedMbps   : Int,
        billingCycle: String,
        description : String,
        isUnlimited : Boolean,
        dataCapGb   : Int
    ) -> Unit
) {
    val isEditing = existingPlan != null

    var name         by remember { mutableStateOf(existingPlan?.name         ?: "") }
    var price        by remember { mutableStateOf(existingPlan?.price?.toString() ?: "") }
    var speed        by remember { mutableStateOf(existingPlan?.speedMbps?.toString() ?: "") }
    var billingCycle by remember { mutableStateOf(existingPlan?.billingCycle  ?: "Monthly") }
    var description  by remember { mutableStateOf(existingPlan?.description   ?: "") }
    var isUnlimited  by remember { mutableStateOf(existingPlan?.isUnlimited   ?: true) }
    var dataCap      by remember { mutableStateOf(existingPlan?.dataCapGb?.toString() ?: "") }

    var nameError    by remember { mutableStateOf(false) }
    var priceError   by remember { mutableStateOf(false) }
    var speedError   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = {
            Text(
                text       = if (isEditing) "Edit Plan" else "New Internet Plan",
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
        },
        text = {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Plan Name
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = { Text("Plan Name *") },
                    isError       = nameError,
                    supportingText = if (nameError) {{ Text("Required") }} else null,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                // Price
                OutlinedTextField(
                    value         = price,
                    onValueChange = { price = it; priceError = false },
                    label         = { Text("Price (KES) *") },
                    isError       = priceError,
                    supportingText = if (priceError) {{ Text("Enter a valid price") }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                // Speed
                OutlinedTextField(
                    value         = speed,
                    onValueChange = { speed = it; speedError = false },
                    label         = { Text("Speed (Mbps) *") },
                    isError       = speedError,
                    supportingText = if (speedError) {{ Text("Enter a valid speed") }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                // Billing Cycle Toggle
                Text(
                    text     = "Billing Cycle",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Monthly", "Yearly").forEach { cycle ->
                        val selected = billingCycle == cycle
                        OutlinedButton(
                            onClick  = { billingCycle = cycle },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) LightBlue else Color.Transparent
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) PrimaryBlue else TextSecondary.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text  = cycle,
                                color = if (selected) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }

                // Data Cap
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = "Unlimited Data",
                        fontSize = 14.sp,
                        color    = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked         = isUnlimited,
                        onCheckedChange = { isUnlimited = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor  = Color.White,
                            checkedTrackColor  = GreenActive
                        )
                    )
                }

                if (!isUnlimited) {
                    OutlinedTextField(
                        value           = dataCap,
                        onValueChange   = { dataCap = it },
                        label           = { Text("Data Cap (GB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier        = Modifier.fillMaxWidth(),
                        singleLine      = true
                    )
                }

                // Description
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2,
                    maxLines      = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate
                    nameError  = name.isBlank()
                    priceError = price.toDoubleOrNull() == null
                    speedError = speed.toIntOrNull() == null

                    if (!nameError && !priceError && !speedError) {
                        onConfirm(
                            name.trim(),
                            price.toDouble(),
                            speed.toInt(),
                            billingCycle,
                            description.trim(),
                            isUnlimited,
                            if (isUnlimited) 0 else (dataCap.toIntOrNull() ?: 0)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEditing) "Save Changes" else "Create Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
