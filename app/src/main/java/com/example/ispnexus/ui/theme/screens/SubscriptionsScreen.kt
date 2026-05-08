package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ispnexus.models.Subscription
import com.example.ispnexus.viewmodels.SubscriptionActionState
import com.example.ispnexus.viewmodels.SubscriptionsState
import com.example.ispnexus.viewmodels.SubscriptionsViewModel
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return "Ksh ${fmt.format(amount)}"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: SubscriptionsViewModel = viewModel()
) {
    val state              by viewModel.state.collectAsStateWithLifecycle()
    val actionState        by viewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery        by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter       by viewModel.statusFilter.collectAsStateWithLifecycle()
    val billingCycleFilter by viewModel.billingCycleFilter.collectAsStateWithLifecycle()

    var showAddDialog      by remember { mutableStateOf(false) }
    var showEditDialog     by remember { mutableStateOf(false) }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var selectedSub        by remember { mutableStateOf<Subscription?>(null) }

    val snackbarHostState  = remember { SnackbarHostState() }

    // ── Side-effects ──────────────────────────────────────────────────────────
    LaunchedEffect(actionState) {
        when (val a = actionState) {
            is SubscriptionActionState.Success -> {
                snackbarHostState.showSnackbar("Done successfully")
                showAddDialog    = false
                showEditDialog   = false
                showDeleteDialog = false
                selectedSub      = null
                viewModel.resetActionState()
            }
            is SubscriptionActionState.Error -> {
                snackbarHostState.showSnackbar(a.message)
                viewModel.resetActionState()
            }
            else -> Unit
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
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text("Subscriptions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Manage billing & plans", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subscription")
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
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { padding ->

        when (val s = state) {
            is SubscriptionsState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = NavyBlue) }
            }

            is SubscriptionsState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadSubscriptions() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) { Text("Retry") }
                    }
                }
            }

            is SubscriptionsState.Success -> {
                val filtered = viewModel.filteredSubscriptions(s.subscriptions)

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
                            SubSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Active",
                                value    = viewModel.totalActive(s.subscriptions).toString(),
                                color    = GreenText,
                                bgColor  = GreenBg,
                                icon     = Icons.Default.CheckCircle
                            )
                            SubSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Suspended",
                                value    = viewModel.totalSuspended(s.subscriptions).toString(),
                                color    = AmberText,
                                bgColor  = AmberBg,
                                icon     = Icons.Default.Pause
                            )
                            SubSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Expired",
                                value    = viewModel.totalExpired(s.subscriptions).toString(),
                                color    = RedText,
                                bgColor  = RedBg,
                                icon     = Icons.Default.Cancel
                            )
                        }
                    }

                    // ── Monthly Revenue Card ───────────────────────────────────
                    item {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            colors    = CardDefaults.cardColors(containerColor = NavyBlue),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Monthly Revenue", fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f))
                                    Text(
                                        text       = formatKsh(viewModel.totalMonthlyRevenue(s.subscriptions)),
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                    Text("From active subscriptions", fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.60f))
                                }
                                Box(
                                    modifier         = Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }

                    // ── Search Bar ────────────────────────────────────────────
                    item {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier      = Modifier.fillMaxWidth(),
                            placeholder   = { Text("Search institution or plan...", fontSize = 13.sp) },
                            leadingIcon   = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear",
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
                            val statuses = listOf("all", "active", "suspended", "expired")
                            items(statuses) { status ->
                                FilterChip(
                                    selected = statusFilter == status,
                                    onClick  = { viewModel.onStatusFilterChange(status) },
                                    label    = { Text(status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor    = NavyBlue,
                                        selectedLabelColor        = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ── Billing Cycle Filter Chips ────────────────────────────
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cycles = listOf("all", "monthly", "yearly")
                            items(cycles) { cycle ->
                                FilterChip(
                                    selected = billingCycleFilter == cycle,
                                    onClick  = { viewModel.onBillingCycleFilterChange(cycle) },
                                    label    = { Text(cycle.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
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
                            text      = "${filtered.size} subscription${if (filtered.size != 1) "s" else ""}",
                            fontSize  = 12.sp,
                            color     = TextSecondary,
                            modifier  = Modifier.padding(horizontal = 2.dp)
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
                                    Icon(
                                        Icons.Outlined.Subscriptions,
                                        contentDescription = null,
                                        tint     = TextSecondary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("No subscriptions found", fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Text("Tap + to add a new subscription", fontSize = 12.sp,
                                        color = TextSecondary.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // ── Subscription Cards ────────────────────────────────────
                    items(filtered, key = { it.id }) { sub ->
                        SubscriptionCard(
                            subscription = sub,
                            onEdit = {
                                selectedSub    = sub
                                showEditDialog = true
                            },
                            onSuspend    = { viewModel.suspendSubscription(sub.id) },
                            onReactivate = { viewModel.reactivateSubscription(sub.id) },
                            onExpire     = { viewModel.expireSubscription(sub.id) },
                            onDelete = {
                                selectedSub      = sub
                                showDeleteDialog = true
                            }
                        )
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── Add Dialog ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        val institutions by viewModel.institutions.collectAsStateWithLifecycle()
        val plans        by viewModel.plans.collectAsStateWithLifecycle()
        SubscriptionFormDialog(
            subscription  = null,
            isLoading     = actionState is SubscriptionActionState.Loading,
            institutions  = institutions,
            plans         = plans,
            onDismiss     = { showAddDialog = false },
            onConfirm     = { viewModel.addSubscription(it) }
        )
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────────
    if (showEditDialog && selectedSub != null) {
        val institutions by viewModel.institutions.collectAsStateWithLifecycle()
        val plans        by viewModel.plans.collectAsStateWithLifecycle()
        SubscriptionFormDialog(
            subscription  = selectedSub,
            isLoading     = actionState is SubscriptionActionState.Loading,
            institutions  = institutions,
            plans         = plans,
            onDismiss     = { showEditDialog = false; selectedSub = null },
            onConfirm     = { viewModel.updateSubscription(it) }
        )
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    if (showDeleteDialog && selectedSub != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; selectedSub = null },
            icon             = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedText)
            },
            title = { Text("Delete Subscription", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Are you sure you want to delete the subscription for " +
                            "${selectedSub?.institutionName}? This action cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedSub?.let { viewModel.deleteSubscription(it.id) } },
                    colors  = ButtonDefaults.buttonColors(containerColor = RedText),
                    enabled = actionState !is SubscriptionActionState.Loading
                ) {
                    if (actionState is SubscriptionActionState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false; selectedSub = null },
                    border  = BorderStroke(1.dp, BorderColor)
                ) { Text("Cancel", color = TextPrimary) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Subscription Card ─────────────────────────────────────────────────────────

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    onEdit: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onExpire: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val (statusText, statusColor, statusBg) = when (subscription.status) {
        "active"    -> Triple("Active",    GreenText, GreenBg)
        "suspended" -> Triple("Suspended", AmberText, AmberBg)
        "expired"   -> Triple("Expired",   RedText,   RedBg)
        else        -> Triple(subscription.status, GreyText, GreyBg)
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
                        Text(
                            text       = subscription.institutionName.take(1).uppercase(),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = NavyBlue
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = subscription.institutionName,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = subscription.planName,
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = statusColor)
                    }
                    // Menu
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null,
                                    modifier = Modifier.size(16.dp)) },
                                onClick = { showMenu = false; onEdit() }
                            )
                            if (subscription.status != "active") {
                                DropdownMenuItem(
                                    text = { Text("Reactivate", fontSize = 13.sp, color = GreenText) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null,
                                        tint = GreenText, modifier = Modifier.size(16.dp)) },
                                    onClick = { showMenu = false; onReactivate() }
                                )
                            }
                            if (subscription.status == "active") {
                                DropdownMenuItem(
                                    text = { Text("Suspend", fontSize = 13.sp, color = AmberText) },
                                    leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null,
                                        tint = AmberText, modifier = Modifier.size(16.dp)) },
                                    onClick = { showMenu = false; onSuspend() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mark Expired", fontSize = 13.sp, color = RedText) },
                                    leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null,
                                        tint = RedText, modifier = Modifier.size(16.dp)) },
                                    onClick = { showMenu = false; onExpire() }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", fontSize = 13.sp, color = RedText) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null,
                                    tint = RedText, modifier = Modifier.size(16.dp)) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(Modifier.height(12.dp))

            // ── Info Row ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubInfoChip(
                    icon  = Icons.Outlined.AttachMoney,
                    label = formatKsh(subscription.amountKsh)
                )
                SubInfoChip(
                    icon  = Icons.Outlined.Refresh,
                    label = subscription.billingCycle.replaceFirstChar { it.uppercase() }
                )
                SubInfoChip(
                    icon  = Icons.Outlined.CalendarToday,
                    label = formatDate(subscription.startDate)
                )
            }

            // ── Expand: end date ──────────────────────────────────────────────
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.EventBusy, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text("End Date: ${formatDate(subscription.endDate)}",
                        fontSize = 12.sp, color = TextSecondary)
                }
            }

            // ── Expand toggle ─────────────────────────────────────────────────
            Row(
                modifier  = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = if (expanded) "Show less" else "Show more",
                    fontSize = 11.sp,
                    color    = NavyBlue
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint     = NavyBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Info Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun SubInfoChip(icon: ImageVector, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null,
            tint = TextSecondary, modifier = Modifier.size(13.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Summary Card ──────────────────────────────────────────────────────────────

@Composable
private fun SubSummaryCard(
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
            modifier             = Modifier.padding(12.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

// ── Form Dialog ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionFormDialog(
    subscription: Subscription?,
    isLoading: Boolean,
    institutions: List<com.example.ispnexus.models.Institution>,
    plans: List<com.example.ispnexus.models.Plan>,
    onDismiss: () -> Unit,
    onConfirm: (Subscription) -> Unit
) {
    var institutionName by remember { mutableStateOf(subscription?.institutionName ?: "") }
    var institutionId   by remember { mutableStateOf(subscription?.institutionId ?: "") }
    var planName        by remember { mutableStateOf(subscription?.planName ?: "") }
    var planId          by remember { mutableStateOf(subscription?.planId ?: "") }
    var amountKsh       by remember { mutableStateOf(
        if (subscription != null && subscription.amountKsh > 0)
            subscription.amountKsh.toInt().toString() else ""
    )}
    var billingCycle    by remember { mutableStateOf(subscription?.billingCycle ?: "monthly") }
    var status          by remember { mutableStateOf(subscription?.status ?: "active") }
    var startDate       by remember { mutableStateOf(subscription?.startDate ?: System.currentTimeMillis()) }
    var endDate         by remember { mutableStateOf(subscription?.endDate ?: 0L) }

    var nameError           by remember { mutableStateOf(false) }
    var amountError         by remember { mutableStateOf(false) }
    var institutionExpanded by remember { mutableStateOf(false) }
    var planExpanded        by remember { mutableStateOf(false) }

    val title = if (subscription == null) "Add Subscription" else "Edit Subscription"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            LazyColumn(
                modifier       = Modifier.padding(20.dp).fillMaxWidth(),
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
                                    if (subscription == null) Icons.Default.Add else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint     = NavyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = TextPrimary)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Institution Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded         = institutionExpanded,
                        onExpandedChange = { institutionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = institutionName.ifEmpty { "Select Institution" },
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Institution *", fontSize = 12.sp) },
                            leadingIcon   = {
                                Icon(Icons.Outlined.Business, null,
                                    tint     = if (nameError) RedText else TextSecondary,
                                    modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = institutionExpanded)
                            },
                            isError        = nameError,
                            supportingText = {
                                if (nameError) Text("Select an institution", fontSize = 10.sp, color = RedText)
                            },
                            shape    = RoundedCornerShape(10.dp),
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = NavyBlue,
                                unfocusedBorderColor    = BorderColor,
                                errorBorderColor        = RedText,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded         = institutionExpanded,
                            onDismissRequest = { institutionExpanded = false }
                        ) {
                            if (institutions.isEmpty()) {
                                DropdownMenuItem(
                                    text    = { Text("No institutions found", fontSize = 12.sp, color = TextSecondary) },
                                    onClick = {}
                                )
                            } else {
                                institutions.forEach { inst ->
                                    DropdownMenuItem(
                                        text    = { Text(inst.name, fontSize = 13.sp) },
                                        onClick = {
                                            institutionName     = inst.name
                                            institutionId       = inst.id
                                            nameError           = false
                                            institutionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Plan Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded         = planExpanded,
                        onExpandedChange = { planExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = planName.ifEmpty { "Select Plan" },
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Plan *", fontSize = 12.sp) },
                            leadingIcon   = {
                                Icon(Icons.Outlined.Wifi, null,
                                    tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded)
                            },
                            shape    = RoundedCornerShape(10.dp),
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = NavyBlue,
                                unfocusedBorderColor    = BorderColor,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded         = planExpanded,
                            onDismissRequest = { planExpanded = false }
                        ) {
                            if (plans.isEmpty()) {
                                DropdownMenuItem(
                                    text    = { Text("No plans found", fontSize = 12.sp, color = TextSecondary) },
                                    onClick = {}
                                )
                            } else {
                                plans.forEach { plan ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(plan.name, fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold)
                                                Text("${plan.speedMbps} Mbps • Ksh ${plan.price.toInt()}",
                                                    fontSize = 11.sp, color = TextSecondary)
                                            }
                                        },
                                        onClick = {
                                            planName     = plan.name
                                            planId       = plan.id
                                            amountKsh    = plan.price.toInt().toString()
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount
//                item {
//                    SubFormField(
//                        value         = amountKsh,
//                        onValueChange = { amountKsh = it; amountError = false },
//                        label         = "Amount (Ksh) *",
//                        icon          = Icons.Outlined.AttachMoney,
//                        isError       = amountError,
//                        errorMessage  = "Enter a valid amount"
//                    )
//                }

                // Billing Cycle
                item {
                    Text("Billing Cycle", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("monthly", "yearly").forEach { cycle ->
                            FilterChip(
                                selected = billingCycle == cycle,
                                onClick  = { billingCycle = cycle },
                                label    = { Text(cycle.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyBlue,
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }

                // Status
                item {
                    Text("Status", fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("active", "suspended", "expired").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick  = { status = s },
                                label    = { Text(s.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (s) {
                                        "active"    -> GreenText
                                        "suspended" -> AmberText
                                        else        -> RedText
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Buttons
                item {
                    Spacer(Modifier.height(6.dp))
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
                                if (institutionName.trim().isEmpty()) {
                                    nameError = true; return@Button
                                }
                                if (amount == null || amount <= 0) {
                                    amountError = true; return@Button
                                }
                                onConfirm(
                                    Subscription(
                                        id              = subscription?.id ?: "",
                                        companyId       = subscription?.companyId ?: "",
                                        institutionId   = institutionId.trim(),
                                        institutionName = institutionName.trim(),
                                        planId          = planId.trim(),
                                        planName        = planName.trim(),
                                        amountKsh       = amount,
                                        billingCycle    = billingCycle,
                                        status          = status,
                                        startDate       = startDate,
                                        endDate         = endDate,
                                        createdAt       = subscription?.createdAt ?: 0L
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
                                Text(if (subscription == null) "Add" else "Save", color = Color.White)
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
private fun SubFormField(
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
            Icon(icon, contentDescription = null,
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