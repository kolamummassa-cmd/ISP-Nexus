package com.example.ispnexus.ui.theme.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.models.Payment
import com.example.ispnexus.viewmodels.InstitutionRevenue
import com.example.ispnexus.viewmodels.MonthlyData
import com.example.ispnexus.viewmodels.ReportExportState
import com.example.ispnexus.viewmodels.ReportsUiState
import com.example.ispnexus.viewmodels.ReportsViewModel
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
private val PurpleText    = Color(0xFF6A1B9A)
private val PurpleBg      = Color(0xFFF3E5F5)

private val ChartBlue     = Color(0xFF1565C0)
private val ChartAmber    = Color(0xFFF59E0B)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return "Ksh ${fmt.format(amount)}"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

// ── PDF Generator ─────────────────────────────────────────────────────────────

private fun generateReportPdf(
    context: Context,
    payments: List<Payment>,
    totalCollected: Double,
    totalOutstanding: Double,
    totalFailed: Double,
    methodBreakdown: List<Triple<String, Double, Int>>,
    topInstitutions: List<InstitutionRevenue>
): File? {
    return try {
        val pdfDoc   = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 1100, 1).create()
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
        val greenPaint = Paint().apply {
            textSize       = 12f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#2E7D32")
        }
        val amberPaint = Paint().apply {
            textSize       = 12f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#F57F17")
        }
        val redPaint = Paint().apply {
            textSize       = 12f
            isFakeBoldText = true
            color          = android.graphics.Color.parseColor("#C62828")
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

        // ── Header ────────────────────────────────────────────────────────────
        canvas.drawText("ISP NEXUS", 40f, y, titlePaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f
        canvas.drawText("FINANCIAL REPORT  —  All Time", 40f, y, headingPaint)
        canvas.drawText("Generated: ${formatDateTime(System.currentTimeMillis())}", 380f, y, labelPaint)
        y += 26f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // ── Revenue Summary ───────────────────────────────────────────────────
        canvas.drawText("REVENUE SUMMARY", 40f, y, headingPaint)
        y += 20f

        fun drawKvRow(label: String, value: String, paint: Paint) {
            canvas.drawText(label, 40f, y, labelPaint)
            canvas.drawText(value, 300f, y, paint)
            y += 20f
        }

        drawKvRow("Total Collected",  formatKsh(totalCollected),  greenPaint)
        drawKvRow("Total Outstanding",formatKsh(totalOutstanding), amberPaint)
        drawKvRow("Total Failed",     formatKsh(totalFailed),      redPaint)
        drawKvRow("Total Payments",   payments.size.toString(),    valuePaint)
        drawKvRow("Completed",        payments.count { it.status == "completed" }.toString(), greenPaint)
        drawKvRow("Pending",          payments.count { it.status == "pending" }.toString(),   amberPaint)
        drawKvRow("Failed",           payments.count { it.status == "failed" }.toString(),    redPaint)

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // ── Payment Method Breakdown ──────────────────────────────────────────
        canvas.drawText("PAYMENT METHOD BREAKDOWN", 40f, y, headingPaint)
        y += 20f

        methodBreakdown.forEach { (method, amount, count) ->
            canvas.drawText(method, 40f, y, valuePaint)
            canvas.drawText(formatKsh(amount), 250f, y, greenPaint)
            canvas.drawText("$count payment${if (count != 1) "s" else ""}", 420f, y, labelPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // ── Top Institutions ──────────────────────────────────────────────────
        canvas.drawText("TOP PAYING INSTITUTIONS", 40f, y, headingPaint)
        y += 20f

        topInstitutions.forEachIndexed { index, inst ->
            canvas.drawText("${index + 1}.  ${inst.institutionName}", 40f, y, valuePaint)
            canvas.drawText(formatKsh(inst.totalPaid), 340f, y, greenPaint)
            canvas.drawText("${inst.paymentCount} payment${if (inst.paymentCount != 1) "s" else ""}",
                460f, y, labelPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // ── Recent Payments table ─────────────────────────────────────────────
        canvas.drawText("RECENT PAYMENTS (Last 10)", 40f, y, headingPaint)
        y += 18f

        // Table header
        val colInstitution = 40f
        val colMethod      = 200f
        val colAmount      = 340f
        val colStatus      = 440f

        canvas.drawText("Institution", colInstitution, y, labelPaint)
        canvas.drawText("Method",      colMethod,      y, labelPaint)
        canvas.drawText("Amount",      colAmount,      y, labelPaint)
        canvas.drawText("Status",      colStatus,      y, labelPaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 14f

        payments.take(10).forEach { p ->
            val statusPaint = when (p.status) {
                "completed" -> greenPaint
                "failed"    -> redPaint
                else        -> amberPaint
            }
            canvas.drawText(p.institutionName.take(22), colInstitution, y, valuePaint)
            canvas.drawText(p.paymentMethod.take(16),   colMethod,      y, valuePaint)
            canvas.drawText(formatKsh(p.amountKsh),     colAmount,      y, valuePaint)
            canvas.drawText(p.status.replaceFirstChar { it.uppercase() }, colStatus, y, statusPaint)
            y += 18f
        }

        y += 14f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        // ── Footer ────────────────────────────────────────────────────────────
        canvas.drawText(
            "ISP Nexus  ·  This report is auto-generated and reflects all-time payment data.",
            40f, y, footerPaint
        )

        pdfDoc.finishPage(page)

        val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val fileName = "ISPNexus_Report_${SimpleDateFormat("yyyyMMdd_HHmm",
            Locale.getDefault()).format(Date())}.pdf"
        val file = File(dir, fileName)
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
fun ReportsScreen(
    onMenuClick: () -> Unit = {},
    viewModel: ReportsViewModel = viewModel(),
    onBack: () -> Boolean
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val context     = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Side-effects ──────────────────────────────────────────────────────────
    LaunchedEffect(exportState) {
        when (val e = exportState) {
            is ReportExportState.Success -> {
                snackbarHostState.showSnackbar("Report saved: ${e.filePath}")
                viewModel.resetExportState()
            }
            is ReportExportState.Error -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.resetExportState()
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
                        Text("Reports", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("All-time financial overview", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    if (state is ReportsUiState.Success) {
                        IconButton(
                            onClick = {
                                val payments = (state as ReportsUiState.Success).payments
                                viewModel.setExportLoading()
                                val file = generateReportPdf(
                                    context          = context,
                                    payments         = payments,
                                    totalCollected   = viewModel.totalCollected(payments),
                                    totalOutstanding = viewModel.totalOutstanding(payments),
                                    totalFailed      = viewModel.totalFailed(payments),
                                    methodBreakdown  = viewModel.methodBreakdown(payments),
                                    topInstitutions  = viewModel.topInstitutions(payments)
                                )
                                if (file != null) {
                                    // Open PDF
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
                                        android.content.Intent.createChooser(intent, "Open Report PDF")
                                    )
                                    viewModel.setExportSuccess(file.name)
                                } else {
                                    viewModel.setExportError("Failed to generate PDF")
                                }
                            },
                            enabled = exportState !is ReportExportState.Loading
                        ) {
                            if (exportState is ReportExportState.Loading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(18.dp),
                                    color       = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->

        when (val s = state) {
            is ReportsUiState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = NavyBlue) }
            }

            is ReportsUiState.Error -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadReports() },
                            colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) { Text("Retry") }
                    }
                }
            }

            is ReportsUiState.Success -> {
                val payments         = s.payments
                val totalCollected   = viewModel.totalCollected(payments)
                val totalOutstanding = viewModel.totalOutstanding(payments)
                val totalFailed      = viewModel.totalFailed(payments)
                val breakdown        = viewModel.methodBreakdown(payments)
                val topInstitutions  = viewModel.topInstitutions(payments)
                val trend            = viewModel.monthlyTrend(payments)

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Revenue Summary Cards ─────────────────────────────────
                    item {
                        SectionHeader(
                            title = "Revenue Summary",
                            icon  = Icons.Outlined.AttachMoney
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RevCard(
                                modifier = Modifier.weight(1f),
                                label    = "Collected",
                                amount   = totalCollected,
                                count    = viewModel.countByStatus(payments, "completed"),
                                color    = GreenText,
                                bgColor  = GreenBg,
                                icon     = Icons.Default.CheckCircle
                            )
                            RevCard(
                                modifier = Modifier.weight(1f),
                                label    = "Outstanding",
                                amount   = totalOutstanding,
                                count    = viewModel.countByStatus(payments, "pending"),
                                color    = AmberText,
                                bgColor  = AmberBg,
                                icon     = Icons.Default.HourglassEmpty
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RevCard(
                                modifier = Modifier.weight(1f),
                                label    = "Failed",
                                amount   = totalFailed,
                                count    = viewModel.countByStatus(payments, "failed"),
                                color    = RedText,
                                bgColor  = RedBg,
                                icon     = Icons.Default.Cancel
                            )
                            // Total payments card
                            Card(
                                modifier  = Modifier.weight(1f),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = NavyBlue),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier            = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier.size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Receipt, null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(17.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text       = payments.size.toString(),
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                    Text("Total Payments", fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.75f))
                                }
                            }
                        }
                    }

                    // ── Monthly Trend Chart ───────────────────────────────────
                    item {
                        SectionHeader(title = "Monthly Trend", icon = Icons.Outlined.TrendingUp)
                        Spacer(Modifier.height(10.dp))
                        TrendChart(trend = trend)
                    }

                    // ── Payment Method Breakdown ──────────────────────────────
                    item {
                        SectionHeader(
                            title = "Payment Method Breakdown",
                            icon  = Icons.Outlined.AccountBalanceWallet
                        )
                        Spacer(Modifier.height(10.dp))
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            colors    = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(1.dp),
                            border    = BorderStroke(1.dp, BorderColor)
                        ) {
                            val grandTotal = breakdown.sumOf { it.second }
                                .takeIf { it > 0 } ?: 1.0

                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                breakdown.forEachIndexed { index, (method, amount, count) ->
                                    val fraction = (amount / grandTotal).toFloat()
                                    val methodColor = when (method) {
                                        "Bank Transfer" -> NavyBlue
                                        "Mobile Money"  -> GreenText
                                        else            -> AmberText
                                    }
                                    val methodIcon = when (method) {
                                        "Bank Transfer" -> Icons.Outlined.AccountBalance
                                        "Mobile Money"  -> Icons.Outlined.PhoneAndroid
                                        else            -> Icons.Outlined.Payments
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier              = Modifier.fillMaxWidth(),
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier         = Modifier.size(30.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(LightBlue),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(methodIcon, null,
                                                        tint     = methodColor,
                                                        modifier = Modifier.size(16.dp))
                                                }
                                                Column {
                                                    Text(method, fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary)
                                                    Text("$count payment${if (count != 1) "s" else ""}",
                                                        fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }
                                            Text(formatKsh(amount), fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold, color = methodColor)
                                        }
                                        // Progress bar
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(BorderColor)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(methodColor)
                                            )
                                        }
                                    }
                                    if (index < breakdown.size - 1) {
                                        HorizontalDivider(color = BorderColor)
                                    }
                                }
                            }
                        }
                    }

                    // ── Top Paying Institutions ───────────────────────────────
                    item {
                        SectionHeader(
                            title = "Top Paying Institutions",
                            icon  = Icons.Outlined.Leaderboard
                        )
                        Spacer(Modifier.height(10.dp))
                        if (topInstitutions.isEmpty()) {
                            EmptySection("No payment data yet")
                        } else {
                            Card(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = CardBg),
                                elevation = CardDefaults.cardElevation(1.dp),
                                border    = BorderStroke(1.dp, BorderColor)
                            ) {
                                val maxAmount = topInstitutions.maxOfOrNull { it.totalPaid }
                                    ?.takeIf { it > 0 } ?: 1.0
                                Column(modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    topInstitutions.forEachIndexed { index, inst ->
                                        val fraction = (inst.totalPaid / maxAmount).toFloat()
                                        val rankColor = when (index) {
                                            0    -> Color(0xFFFFD700) // Gold
                                            1    -> Color(0xFFC0C0C0) // Silver
                                            2    -> Color(0xFFCD7F32) // Bronze
                                            else -> TextSecondary
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                        modifier         = Modifier.size(28.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(rankColor.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text       = "${index + 1}",
                                                            fontSize   = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color      = rankColor
                                                        )
                                                    }
                                                    Column {
                                                        Text(inst.institutionName, fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary)
                                                        Text(
                                                            "${inst.paymentCount} payment${if (inst.paymentCount != 1) "s" else ""}",
                                                            fontSize = 10.sp, color = TextSecondary
                                                        )
                                                    }
                                                }
                                                Text(formatKsh(inst.totalPaid), fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold, color = GreenText)
                                            }
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(BorderColor)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(GreenText)
                                                )
                                            }
                                        }
                                        if (index < topInstitutions.size - 1) {
                                            HorizontalDivider(color = BorderColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

// ── Trend Chart ───────────────────────────────────────────────────────────────

@Composable
private fun TrendChart(trend: List<MonthlyData>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border    = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = ChartBlue,  label = "Collected")
                LegendDot(color = ChartAmber, label = "Outstanding")
            }
            Spacer(Modifier.height(12.dp))

            if (trend.all { it.collected == 0.0 && it.outstanding == 0.0 }) {
                EmptySection("No data for the last 6 months")
                return@Column
            }

            val maxVal = trend.maxOf { maxOf(it.collected, it.outstanding) }
                .takeIf { it > 0 } ?: 1.0

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w         = size.width
                val h         = size.height
                val padLeft   = 12.dp.toPx()
                val padBottom = 24.dp.toPx()
                val chartW    = w - padLeft
                val chartH    = h - padBottom
                val n         = trend.size
                val stepX     = chartW / (n - 1).coerceAtLeast(1)

                // Grid lines
                (0..3).forEach { i ->
                    val lineY = chartH * (i / 3f)
                    drawLine(
                        color       = Color(0xFFE8EAF0),
                        start       = Offset(padLeft, lineY),
                        end         = Offset(w, lineY),
                        strokeWidth = 1f
                    )
                }

                // Collected area fill
                val collectedPath = Path()
                trend.forEachIndexed { i, d ->
                    val x = padLeft + i * stepX
                    val y = chartH - (d.collected / maxVal * chartH).toFloat()
                    if (i == 0) collectedPath.moveTo(x, y) else collectedPath.lineTo(x, y)
                }
                val fillPath = Path().apply {
                    addPath(collectedPath)
                    lineTo(padLeft + (n - 1) * stepX, chartH)
                    lineTo(padLeft, chartH)
                    close()
                }
                drawPath(fillPath, color = ChartBlue.copy(alpha = 0.12f))

                // Collected line
                drawPath(
                    path        = collectedPath,
                    color       = ChartBlue,
                    style       = Stroke(width = 2.5f)
                )

                // Outstanding line
                val outstandingPath = Path()
                trend.forEachIndexed { i, d ->
                    val x = padLeft + i * stepX
                    val y = chartH - (d.outstanding / maxVal * chartH).toFloat()
                    if (i == 0) outstandingPath.moveTo(x, y)
                    else outstandingPath.lineTo(x, y)
                }
                drawPath(
                    path        = outstandingPath,
                    color       = ChartAmber,
                    style       = Stroke(width = 2f)
                )

                // Dots — collected
                trend.forEachIndexed { i, d ->
                    val x = padLeft + i * stepX
                    val y = chartH - (d.collected / maxVal * chartH).toFloat()
                    drawCircle(color = ChartBlue,  radius = 5f, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, y))
                }

                // Dots — outstanding
                trend.forEachIndexed { i, d ->
                    val x = padLeft + i * stepX
                    val y = chartH - (d.outstanding / maxVal * chartH).toFloat()
                    drawCircle(color = ChartAmber, radius = 4f, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 2f,  center = Offset(x, y))
                }
            }

            // X-axis labels
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trend.forEach { d ->
                    Text(
                        text      = d.monthLabel.take(3),
                        fontSize  = 9.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ── Revenue Card ──────────────────────────────────────────────────────────────

@Composable
private fun RevCard(
    modifier: Modifier,
    label: String,
    amount: Double,
    count: Int,
    color: Color,
    bgColor: Color,
    icon: ImageVector
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border    = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
            }
            Text(formatKsh(amount), fontSize = 15.sp,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text("$count payment${if (count != 1) "s" else ""}",
                fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier         = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                .background(LightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(15.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

// ── Legend Dot ────────────────────────────────────────────────────────────────

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Empty Section ─────────────────────────────────────────────────────────────

@Composable
private fun EmptySection(message: String) {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}