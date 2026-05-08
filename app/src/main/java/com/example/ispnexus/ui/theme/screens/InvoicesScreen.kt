package com.example.ispnexus.ui.theme.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.models.Invoice
import com.example.ispnexus.viewmodels.InvoiceActionState
import com.example.ispnexus.viewmodels.InvoicesUiState
import com.example.ispnexus.viewmodels.InvoicesViewModel
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

private fun formatDateTime(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
}

// ── PDF Generator ─────────────────────────────────────────────────────────────

private fun generateInvoicePdf(context: Context, invoice: Invoice): File? {
    return try {
        val pdfDoc  = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page    = pdfDoc.startPage(pageInfo)
        val canvas  = page.canvas

        val titlePaint = Paint().apply {
            textSize  = 22f
            isFakeBoldText = true
            color     = android.graphics.Color.parseColor("#1565C0")
        }
        val labelPaint = Paint().apply {
            textSize = 11f
            color    = android.graphics.Color.parseColor("#90A4AE")
        }
        val valuePaint = Paint().apply {
            textSize = 12f
            color    = android.graphics.Color.parseColor("#1A1A2E")
        }
        val boldPaint = Paint().apply {
            textSize       = 13f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#1A1A2E")
        }
        val amountPaint = Paint().apply {
            textSize       = 20f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#1565C0")
        }
        val linePaint = Paint().apply {
            color       = android.graphics.Color.parseColor("#E8EAF0")
            strokeWidth = 1f
        }
        val statusColor = when (invoice.status) {
            "paid", "completed" -> android.graphics.Color.parseColor("#2E7D32")
            "failed"            -> android.graphics.Color.parseColor("#C62828")
            else                -> android.graphics.Color.parseColor("#F57F17")
        }
        val statusPaint = Paint().apply {
            textSize       = 12f
            isFakeBoldText = true
            color          = statusColor
        }

        var y = 60f

        // Header
        canvas.drawText("ISP NEXUS", 40f, y, titlePaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 28f

        canvas.drawText("INVOICE", 40f, y, boldPaint.apply { textSize = 18f })
        canvas.drawText(invoice.invoiceNumber, 400f, y, titlePaint.apply { textSize = 14f })
        y += 28f

        // Status badge
        val statusLabel = when (invoice.status) {
            "paid", "completed" -> "PAID"
            "failed"            -> "FAILED"
            else                -> "PENDING"
        }
        canvas.drawText(statusLabel, 400f, y, statusPaint)
        y += 30f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        // Bill to
        canvas.drawText("BILL TO", 40f, y, labelPaint)
        y += 18f
        canvas.drawText(invoice.institutionName, 40f, y, boldPaint.apply { textSize = 14f })
        y += 30f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        // Fields
        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 40f, y, labelPaint)
            canvas.drawText(value, 200f, y, valuePaint)
            y += 22f
        }

        drawRow("Invoice Number:",  invoice.invoiceNumber)
        drawRow("Issued Date:",     formatDate(invoice.issuedAt))
        drawRow("Payment Method:",  invoice.paymentMethod.ifEmpty { "—" })
        drawRow("Subscription ID:", invoice.subscriptionId.ifEmpty { "—" })
        if (invoice.status == "paid" || invoice.status == "completed") {
            drawRow("Paid On:", formatDateTime(invoice.paidAt))
        }
        if (invoice.notes.isNotEmpty()) {
            drawRow("Notes:", invoice.notes)
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        // Amount
        canvas.drawText("TOTAL AMOUNT", 40f, y, labelPaint)
        canvas.drawText(formatKsh(invoice.amountKsh), 300f, y, amountPaint)
        y += 40f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        // Footer
        val footerPaint = Paint().apply {
            textSize = 10f
            color    = android.graphics.Color.parseColor("#90A4AE")
        }
        canvas.drawText("Generated by ISP Nexus  ·  ${formatDateTime(System.currentTimeMillis())}",
            40f, y, footerPaint)

        pdfDoc.finishPage(page)

        val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, "${invoice.invoiceNumber}.pdf")
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
fun InvoicesScreen(
    onMenuClick: () -> Unit = {},
    viewModel: InvoicesViewModel = viewModel(),
    onBack: () -> Boolean
) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val actionState  by viewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery  by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    var showDetailDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var selectedInvoice  by remember { mutableStateOf<Invoice?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    // ── Side-effects ──────────────────────────────────────────────────────────
    LaunchedEffect(actionState) {
        when (val a = actionState) {
            is InvoiceActionState.Success -> {
                snackbarHostState.showSnackbar("Notes updated successfully")
                showEditDialog = false
                viewModel.resetActionState()
            }
            is InvoiceActionState.Error -> {
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
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text("Invoices", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Auto-generated from payments", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                }
            )
        }
    ) { padding ->

        when (val s = state) {
            is InvoicesUiState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = NavyBlue) }
            }

            is InvoicesUiState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadInvoices() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) { Text("Retry") }
                    }
                }
            }

            is InvoicesUiState.Success -> {
                val filtered = viewModel.filteredInvoices(s.invoices)

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
                            InvSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Paid",
                                value    = viewModel.totalPaid(s.invoices).toString(),
                                color    = GreenText,
                                bgColor  = GreenBg,
                                icon     = Icons.Default.CheckCircle
                            )
                            InvSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Pending",
                                value    = viewModel.totalPending(s.invoices).toString(),
                                color    = AmberText,
                                bgColor  = AmberBg,
                                icon     = Icons.Default.HourglassEmpty
                            )
                            InvSummaryCard(
                                modifier = Modifier.weight(1f),
                                label    = "Failed",
                                value    = viewModel.totalFailed(s.invoices).toString(),
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
                                    Text("Total Collected", fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.75f))
                                    Text(
                                        formatKsh(viewModel.totalPaidAmount(s.invoices)),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                    Text("From paid invoices", fontSize = 9.sp,
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
                                        formatKsh(viewModel.totalOutstandingAmount(s.invoices)),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = AmberText
                                    )
                                    Text("Pending invoices", fontSize = 9.sp,
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
                            placeholder   = { Text("Search institution or invoice no...", fontSize = 13.sp) },
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
                            val statuses = listOf("all", "paid", "pending", "failed")
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
                                            "paid"    -> GreenText
                                            "pending" -> AmberText
                                            "failed"  -> RedText
                                            else      -> NavyBlue
                                        },
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ── Count ─────────────────────────────────────────────────
                    item {
                        Text(
                            "${filtered.size} invoice${if (filtered.size != 1) "s" else ""}",
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
                                    Icon(Icons.Outlined.Receipt, null,
                                        tint = TextSecondary, modifier = Modifier.size(52.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No invoices found", fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Text("Invoices are generated automatically when a payment is recorded.",
                                        fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier  = Modifier.padding(horizontal = 32.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // ── Invoice Cards ─────────────────────────────────────────
                    items(filtered, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice      = invoice,
                            displayStatus = viewModel.displayStatus(invoice),
                            onView       = { selectedInvoice = invoice; showDetailDialog = true },
                            onEditNotes  = { selectedInvoice = invoice; showEditDialog = true },
                            onDownload   = {
                                val file = generateInvoicePdf(context, invoice)
                                // share or open the file
                                if (file != null) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(intent, "Open Invoice PDF")
                                    )
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── Detail Dialog ─────────────────────────────────────────────────────────
    if (showDetailDialog && selectedInvoice != null) {
        InvoiceDetailDialog(
            invoice       = selectedInvoice!!,
            displayStatus = viewModel.displayStatus(selectedInvoice!!),
            onDismiss     = { showDetailDialog = false; selectedInvoice = null },
            onEditNotes   = { showDetailDialog = false; showEditDialog = true },
            onDownload    = {
                val file = generateInvoicePdf(context, selectedInvoice!!)
                if (file != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.provider", file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Open Invoice PDF"))
                }
            }
        )
    }

    // ── Edit Notes Dialog ─────────────────────────────────────────────────────
    if (showEditDialog && selectedInvoice != null) {
        EditNotesDialog(
            invoice   = selectedInvoice!!,
            isLoading = actionState is InvoiceActionState.Loading,
            onDismiss = { showEditDialog = false; selectedInvoice = null },
            onConfirm = { invoiceId, notes -> viewModel.updateNotes(invoiceId, notes) }
        )
    }
}

// ── Invoice Card ──────────────────────────────────────────────────────────────

@Composable
private fun InvoiceCard(
    invoice: Invoice,
    displayStatus: String,
    onView: () -> Unit,
    onEditNotes: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val (statusColor, statusBg) = when (invoice.status) {
        "paid", "completed" -> GreenText to GreenBg
        "pending"           -> AmberText to AmberBg
        "failed"            -> RedText   to RedBg
        else                -> GreyText  to GreyBg
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onView() },
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
                        Icon(Icons.Outlined.Receipt, null,
                            tint     = NavyBlue,
                            modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = invoice.invoiceNumber,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = NavyBlue,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = invoice.institutionName,
                            fontSize = 12.sp,
                            color    = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = formatKsh(invoice.amountKsh),
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
                            Text(displayStatus, fontSize = 10.sp,
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
                                text        = { Text("View Details", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Visibility, null,
                                    modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onView() }
                            )
                            DropdownMenuItem(
                                text        = { Text("Edit Notes", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Edit, null,
                                    modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onEditNotes() }
                            )
                            DropdownMenuItem(
                                text        = { Text("Download PDF", fontSize = 13.sp,
                                    color = NavyBlue) },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null,
                                    tint = NavyBlue, modifier = Modifier.size(16.dp)) },
                                onClick     = { showMenu = false; onDownload() }
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
                InvInfoChip(
                    icon  = Icons.Outlined.CalendarToday,
                    label = formatDate(invoice.issuedAt)
                )
                InvInfoChip(
                    icon  = Icons.Outlined.Payment,
                    label = invoice.paymentMethod.ifEmpty { "—" }
                )
                InvInfoChip(
                    icon  = if (invoice.status == "paid" || invoice.status == "completed")
                        Icons.Outlined.EventAvailable else Icons.Outlined.EventBusy,
                    label = if (invoice.status == "paid" || invoice.status == "completed")
                        formatDate(invoice.paidAt) else "Not paid"
                )
            }
        }
    }
}

// ── Invoice Detail Dialog ─────────────────────────────────────────────────────

@Composable
private fun InvoiceDetailDialog(
    invoice: Invoice,
    displayStatus: String,
    onDismiss: () -> Unit,
    onEditNotes: () -> Unit,
    onDownload: () -> Unit
) {
    val (statusColor, statusBg) = when (invoice.status) {
        "paid", "completed" -> GreenText to GreenBg
        "pending"           -> AmberText to AmberBg
        "failed"            -> RedText   to RedBg
        else                -> GreyText  to GreyBg
    }

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
                                Icon(Icons.Outlined.Receipt, null,
                                    tint = NavyBlue, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Invoice Details", fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(invoice.invoiceNumber, fontSize = 11.sp, color = NavyBlue)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Close",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Status + Amount
                item {
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = statusBg),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Amount", fontSize = 10.sp, color = statusColor.copy(alpha = 0.7f))
                                Text(formatKsh(invoice.amountKsh), fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = statusColor)
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(statusColor)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(displayStatus, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Detail rows
                item {
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PageBg),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InvDetailRow("Institution",    invoice.institutionName)
                            HorizontalDivider(color = BorderColor)
                            InvDetailRow("Payment Method", invoice.paymentMethod.ifEmpty { "—" })
                            HorizontalDivider(color = BorderColor)
                            InvDetailRow("Issued Date",    formatDate(invoice.issuedAt))
                            HorizontalDivider(color = BorderColor)
                            InvDetailRow(
                                "Paid Date",
                                if (invoice.status == "paid" || invoice.status == "completed")
                                    formatDateTime(invoice.paidAt) else "—"
                            )
                            if (invoice.subscriptionId.isNotEmpty()) {
                                HorizontalDivider(color = BorderColor)
                                InvDetailRow("Subscription", invoice.subscriptionId)
                            }
                            if (invoice.paymentId.isNotEmpty()) {
                                HorizontalDivider(color = BorderColor)
                                InvDetailRow("Payment Ref", invoice.paymentId.take(12))
                            }
                            if (invoice.notes.isNotEmpty()) {
                                HorizontalDivider(color = BorderColor)
                                InvDetailRow("Notes", invoice.notes)
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onEditNotes,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            border   = BorderStroke(1.dp, NavyBlue)
                        ) {
                            Icon(Icons.Default.Edit, null,
                                tint = NavyBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Notes", color = NavyBlue, fontSize = 13.sp)
                        }
                        Button(
                            onClick  = onDownload,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, null,
                                tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download PDF", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Edit Notes Dialog ─────────────────────────────────────────────────────────

@Composable
private fun EditNotesDialog(
    invoice: Invoice,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var notes by remember { mutableStateOf(invoice.notes) }

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
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Edit Notes", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close",
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Text(invoice.invoiceNumber, fontSize = 12.sp, color = NavyBlue)

                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Notes", fontSize = 12.sp) },
                    leadingIcon   = {
                        Icon(Icons.Outlined.Notes, null,
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = NavyBlue,
                        unfocusedBorderColor    = BorderColor,
                        focusedContainerColor   = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )

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
                        onClick  = { onConfirm(invoice.id, notes) },
                        enabled  = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Detail Row ────────────────────────────────────────────────────────────────

@Composable
private fun InvDetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

// ── Info Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun InvInfoChip(icon: ImageVector, label: String) {
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
private fun InvSummaryCard(
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
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
