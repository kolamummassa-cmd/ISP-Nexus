package com.example.ispnexus.ui.theme.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.DefaulterActionState
import com.example.ispnexus.viewmodels.DefaulterEntry
import com.example.ispnexus.viewmodels.DefaultersUiState
import com.example.ispnexus.viewmodels.DefaultersViewModel
import java.io.File
import java.io.FileOutputStream
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
private val OrangeText    = Color(0xFFE65100)
private val OrangeBg      = Color(0xFFFFF3E0)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return "Ksh ${fmt.format(amount)}"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "Never"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

// ── Severity colors ───────────────────────────────────────────────────────────

private fun severityColors(severity: String): Pair<Color, Color> = when (severity) {
    "Critical" -> RedText   to RedBg
    "High"     -> OrangeText to OrangeBg
    "Medium"   -> AmberText to AmberBg
    else       -> Color(0xFF1565C0) to LightBlue
}

// ── PDF Generator ─────────────────────────────────────────────────────────────

private fun generateDefaultersPdf(
    context: Context,
    defaulters: List<DefaulterEntry>,
    totalOwed: Double
): File? {
    return try {
        val pdfDoc   = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 200 + (defaulters.size * 90).coerceAtMost(900), 1).create()
        val page     = pdfDoc.startPage(pageInfo)
        val canvas   = page.canvas

        val titlePaint = Paint().apply {
            textSize       = 20f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#1565C0")
        }
        val headingPaint = Paint().apply {
            textSize       = 13f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#1A1A2E")
        }
        val labelPaint = Paint().apply {
            textSize = 10f
            color    = android.graphics.Color.parseColor("#90A4AE")
        }
        val valuePaint = Paint().apply {
            textSize = 11f
            color    = android.graphics.Color.parseColor("#1A1A2E")
        }
        val redPaint = Paint().apply {
            textSize       = 11f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#C62828")
        }
        val amberPaint = Paint().apply {
            textSize = 11f
            color    = android.graphics.Color.parseColor("#F57F17")
        }
        val linePaint = Paint().apply {
            color       = android.graphics.Color.parseColor("#E8EAF0")
            strokeWidth = 1f
        }
        val footerPaint = Paint().apply {
            textSize = 9f
            color    = android.graphics.Color.parseColor("#90A4AE")
        }

        var y = 50f

        // Header
        canvas.drawText("ISP NEXUS", 40f, y, titlePaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f
        canvas.drawText("DEFAULTERS REPORT  —  Overdue Subscriptions", 40f, y, headingPaint)
        canvas.drawText("Generated: ${formatDateTime(System.currentTimeMillis())}", 310f, y, labelPaint)
        y += 20f
        canvas.drawText("Total Amount Owed: ${formatKsh(totalOwed)}", 40f, y, redPaint)
        canvas.drawText("Total Defaulters: ${defaulters.size}", 360f, y, valuePaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Column headers
        canvas.drawText("Institution",    40f,  y, labelPaint)
        canvas.drawText("Plan",          200f,  y, labelPaint)
        canvas.drawText("Days Overdue",  310f,  y, labelPaint)
        canvas.drawText("Amount Owed",   400f,  y, labelPaint)
        canvas.drawText("Last Payment",  490f,  y, labelPaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        defaulters.forEach { entry ->
            canvas.drawText(entry.institution.name.take(22),     40f,  y, valuePaint)
            canvas.drawText(entry.subscription.planName.take(14), 200f, y, valuePaint)
            canvas.drawText("${entry.daysOverdue}d",             310f,  y, redPaint)
            canvas.drawText(formatKsh(entry.amountOwed),         400f,  y, amberPaint)
            canvas.drawText(formatDate(entry.lastPaymentDate),   490f,  y, valuePaint)
            y += 14f

            // Contact person sub-row
            val contact = "${entry.institution.contactPersonName}  ·  ${entry.institution.contactPersonPhone}"
            canvas.drawText(contact, 40f, y, labelPaint)
            y += 18f

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 10f
        }

        y += 10f
        canvas.drawText(
            "ISP Nexus  ·  This report lists all institutions with overdue subscriptions.",
            40f, y, footerPaint
        )

        pdfDoc.finishPage(page)

        val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val name = "ISPNexus_Defaulters_${SimpleDateFormat("yyyyMMdd_HHmm",
            Locale.getDefault()).format(Date())}.pdf"
        val file = File(dir, name)
        pdfDoc.writeTo(FileOutputStream(file))
        pdfDoc.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultersScreen(
    onMenuClick: () -> Unit = {},
    viewModel: DefaultersViewModel = viewModel()
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showReminderDialog  by remember { mutableStateOf(false) }
    var showResolveDialog   by remember { mutableStateOf(false) }
    var selectedEntry       by remember { mutableStateOf<DefaulterEntry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    // ── Side-effects ──────────────────────────────────────────────────────────
    LaunchedEffect(actionState) {
        when (val a = actionState) {
            is DefaulterActionState.Success -> {
                snackbarHostState.showSnackbar("Done successfully")
                showReminderDialog = false
                showResolveDialog  = false
                selectedEntry      = null
                viewModel.resetActionState()
            }
            is DefaulterActionState.Error -> {
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
                        Text("Defaulters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Overdue subscriptions", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    if (state is DefaultersUiState.Success) {
                        IconButton(onClick = {
                            val defaulters = (state as DefaultersUiState.Success).defaulters
                            val file = generateDefaultersPdf(
                                context    = context,
                                defaulters = defaulters,
                                totalOwed  = viewModel.totalAmountOwed(defaulters)
                            )
                            if (file != null) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context, "${context.packageName}.provider", file
                                )
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW
                                ).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(intent, "Open Defaulters PDF")
                                )
                            }
                        }) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->

        when (val s = state) {
            is DefaultersUiState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = NavyBlue) }
            }

            is DefaultersUiState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadDefaulters() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) { Text("Retry") }
                    }
                }
            }

            is DefaultersUiState.Success -> {
                val filtered   = viewModel.filteredDefaulters(s.defaulters)
                val totalOwed  = viewModel.totalAmountOwed(s.defaulters)
                val critical   = viewModel.criticalCount(s.defaulters)

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Summary Banner ────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total defaulters
                            Card(
                                modifier  = Modifier.weight(1f),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = RedBg),
                                elevation = CardDefaults.cardElevation(1.dp),
                                border    = BorderStroke(1.dp, RedText.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier         = Modifier.size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RedText.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Warning, null,
                                            tint     = RedText,
                                            modifier = Modifier.size(17.dp))
                                    }
                                    Text(s.defaulters.size.toString(), fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold, color = RedText)
                                    Text("Total Defaulters", fontSize = 10.sp, color = RedText.copy(alpha = 0.7f))
                                    if (critical > 0) {
                                        Text("$critical critical (30+ days)", fontSize = 9.sp,
                                            color = RedText.copy(alpha = 0.6f))
                                    }
                                }
                            }

                            // Total owed
                            Card(
                                modifier  = Modifier.weight(1f),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = NavyBlue),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier         = Modifier.size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.AttachMoney, null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(17.dp))
                                    }
                                    Text(
                                        text       = formatKsh(totalOwed),
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                    Text("Total Owed", fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.75f))
                                    Text("From overdue subscriptions", fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.55f))
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
                            placeholder   = { Text("Search institution or contact...", fontSize = 13.sp) },
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

                    // ── Count ─────────────────────────────────────────────────
                    item {
                        Text(
                            "${filtered.size} defaulter${if (filtered.size != 1) "s" else ""}",
                            fontSize = 12.sp, color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    // ── Empty State ───────────────────────────────────────────
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier         = Modifier.size(64.dp)
                                            .clip(CircleShape)
                                            .background(GreenBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null,
                                            tint     = GreenText,
                                            modifier = Modifier.size(34.dp))
                                    }
                                    Text("No defaulters!", fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold, color = GreenText)
                                    Text(
                                        if (searchQuery.isEmpty())
                                            "All subscriptions are up to date."
                                        else
                                            "No results for \"$searchQuery\"",
                                        fontSize  = 12.sp,
                                        color     = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ── Defaulter Cards ───────────────────────────────────────
                    items(filtered, key = { it.subscription.id }) { entry ->
                        DefaulterCard(
                            entry    = entry,
                            severity = viewModel.severity(entry.daysOverdue),
                            onRemind = { selectedEntry = entry; showReminderDialog = true },
                            onResolve = { selectedEntry = entry; showResolveDialog = true }
                        )
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── Reminder Dialog ───────────────────────────────────────────────────────
    if (showReminderDialog && selectedEntry != null) {
        ReminderDialog(
            entry     = selectedEntry!!,
            isLoading = actionState is DefaulterActionState.Loading,
            onDismiss = { showReminderDialog = false; selectedEntry = null },
            onConfirm = { subscriptionId, note ->
                viewModel.sendReminder(subscriptionId, note)
            }
        )
    }

    // ── Resolve Confirm Dialog ────────────────────────────────────────────────
    if (showResolveDialog && selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false; selectedEntry = null },
            icon  = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenText)
            },
            title = { Text("Mark as Resolved", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "This will reactivate ${selectedEntry?.institution?.name}'s subscription. " +
                            "Only do this once payment has been confirmed.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedEntry?.let { viewModel.markResolved(it.subscription.id) } },
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenText),
                    enabled = actionState !is DefaulterActionState.Loading
                ) {
                    if (actionState is DefaulterActionState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Mark Resolved", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResolveDialog = false; selectedEntry = null },
                    border  = BorderStroke(1.dp, BorderColor)
                ) { Text("Cancel", color = TextPrimary) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Defaulter Card ────────────────────────────────────────────────────────────

@Composable
private fun DefaulterCard(
    entry: DefaulterEntry,
    severity: String,
    onRemind: () -> Unit,
    onResolve: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (sevColor, sevBg) = severityColors(severity)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border    = BorderStroke(1.5.dp, sevColor.copy(alpha = 0.25f))
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
                        modifier         = Modifier.size(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(sevBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = entry.institution.name.take(1).uppercase(),
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = sevColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = entry.institution.name,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = entry.subscription.planName.ifEmpty { "No plan" },
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Severity badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(sevBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(severity, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, color = sevColor)
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
                                text        = { Text("Send Reminder", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Outlined.NotificationsActive, null,
                                    modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onRemind() }
                            )
                            DropdownMenuItem(
                                text        = { Text("Mark Resolved", fontSize = 13.sp,
                                    color = GreenText) },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null,
                                    tint = GreenText, modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onResolve() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(Modifier.height(12.dp))

            // ── Stats Row ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DefaulterStat(
                    icon    = Icons.Outlined.Timer,
                    label   = "Days Overdue",
                    value   = "${entry.daysOverdue}d",
                    color   = sevColor
                )
                DefaulterStat(
                    icon    = Icons.Outlined.AttachMoney,
                    label   = "Amount Owed",
                    value   = formatKsh(entry.amountOwed),
                    color   = TextPrimary
                )
                DefaulterStat(
                    icon    = Icons.Outlined.CalendarToday,
                    label   = "Last Payment",
                    value   = formatDate(entry.lastPaymentDate),
                    color   = TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(Modifier.height(10.dp))

            // ── Contact Row ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Person, null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(14.dp))
                Text(
                    text     = entry.institution.contactPersonName.ifEmpty { "—" },
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
                Text("·", fontSize = 12.sp, color = TextSecondary)
                Icon(Icons.Outlined.Phone, null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(14.dp))
                Text(
                    text     = entry.institution.contactPersonPhone.ifEmpty { "—" },
                    fontSize = 12.sp,
                    color    = NavyBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Action Buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onRemind,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, NavyBlue)
                ) {
                    Icon(Icons.Outlined.NotificationsActive, null,
                        tint = NavyBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remind", fontSize = 12.sp, color = NavyBlue)
                }
                Button(
                    onClick  = onResolve,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenText)
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Resolved", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

// ── Stat Item ─────────────────────────────────────────────────────────────────

@Composable
private fun DefaulterStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            Text(label, fontSize = 9.sp, color = TextSecondary)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Reminder Dialog ───────────────────────────────────────────────────────────

@Composable
private fun ReminderDialog(
    entry: DefaulterEntry,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var note by remember {
        mutableStateOf(
            "Dear ${entry.institution.contactPersonName}, " +
                    "this is a reminder that your subscription (${entry.subscription.planName}) " +
                    "is ${entry.daysOverdue} day(s) overdue. " +
                    "Please make a payment of ${
                        NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
                            .format(entry.amountOwed).let { "Ksh $it" }
                    } at your earliest convenience."
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                            Icon(Icons.Outlined.NotificationsActive, null,
                                tint = NavyBlue, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Send Reminder", fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(entry.institution.name, fontSize = 11.sp, color = TextSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close",
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Contact info banner
                Row(
                    modifier              = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightBlue)
                        .padding(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Phone, null,
                        tint = NavyBlue, modifier = Modifier.size(14.dp))
                    Text(
                        text     = entry.institution.contactPersonPhone.ifEmpty { "No phone on record" },
                        fontSize = 12.sp,
                        color    = NavyBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Reminder note field
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Reminder Note", fontSize = 12.sp) },
                    leadingIcon   = {
                        Icon(Icons.Outlined.Notes, null,
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    minLines = 4,
                    maxLines = 7,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = NavyBlue,
                        unfocusedBorderColor    = BorderColor,
                        focusedContainerColor   = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )

                // Buttons
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
                        onClick  = { onConfirm(entry.subscription.id, note) },
                        enabled  = !isLoading && note.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save Note", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
