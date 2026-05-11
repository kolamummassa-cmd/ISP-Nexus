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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onBack: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: PaymentsViewModel = viewModel()
) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val actionState  by viewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery  by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val methodFilter by viewModel.methodFilter.collectAsStateWithLifecycle()

    // ── Picker state ──────────────────────────────────────────────────────────
    val institutions         by viewModel.institutions.collectAsStateWithLifecycle()
    val institutionsLoading  by viewModel.institutionsLoading.collectAsStateWithLifecycle()
    val subscriptions        by viewModel.subscriptionsForInstitution.collectAsStateWithLifecycle()
    val subscriptionsLoading by viewModel.subscriptionsLoading.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPayment  by remember { mutableStateOf<Payment?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Side-effects ──────────────────────────────────────────────────────────
    LaunchedEffect(actionState) {
        when (val a = actionState) {
            is PaymentActionState.Success -> {
                snackbarHostState.showSnackbar("Done successfully")
                showAddDialog    = false
                showEditDialog   = false
                showDeleteDialog = false
                selectedPayment  = null
                viewModel.resetActionState()
            }
            is PaymentActionState.Error -> {
                snackbarHostState.showSnackbar(a.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    // Load institutions whenever add or edit dialog opens
    LaunchedEffect(showAddDialog, showEditDialog) {
        if (showAddDialog || showEditDialog) {
            viewModel.loadInstitutions()
        }
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NavyBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor     = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Payments", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Track & manage payments", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Payment")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddDialog = true },
                containerColor = NavyBlue,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { padding ->

        when (val s = state) {
            is PaymentsUiState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = NavyBlue) }
            }

            is PaymentsUiState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadPayments() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) { Text("Retry") }
                    }
                }
            }

            is PaymentsUiState.Success -> {
                val filtered = viewModel.filteredPayments(s.payments)

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Summary Cards ─────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PaySummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Completed",
                                value    = viewModel.totalCompleted(s.payments).toString(),
                                color    = GreenText,
                                bgColor  = GreenBg,
                                icon     = Icons.Default.CheckCircle
                            )
                            PaySummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Pending",
                                value    = viewModel.totalPending(s.payments).toString(),
                                color    = AmberText,
                                bgColor  = AmberBg,
                                icon     = Icons.Default.HourglassEmpty
                            )
                            PaySummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Failed",
                                value    = viewModel.totalFailed(s.payments).toString(),
                                color    = RedText,
                                bgColor  = RedBg,
                                icon     = Icons.Default.Cancel
                            )
                        }
                    }

                    // ── Revenue Banner ────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier  = Modifier.weight(1f),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = NavyBlue),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Collected", fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.75f))
                                    Text(
                                        text       = formatKsh(viewModel.totalCompletedAmount(s.payments)),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                    Text("Completed payments", fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.55f))
                                }
                            }
                            Card(
                                modifier  = Modifier.weight(1f),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = AmberBg),
                                elevation = CardDefaults.cardElevation(1.dp),
                                border    = BorderStroke(1.dp, AmberText.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Outstanding", fontSize = 10.sp, color = AmberText)
                                    Text(
                                        text       = formatKsh(viewModel.totalPendingAmount(s.payments)),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = AmberText
                                    )
                                    Text("Pending payments", fontSize = 9.sp,
                                        color = AmberText.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // ── Search ────────────────────────────────────────────────
                    item {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier      = Modifier.fillMaxWidth(),
                            placeholder   = { Text("Search institution or invoice...", fontSize = 13.sp) },
                            leadingIcon   = {
                                Icon(Icons.Default.Search, null,
                                    tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, "Clear",
                                            tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine    = true,
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = NavyBlue,
                                unfocusedBorderColor    = BorderColor,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            )
                        )
                    }

                    // ── Status Filter Chips ───────────────────────────────────
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statuses = listOf("all", "completed", "pending", "failed")
                            items(statuses) { status ->
                                FilterChip(
                                    selected = statusFilter == status,
                                    onClick  = { viewModel.onStatusFilterChange(status) },
                                    label    = {
                                        Text(status.replaceFirstChar { it.uppercase() },
                                            fontSize = 12.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (status) {
                                            "completed" -> GreenText
                                            "pending"   -> AmberText
                                            "failed"    -> RedText
                                            else        -> NavyBlue
                                        },
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ── Method Filter Chips ───────────────────────────────────
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val methods = listOf("all") + paymentMethods
                            items(methods) { method ->
                                FilterChip(
                                    selected = methodFilter == method,
                                    onClick  = { viewModel.onMethodFilterChange(method) },
                                    label    = {
                                        Text(method.replaceFirstChar { it.uppercase() },
                                            fontSize = 12.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyBlue,
                                        selectedLabelColor     = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ── Count ─────────────────────────────────────────────────
                    item {
                        Text(
                            "${filtered.size} payment${if (filtered.size != 1) "s" else ""}",
                            fontSize = 12.sp, color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    // ── Empty State ───────────────────────────────────────────
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Payments, null,
                                        tint = TextSecondary, modifier = Modifier.size(52.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No payments found", fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Text("Tap + to record a payment", fontSize = 12.sp,
                                        color = TextSecondary.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // ── Payment Cards ─────────────────────────────────────────
                    items(filtered, key = { it.id }) { payment ->
                        PaymentCard(
                            payment    = payment,
                            onEdit     = { selectedPayment = payment; showEditDialog = true },
                            onComplete = { viewModel.markCompleted(payment) },
                            onFail     = { viewModel.markFailed(payment) },
                            onDelete   = { selectedPayment = payment; showDeleteDialog = true }
                        )
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── Add Dialog ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        PaymentFormDialog(
            payment              = null,
            isLoading            = actionState is PaymentActionState.Loading,
            institutions         = institutions,
            institutionsLoading  = institutionsLoading,
            subscriptions        = subscriptions,
            subscriptionsLoading = subscriptionsLoading,
            onInstitutionSelected = { institution ->
                viewModel.loadSubscriptionsForInstitution(institution.id)
            },
            onDismiss = {
                showAddDialog = false
                viewModel.clearSubscriptions()
            },
            onConfirm = { viewModel.addPayment(it) }
        )
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────────
    if (showEditDialog && selectedPayment != null) {
        PaymentFormDialog(
            payment              = selectedPayment,
            isLoading            = actionState is PaymentActionState.Loading,
            institutions         = institutions,
            institutionsLoading  = institutionsLoading,
            subscriptions        = subscriptions,
            subscriptionsLoading = subscriptionsLoading,
            onInstitutionSelected = { institution ->
                viewModel.loadSubscriptionsForInstitution(institution.id)
            },
            onDismiss = {
                showEditDialog  = false
                selectedPayment = null
                viewModel.clearSubscriptions()
            },
            onConfirm = { viewModel.updatePayment(it) }
        )
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    if (showDeleteDialog && selectedPayment != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; selectedPayment = null },
            icon  = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedText) },
            title = { Text("Delete Payment", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Delete payment of ${formatKsh(selectedPayment?.amountKsh ?: 0.0)} " +
                            "for ${selectedPayment?.institutionName}? This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedPayment?.let { viewModel.deletePayment(it.id) } },
                    colors  = ButtonDefaults.buttonColors(containerColor = RedText),
                    enabled = actionState !is PaymentActionState.Loading
                ) {
                    if (actionState is PaymentActionState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false; selectedPayment = null },
                    border  = BorderStroke(1.dp, BorderColor)
                ) { Text("Cancel", color = TextPrimary) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Payment Card ──────────────────────────────────────────────────────────────

@Composable
private fun PaymentCard(
    payment: Payment,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onFail: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val (statusText, statusColor, statusBg) = when (payment.status) {
        "completed" -> Triple("Completed", GreenText, GreenBg)
        "pending"   -> Triple("Pending",   AmberText, AmberBg)
        "failed"    -> Triple("Failed",    RedText,   RedBg)
        else        -> Triple(payment.status, GreyText, GreyBg)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border    = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Top Row ───────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.weight(1f)
                ) {
                    Box(
                        modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                            .background(LightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            methodIcon(payment.paymentMethod),
                            contentDescription = null,
                            tint     = NavyBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = payment.institutionName,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = payment.paymentMethod.ifEmpty { "—" },
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = formatKsh(payment.amountKsh),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(statusText, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, "More",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Edit", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Edit, null,
                                    modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onEdit() }
                            )
                            if (payment.status == "pending") {
                                DropdownMenuItem(
                                    text        = { Text("Mark Completed", fontSize = 13.sp,
                                        color = GreenText) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null,
                                        tint = GreenText, modifier = Modifier.size(16.dp)) },
                                    onClick     = { showMenu = false; onComplete() }
                                )
                                DropdownMenuItem(
                                    text        = { Text("Mark Failed", fontSize = 13.sp,
                                        color = RedText) },
                                    leadingIcon = { Icon(Icons.Default.Cancel, null,
                                        tint = RedText, modifier = Modifier.size(16.dp)) },
                                    onClick     = { showMenu = false; onFail() }
                                )
                            }
                            if (payment.status == "failed") {
                                DropdownMenuItem(
                                    text        = { Text("Mark Completed", fontSize = 13.sp,
                                        color = GreenText) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null,
                                        tint = GreenText, modifier = Modifier.size(16.dp)) },
                                    onClick     = { showMenu = false; onComplete() }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text        = { Text("Delete", fontSize = 13.sp, color = RedText) },
                                leadingIcon = { Icon(Icons.Default.Delete, null,
                                    tint = RedText, modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(Modifier.height(10.dp))

            // ── Info Row ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PayInfoChip(
                    icon  = Icons.Outlined.Receipt,
                    label = payment.invoiceId.take(8).ifEmpty { "No Invoice" }
                )
                PayInfoChip(
                    icon  = Icons.Outlined.CalendarToday,
                    label = formatDate((payment.createdAt as? Long) ?: 0L)
                )
                PayInfoChip(
                    icon  = if (payment.status == "completed") Icons.Outlined.EventAvailable
                    else Icons.Outlined.EventBusy,
                    label = if (payment.status == "completed")
                        formatDate((payment.paidAt as? Long) ?: 0L)
                    else "Not paid"
                )
            }

            // ── Expanded: notes ───────────────────────────────────────────────
            if (expanded && payment.notes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Notes, null,
                        tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text(payment.notes, fontSize = 12.sp, color = TextSecondary)
                }
            }

            // ── Expand toggle ─────────────────────────────────────────────────
            if (payment.notes.isNotEmpty()) {
                Row(
                    modifier  = Modifier.fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(if (expanded) "Show less" else "Show notes",
                        fontSize = 11.sp, color = NavyBlue)
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        null, tint = NavyBlue, modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Info Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun PayInfoChip(icon: ImageVector, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Summary Card ──────────────────────────────────────────────────────────────

@Composable
private fun PaySummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
    bgColor: Color,
    icon: ImageVector
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border    = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary,
                textAlign = TextAlign.Center)
        }
    }
}

// ── Form Dialog ───────────────────────────────────────────────────────────────

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
    var selectedInstitution  by remember {
        mutableStateOf<Institution?>(
            if (payment != null && payment.institutionId.isNotEmpty())
                institutions.find { it.id == payment.institutionId }
            else null
        )
    }
    var institutionExpanded  by remember { mutableStateOf(false) }
    var billingCycle by remember { mutableStateOf(payment?.billingCycle ?: "monthly") }
    var selectedSubscription by remember {
        mutableStateOf<Subscription?>(
            if (payment != null && payment.subscriptionId.isNotEmpty())
                subscriptions.find { it.id == payment.subscriptionId }
            else null
        )
    }
    var subscriptionExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(selectedSubscription) {
        selectedSubscription?.let { sub ->
            if (payment == null) {
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

                // Header
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

                // Institution Dropdown
                item {
                    Text("Institution *", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded         = institutionExpanded,
                        onExpandedChange = { institutionExpanded = it; institutionError = false }
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
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                        color = NavyBlue, strokeWidth = 2.dp)
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
                                    text    = { Text("No institutions found",
                                        fontSize = 12.sp, color = TextSecondary) },
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
                                                Text(institution.name.take(1).uppercase(),
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                    color = NavyBlue)
                                            }
                                        },
                                        onClick = {
                                            selectedInstitution  = institution
                                            selectedSubscription = null

                                            institutionExpanded  = false
                                            onInstitutionSelected(institution)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Subscription Dropdown
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
                                        selectedInstitution == null -> "Select institution first"
                                        subscriptionsLoading       -> "Loading subscriptions..."
                                        subscriptions.isEmpty()    -> "No subscriptions found"
                                        else                       -> "Select subscription"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon   = {
                                Icon(Icons.Outlined.Wifi, null,
                                    tint     = if (selectedInstitution == null)
                                        TextSecondary.copy(alpha = 0.4f)
                                    else TextSecondary,
                                    modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                if (subscriptionsLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                        color = NavyBlue, strokeWidth = 2.dp)
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = subscriptionExpanded)
                                }
                            },
                            singleLine = true,
                            shape      = RoundedCornerShape(10.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = NavyBlue,
                                unfocusedBorderColor    = BorderColor,
                                disabledBorderColor     = BorderColor,
                                disabledContainerColor  = CardBg,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            )
                        )
                        ExposedDropdownMenu(
                            expanded         = subscriptionExpanded,
                            onDismissRequest = { subscriptionExpanded = false }
                        ) {
                            if (subscriptions.isEmpty() && !subscriptionsLoading) {
                                DropdownMenuItem(
                                    text    = { Text("No subscriptions for this institution",
                                        fontSize = 12.sp, color = TextSecondary) },
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
                                            billingCycle         = sub.billingCycle
                                            subscriptionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount
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

                // Payment Method
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

                // Status
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

                // Notes
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

                // Invoice info banner
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

                // Buttons
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
                                        billingCycle    = billingCycle,
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

// ── Reusable Form Field ───────────────────────────────────────────────────────

@Composable
private fun PayFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text(label, fontSize = 12.sp) },
        leadingIcon   = {
            Icon(icon, null,
                tint     = if (isError) RedText else TextSecondary,
                modifier = Modifier.size(18.dp))
        },
        isError        = isError,
        supportingText = {
            if (isError) Text(errorMessage, fontSize = 10.sp, color = RedText)
        },
        singleLine    = true,
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = NavyBlue,
            unfocusedBorderColor    = BorderColor,
            errorBorderColor        = RedText,
            focusedContainerColor   = CardBg,
            unfocusedContainerColor = CardBg
        )
    )
}