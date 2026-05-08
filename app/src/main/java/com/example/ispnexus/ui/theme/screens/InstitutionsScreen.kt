package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ispnexus.viewmodels.InstitutionActionState
import com.example.ispnexus.viewmodels.InstitutionsState
import com.example.ispnexus.viewmodels.InstitutionsViewModel

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue      = Color(0xFF1565C0)
private val LightBlue     = Color(0xFFE3F2FD)
private val PageBg        = Color(0xFFF4F6FB)
private val CardBg        = Color(0xFFFFFFFF)
private val BorderColor   = Color(0xFFE8EAF0)
private val DividerColor  = Color(0xFFF0F2F8)
private val TextPrimary   = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF90A4AE)
private val GreenText     = Color(0xFF2E7D32)
private val GreenBg       = Color(0xFFE8F5E9)
private val AmberText     = Color(0xFFF57F17)
private val AmberBg       = Color(0xFFFFF8E1)
private val RedText       = Color(0xFFC62828)
private val RedBg         = Color(0xFFFFEBEE)
private val PurpleText    = Color(0xFF6A1B9A)
private val PurpleBg      = Color(0xFFF3E5F5)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionsScreen(
    onBack: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: InstitutionsViewModel = viewModel()
) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val actionState  by viewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery  by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedInstitution by remember { mutableStateOf<Institution?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Handle action state side-effects ──────────────────────────────────────
    LaunchedEffect(actionState) {
        when (val a = actionState) {
            is InstitutionActionState.Success -> {
                snackbarHostState.showSnackbar("Done successfully")
                showAddDialog    = false
                showEditDialog   = false
                showDeleteDialog = false
                selectedInstitution = null
                viewModel.resetActionState()
            }
            is InstitutionActionState.Error -> {
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
                        Text("Institutions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Manage your institutions", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Institution")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { showAddDialog = true },
                containerColor    = NavyBlue,
                contentColor      = Color.White,
                shape             = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Institution")
            }
        }
    ) { padding ->

        when (val s = state) {
            is InstitutionsState.Loading -> {
                Box(
                    modifier           = Modifier.fillMaxSize().padding(padding),
                    contentAlignment   = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }

            is InstitutionsState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text      = s.message,
                            color     = RedText,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadInstitutions() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is InstitutionsState.Success -> {
                val filtered = viewModel.filteredInstitutions(s.institutions)

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Summary Cards ─────────────────────────────────────────
                    item {
                        Row(
                            modifier                = Modifier.fillMaxWidth(),
                            horizontalArrangement   = Arrangement.spacedBy(10.dp)
                        ) {
                            InstitutionSummaryCard(
                                label    = "Total",
                                value    = s.institutions.size.toString(),
                                icon     = Icons.Outlined.Business,
                                iconBg   = LightBlue,
                                iconTint = NavyBlue,
                                modifier = Modifier.weight(1f)
                            )
                            InstitutionSummaryCard(
                                label    = "Active",
                                value    = s.institutions.count { it.status == "active" }.toString(),
                                icon     = Icons.Outlined.CheckCircle,
                                iconBg   = GreenBg,
                                iconTint = GreenText,
                                modifier = Modifier.weight(1f)
                            )
                            InstitutionSummaryCard(
                                label    = "Suspended",
                                value    = s.institutions.count { it.status == "suspended" }.toString(),
                                icon     = Icons.Outlined.Block,
                                iconBg   = RedBg,
                                iconTint = RedText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── Search Bar ────────────────────────────────────────────
                    item {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier      = Modifier.fillMaxWidth(),
                            placeholder   = { Text("Search institutions...", fontSize = 13.sp) },
                            leadingIcon   = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = TextSecondary)
                            },
                            trailingIcon  = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear",
                                            tint = TextSecondary)
                                    }
                                }
                            },
                            shape         = RoundedCornerShape(12.dp),
                            singleLine    = true,
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = NavyBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor   = CardBg,
                                unfocusedContainerColor = CardBg
                            )
                        )
                    }

                    // ── Status Filter Chips ───────────────────────────────────
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("all", "active", "suspended", "pending").forEach { filter ->
                                FilterChip(
                                    selected = statusFilter == filter,
                                    onClick  = { viewModel.onStatusFilterChange(filter) },
                                    label    = {
                                        Text(
                                            text     = filter.replaceFirstChar { it.uppercase() },
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor    = NavyBlue,
                                        selectedLabelColor        = Color.White,
                                        containerColor            = CardBg,
                                        labelColor                = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled          = true,
                                        selected         = statusFilter == filter,
                                        borderColor      = BorderColor,
                                        selectedBorderColor = NavyBlue
                                    )
                                )
                            }
                        }
                    }

                    // ── List Header ───────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "${filtered.size} Institution${if (filtered.size != 1) "s" else ""}",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextPrimary
                            )
                            if (filtered.size != s.institutions.size) {
                                Text(
                                    text     = "Filtered",
                                    fontSize = 11.sp,
                                    color    = NavyBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Empty State ───────────────────────────────────────────
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier         = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(LightBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Business,
                                            contentDescription = null,
                                            tint     = NavyBlue,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text       = if (searchQuery.isNotEmpty()) "No results found" else "No institutions yet",
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = TextPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text      = if (searchQuery.isNotEmpty()) "Try a different search term" else "Tap + to add your first institution",
                                        fontSize  = 12.sp,
                                        color     = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ── Institution Cards ─────────────────────────────────────
                    items(filtered, key = { it.id }) { institution ->
                        InstitutionCard(
                            institution = institution,
                            onEdit      = {
                                selectedInstitution = institution
                                showEditDialog      = true
                            },
                            onSuspend   = {
                                viewModel.suspendInstitution(institution.id)
                            },
                            onReactivate = {
                                viewModel.reactivateInstitution(institution.id)
                            },
                            onDelete    = {
                                selectedInstitution = institution
                                showDeleteDialog    = true
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
        InstitutionFormDialog(
            title       = "Add Institution",
            institution = null,
            isLoading   = actionState is InstitutionActionState.Loading,
            onDismiss   = { showAddDialog = false },
            onConfirm   = { institution -> viewModel.addInstitution(institution) }
        )
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────────
    if (showEditDialog && selectedInstitution != null) {
        InstitutionFormDialog(
            title       = "Edit Institution",
            institution = selectedInstitution,
            isLoading   = actionState is InstitutionActionState.Loading,
            onDismiss   = {
                showEditDialog      = false
                selectedInstitution = null
            },
            onConfirm   = { institution -> viewModel.updateInstitution(institution) }
        )
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    if (showDeleteDialog && selectedInstitution != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog    = false
                selectedInstitution = null
            },
            containerColor = CardBg,
            shape          = RoundedCornerShape(16.dp),
            icon           = {
                Box(
                    modifier         = Modifier.size(52.dp).clip(CircleShape).background(RedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = RedText, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    text       = "Delete Institution",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            },
            text = {
                Text(
                    text      = "Are you sure you want to delete \"${selectedInstitution?.name}\"? This action cannot be undone.",
                    fontSize  = 13.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedInstitution?.let { viewModel.deleteInstitution(it.id) }
                    },
                    enabled = actionState !is InstitutionActionState.Loading,
                    colors  = ButtonDefaults.buttonColors(containerColor = RedText),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    if (actionState is InstitutionActionState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color    = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog    = false
                        selectedInstitution = null
                    },
                    shape  = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }
}

// ── Institution Card ──────────────────────────────────────────────────────────

@Composable
private fun InstitutionCard(
    institution: Institution,
    onEdit: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val initials = institution.name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()

    val (statusBg, statusColor) = when (institution.status.lowercase()) {
        "active"    -> GreenBg to GreenText
        "suspended" -> RedBg   to RedText
        "pending"   -> AmberBg to AmberText
        else        -> LightBlue to NavyBlue
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        border    = BorderStroke(0.5.dp, BorderColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Row 1: Avatar + Info + Status + Menu ──────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initials Avatar
                Box(
                    modifier         = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(LightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initials.ifEmpty { "IN" },
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NavyBlue
                    )
                }

                // Name + Plan
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = institution.name,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (institution.planName.isNotEmpty()) {
                        Text(
                            text     = institution.planName,
                            fontSize = 11.sp,
                            color    = NavyBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text     = institution.phoneNumber,
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }

                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = institution.status.replaceFirstChar { it.uppercase() },
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = statusColor
                    )
                }

                // Expand / Collapse toggle
                IconButton(
                    onClick  = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = TextSecondary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            // ── Expanded Details ──────────────────────────────────────────────
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // Details grid
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InstitutionDetailRow(Icons.Outlined.Email, "Email", institution.email)
                        InstitutionDetailRow(Icons.Outlined.LocationOn, "Address", institution.address)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InstitutionDetailRow(Icons.Outlined.Person, "Contact", institution.contactPersonName)
                        InstitutionDetailRow(Icons.Outlined.Phone, "Phone", institution.contactPersonPhone)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit
                    OutlinedButton(
                        onClick   = onEdit,
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(10.dp),
                        border    = BorderStroke(1.dp, NavyBlue),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null,
                            tint = NavyBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, color = NavyBlue)
                    }

                    // Suspend / Reactivate
                    if (institution.status.lowercase() == "active" || institution.status.lowercase() == "pending") {
                        OutlinedButton(
                            onClick        = onSuspend,
                            modifier       = Modifier.weight(1f),
                            shape          = RoundedCornerShape(10.dp),
                            border         = BorderStroke(1.dp, AmberText),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Block, contentDescription = null,
                                tint = AmberText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Suspend", fontSize = 12.sp, color = AmberText)
                        }
                    } else {
                        OutlinedButton(
                            onClick        = onReactivate,
                            modifier       = Modifier.weight(1f),
                            shape          = RoundedCornerShape(10.dp),
                            border         = BorderStroke(1.dp, GreenText),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                                tint = GreenText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Activate", fontSize = 12.sp, color = GreenText)
                        }
                    }

                    // Delete
                    OutlinedButton(
                        onClick        = onDelete,
                        modifier       = Modifier.weight(1f),
                        shape          = RoundedCornerShape(10.dp),
                        border         = BorderStroke(1.dp, RedText),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null,
                            tint = RedText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, color = RedText)
                    }
                }
            }
        }
    }
}

// ── Institution Detail Row ────────────────────────────────────────────────────

@Composable
private fun InstitutionDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(13.dp).padding(top = 1.dp)
        )
        Column {
            Text(label, fontSize = 9.sp, color = TextSecondary)
            Text(
                text     = value.ifEmpty { "—" },
                fontSize = 11.sp,
                color    = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Summary Card ──────────────────────────────────────────────────────────────

@Composable
private fun InstitutionSummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        border    = BorderStroke(0.5.dp, BorderColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = modifier
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ── Institution Form Dialog ───────────────────────────────────────────────────

@Composable
private fun InstitutionFormDialog(
    title: String,
    institution: Institution?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Institution) -> Unit
) {
    var name               by remember { mutableStateOf(institution?.name               ?: "") }
    var email              by remember { mutableStateOf(institution?.email              ?: "") }
    var phoneNumber        by remember { mutableStateOf(institution?.phoneNumber        ?: "") }
    var address            by remember { mutableStateOf(institution?.address            ?: "") }
    var contactPersonName  by remember { mutableStateOf(institution?.contactPersonName  ?: "") }
    var contactPersonPhone by remember { mutableStateOf(institution?.contactPersonPhone ?: "") }
    var planName           by remember { mutableStateOf(institution?.planName           ?: "") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(40.dp).clip(CircleShape).background(LightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (institution == null) Icons.Default.Add else Icons.Default.Edit,
                                contentDescription = null,
                                tint     = NavyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close",
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Form Fields
                FormField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = "Institution Name *",
                    icon          = Icons.Outlined.Business,
                    isError       = nameError,
                    errorMessage  = "Name is required"
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = "Email Address",
                    icon          = Icons.Outlined.Email
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label         = "Phone Number",
                    icon          = Icons.Outlined.Phone
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = address,
                    onValueChange = { address = it },
                    label         = "Address",
                    icon          = Icons.Outlined.LocationOn
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = contactPersonName,
                    onValueChange = { contactPersonName = it },
                    label         = "Contact Person Name",
                    icon          = Icons.Outlined.Person
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = contactPersonPhone,
                    onValueChange = { contactPersonPhone = it },
                    label         = "Contact Person Phone",
                    icon          = Icons.Outlined.Phone
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    value         = planName,
                    onValueChange = { planName = it },
                    label         = "Plan Name",
                    icon          = Icons.Outlined.Wifi
                )

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick   = onDismiss,
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(10.dp),
                        border    = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                nameError = true
                                return@Button
                            }
                            onConfirm(
                                Institution(
                                    id                  = institution?.id ?: "",
                                    name                = name.trim(),
                                    email               = email.trim(),
                                    phoneNumber         = phoneNumber.trim(),
                                    address             = address.trim(),
                                    contactPersonName   = contactPersonName.trim(),
                                    contactPersonPhone  = contactPersonPhone.trim(),
                                    companyId           = institution?.companyId ?: "",
                                    planId              = institution?.planId ?: "",
                                    planName            = planName.trim(),
                                    status              = institution?.status ?: "active",
                                    createdAt           = institution?.createdAt ?: 0L
                                )
                            )
                        },
                        enabled  = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (institution == null) "Add" else "Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable Form Field ───────────────────────────────────────────────────────

@Composable
private fun FormField(
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
                tint = if (isError) RedText else TextSecondary,
                modifier = Modifier.size(18.dp))
        },
        isError       = isError,
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
