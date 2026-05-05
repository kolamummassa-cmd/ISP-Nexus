package com.example.ispnexus.ui.theme.screens



import androidx.compose.foundation.Canvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.DefaulterItem
import com.example.ispnexus.viewmodels.FinanceDashboardUiState
import com.example.ispnexus.viewmodels.FinanceDashboardViewModel
import com.example.ispnexus.viewmodels.PaymentHistoryItem
import com.example.ispnexus.viewmodels.RevenuePoint
import kotlin.math.roundToInt

// ─── Color Palette ────────────────────────────────────────────────────────────
private val PrimaryBlue   = Color(0xFF1565C0)
private val LightBlue     = Color(0xFFE3F2FD)
private val TextPrimary   = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF90A4AE)
private val CardBg        = Color(0xFFFFFFFF)
private val PageBg        = Color(0xFFF4F6FB)
private val BorderColor   = Color(0xFFE8EAF0)
private val DividerColor  = Color(0xFFF0F2F8)

private val GreenText   = Color(0xFF2E7D32)
private val GreenBg     = Color(0xFFE8F5E9)
private val AmberText   = Color(0xFFF57F17)
private val AmberBg     = Color(0xFFFFF8E1)
private val RedText     = Color(0xFFC62828)
private val RedBg       = Color(0xFFFFEBEE)
private val PurpleText  = Color(0xFF6A1B9A)
private val PurpleBg    = Color(0xFFF3E5F5)
private val OnlineGreen = Color(0xFF43A047)
private val OfflineRed  = Color(0xFFE53935)
private val ChartBlue   = Color(0xFF1E88E5)

// ─── Currency Formatter ───────────────────────────────────────────────────────
private fun formatKsh(amount: Double): String {
    val formatted = "%,.2f".format(amount)
    return "Ksh $formatted"
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceDashboardViewModel = viewModel(),
    onNavigateToPayments: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToMore: () -> Unit = {},
    onPaymentClick: (PaymentHistoryItem) -> Unit = {},
    onRecordPayment: () -> Unit = {},
    onGenerateInvoice: () -> Unit = {},
    onExportReport: () -> Unit = {},
    onViewDefaulters: () -> Unit = {},
    onViewAllPayments: () -> Unit = {},
    onViewAllDefaulters: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNavItem by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            FinanceTopBar(
                officerName = uiState.officerName,
                companyName = uiState.companyName,
                notificationCount = uiState.notificationCount,
                onMenuClick = onMenuClick
            )
        },
        bottomBar = {
            FinanceBottomNav(
                selectedItem = selectedNavItem,
                onItemSelected = { index ->
                    selectedNavItem = index
                    when (index) {
                        1 -> onNavigateToPayments()
                        2 -> onNavigateToInvoices()
                        3 -> onNavigateToReports()
                        4 -> onNavigateToMore()
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                WelcomeHeader(officerName = uiState.officerName)
                Spacer(modifier = Modifier.height(14.dp))
                FinanceStatsGrid(state = uiState)
                Spacer(modifier = Modifier.height(14.dp))
                PaymentHistorySection(
                    payments = uiState.recentPayments,
                    onViewAll = onViewAllPayments,
                    onPaymentClick = onPaymentClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                AnalyticsRow(
                    revenuePoints = uiState.revenuePoints,
                    totalRevenue = uiState.totalRevenue,
                    revenueChange = uiState.totalRevenueChange,
                    defaulters = uiState.topDefaulters,
                    onViewAllDefaulters = onViewAllDefaulters
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickActionsSection(
                    onRecordPayment = onRecordPayment,
                    onGenerateInvoice = onGenerateInvoice,
                    onExportReport = onExportReport,
                    onViewDefaulters = onViewDefaulters
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceTopBar(
    officerName: String,
    companyName: String = "",
    notificationCount: Int,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(companyName.ifEmpty { "ISP NEXUS" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, letterSpacing = 0.5.sp)
                    Text("CONNECT · MANAGE · GROW", fontSize = 8.sp, color = TextSecondary, letterSpacing = 0.8.sp)
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(OfflineRed)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(notificationCount.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            val initials = officerName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6A1B9A))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    )
}

// ─── Welcome Header ───────────────────────────────────────────────────────────
@Composable
private fun WelcomeHeader(officerName: String) {
    Surface(color = CardBg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row {
                Text("Welcome back, ", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(officerName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Text(" 👋", fontSize = 17.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("Here's your financial overview for today.", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ─── Stats Grid ───────────────────────────────────────────────────────────────
@Composable
private fun FinanceStatsGrid(state: FinanceDashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceStatCard(
                label = "Total Revenue",
                labelColor = GreenText,
                iconBg = GreenBg,
                icon = Icons.Outlined.TrendingUp,
                iconTint = GreenText,
                primaryValue = formatKsh(state.totalRevenue),
                subValue = "This Month",
                change = "▲ ${state.totalRevenueChange}%",
                changeColor = OnlineGreen
            )
            FinanceStatCard(
                label = "Defaulters",
                labelColor = RedText,
                iconBg = RedBg,
                icon = Icons.Outlined.Group,
                iconTint = RedText,
                primaryValue = state.defaultersCount.toString(),
                subValue = "Institutions",
                change = "▲ ${state.newDefaulters} new",
                changeColor = OfflineRed
            )
        }
        // Right column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceStatCard(
                label = "Pending Payments",
                labelColor = AmberText,
                iconBg = AmberBg,
                icon = Icons.Outlined.Schedule,
                iconTint = AmberText,
                primaryValue = formatKsh(state.pendingPayments),
                subValue = "${state.pendingInvoiceCount} Invoices",
                change = "▲ ${state.pendingPaymentsChange}%",
                changeColor = AmberText
            )
            FinanceStatCard(
                label = "Paid Today",
                labelColor = PrimaryBlue,
                iconBg = LightBlue,
                icon = Icons.Outlined.CreditCard,
                iconTint = PrimaryBlue,
                primaryValue = formatKsh(state.paidToday),
                subValue = "${state.paidTodayCount} Payments",
                change = "▲ ${state.paidTodayChange}%",
                changeColor = OnlineGreen
            )
        }
    }
}

@Composable
private fun FinanceStatCard(
    label: String,
    labelColor: Color,
    iconBg: Color,
    icon: ImageVector,
    iconTint: Color,
    primaryValue: String,
    subValue: String,
    change: String,
    changeColor: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = primaryValue,
                fontSize = if (primaryValue.length > 10) 13.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Text(subValue, fontSize = 10.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(change, fontSize = 10.sp, color = changeColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Payment History Section ──────────────────────────────────────────────────
@Composable
private fun PaymentHistorySection(
    payments: List<PaymentHistoryItem>,
    onViewAll: () -> Unit,
    onPaymentClick: (PaymentHistoryItem) -> Unit
) {
    SectionCard(title = "Recent Payment History", onViewAll = onViewAll) {
        payments.forEachIndexed { index, payment ->
            PaymentRow(payment = payment, onClick = { onPaymentClick(payment) })
            if (index < payments.lastIndex) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentHistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Institution Icon
        val (iconBg, iconTint) = when (payment.status) {
            "Paid"    -> GreenBg to GreenText
            "Pending" -> AmberBg to AmberText
            else      -> LightBlue to PrimaryBlue
        }
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        // Name + Invoice
        Column(modifier = Modifier.weight(1f)) {
            Text(payment.institutionName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(payment.invoiceNumber, fontSize = 11.sp, color = PrimaryBlue)
        }

        // Amount + Date
        Column(horizontalAlignment = Alignment.End) {
            Text(formatKsh(payment.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(payment.date, fontSize = 10.sp, color = TextSecondary)
        }

        // Status Badge
        StatusBadge(status = payment.status)

        // Payment Method
        if (payment.paymentMethod.isNotEmpty()) {
            Text(payment.paymentMethod, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
        } else {
            Text("–", fontSize = 10.sp, color = TextSecondary)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFC5CAE9), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, text) = when (status) {
        "Paid"    -> GreenBg to GreenText
        "Pending" -> AmberBg to AmberText
        else      -> RedBg to RedText
    }
    Box(
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = text)
    }
}

// ─── Analytics Row (Chart + Defaulters) ──────────────────────────────────────
@Composable
private fun AnalyticsRow(
    revenuePoints: List<RevenuePoint>,
    totalRevenue: Double,
    revenueChange: Double,
    defaulters: List<DefaulterItem>,
    onViewAllDefaulters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Revenue Chart Card
        Card(
            modifier = Modifier.weight(1.1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly Revenue", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightBlue)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("This Month ▾", fontSize = 9.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatKsh(totalRevenue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("▲ $revenueChange%", fontSize = 10.sp, color = OnlineGreen, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                RevenueLineChart(points = revenuePoints, modifier = Modifier.fillMaxWidth().height(100.dp))
                Spacer(modifier = Modifier.height(4.dp))
                // X-axis labels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    revenuePoints.forEach { point ->
                        Text(point.month, fontSize = 8.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Defaulters Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Defaulters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("View All", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onViewAllDefaulters() })
                }
                Spacer(modifier = Modifier.height(10.dp))
                defaulters.forEach { defaulter ->
                    DefaulterRow(defaulter = defaulter)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DefaulterRow(defaulter: DefaulterItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${defaulter.rank}",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.width(12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(defaulter.institutionName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${defaulter.daysOverdue} Days", fontSize = 10.sp, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatKsh(defaulter.amountOwed), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedText)
        }
    }
}

// ─── Revenue Line Chart ───────────────────────────────────────────────────────
@Composable
private fun RevenueLineChart(points: List<RevenuePoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return

    Canvas(modifier = modifier) {
        val maxVal = points.maxOf { it.amount }
        val minVal = 0.0
        val range  = (maxVal - minVal).coerceAtLeast(1.0)

        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val offsets = points.mapIndexed { i, pt ->
            val x = i * stepX
            val y = size.height - ((pt.amount - minVal) / range * size.height).toFloat()
            Offset(x, y)
        }

        // Fill path
        val fillPath = Path().apply {
            moveTo(offsets.first().x, size.height)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ChartBlue.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = ChartBlue,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dots
        offsets.forEach { offset ->
            drawCircle(color = ChartBlue, radius = 4.dp.toPx(), center = offset)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = offset)
        }
    }
}

// ─── Quick Actions ────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsSection(
    onRecordPayment: () -> Unit,
    onGenerateInvoice: () -> Unit,
    onExportReport: () -> Unit,
    onViewDefaulters: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Quick Actions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Record Payment",
                icon = Icons.Outlined.Receipt,
                iconBg = GreenBg,
                iconTint = GreenText,
                onClick = onRecordPayment
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Generate Invoice",
                icon = Icons.Outlined.Description,
                iconBg = LightBlue,
                iconTint = PrimaryBlue,
                onClick = onGenerateInvoice
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Export Report",
                icon = Icons.Outlined.FileDownload,
                iconBg = PurpleBg,
                iconTint = PurpleText,
                onClick = onExportReport
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "View Defaulters",
                icon = Icons.Outlined.Group,
                iconBg = AmberBg,
                iconTint = AmberText,
                onClick = onViewDefaulters
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = iconTint, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ─── Section Card Wrapper ─────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    onViewAll: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("View All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue, modifier = Modifier.clickable { onViewAll() })
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
private fun FinanceBottomNav(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    data class NavItem(val label: String, val icon: ImageVector, val activeIcon: ImageVector)

    val items = listOf(
        NavItem("Dashboard", Icons.Outlined.Home,           Icons.Filled.Home),
        NavItem("Payments",  Icons.Outlined.AttachMoney,    Icons.Filled.AttachMoney),
        NavItem("Invoices",  Icons.Outlined.Description,    Icons.Filled.Description),
        NavItem("Reports",   Icons.Outlined.BarChart,       Icons.Filled.BarChart),
        NavItem("More",      Icons.Outlined.MoreHoriz,      Icons.Filled.MoreHoriz),
    )

    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 0.dp,
        modifier = Modifier.border(0.5.dp, BorderColor, RoundedCornerShape(0.dp))
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedItem == index
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.activeIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = LightBlue
                )
            )
        }
    }
}