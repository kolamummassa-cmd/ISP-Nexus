package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.AdminDashboardData
import com.example.ispnexus.viewmodels.AdminDashboardState
import com.example.ispnexus.viewmodels.AdminViewModel
import com.example.ispnexus.viewmodels.RecentInstitution
import com.example.ispnexus.viewmodels.RecentPayment
import com.example.ispnexus.viewmodels.RevenueStat

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue    = Color(0xFF1A2341)
private val CorporateBlue = Color(0xFF0D47A1)
private val PageBg      = Color(0xFFF4F6FA)
private val CardWhite   = Color(0xFFFFFFFF)
private val ActiveGreen = Color(0xFF2E7D32)
private val AmberColor  = Color(0xFFB7791F)
private val RedColor    = Color(0xFFC62828)
private val PurpleColor = Color(0xFF6A1B9A)

// ── Currency formatter ────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "Ksh ${"%.2f".format(amount / 1_000_000)}M"
        amount >= 1_000     -> "Ksh ${"%.0f".format(amount / 1_000)}K"
        else                -> "Ksh ${"%.0f".format(amount)}"
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    companyId: String,
    onLogout: () -> Unit = {},
    onInstitutions: () -> Unit = {},
    onSubscriptions: () -> Unit = {},
    onPlans: () -> Unit = {},
    onPayments: () -> Unit = {},
    onStaff: () -> Unit = {},
    onTechnicians: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(companyId) {
        viewModel.loadDashboard(companyId)
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = NavyBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Wifi,
                            contentDescription = null,
                            tint               = Color(0xFF4FC3F7),
                            modifier           = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text       = "ISP-NEXUS",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp,
                                color      = Color.White
                            )
                            Text(
                                text     = "Connect. Manage. Grow.",
                                fontSize = 9.sp,
                                color    = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    // Notification bell
                    BadgedBox(
                        badge = {
                            Badge { Text("8") }
                        }
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->

        when (val s = state) {
            is AdminDashboardState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CorporateBlue)
                }
            }

            is AdminDashboardState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RedColor, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadDashboard(companyId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CorporateBlue
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is AdminDashboardState.Success -> {
                DashboardContent(
                    data           = s.data,
                    padding        = padding,
                    onInstitutions = onInstitutions,
                    onSubscriptions = onSubscriptions,
                    onPlans        = onPlans,
                    onPayments     = onPayments,
                    onStaff        = onStaff,
                    onAnalytics    = onAnalytics
                )
            }
        }
    }
}

// ── Dashboard Content ─────────────────────────────────────────────────────────

@Composable
private fun DashboardContent(
    data: AdminDashboardData,
    padding: PaddingValues,
    onInstitutions: () -> Unit,
    onSubscriptions: () -> Unit,
    onPlans: () -> Unit,
    onPayments: () -> Unit,
    onStaff: () -> Unit,
    onAnalytics: () -> Unit
) {
    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Welcome header ────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text       = "Welcome, Admin",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A1A)
                )
                Text(
                    text     = "Here's what's happening with your ISP today.",
                    fontSize = 13.sp,
                    color    = Color(0xFF6B7280)
                )
            }
        }

        // ── Top 4 stat cards ──────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    label    = "Total Institutions",
                    value    = data.totalInstitutions.toString(),
                    sub      = "",
                    subColor = ActiveGreen,
                    iconBg   = Color(0xFFEDE7F6),
                    iconTint = PurpleColor,
                    icon     = Icons.Default.Business,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label    = "Active Subscriptions",
                    value    = data.activeSubscriptions.toString(),
                    sub      = "",
                    subColor = ActiveGreen,
                    iconBg   = Color(0xFFE8F5E9),
                    iconTint = ActiveGreen,
                    icon     = Icons.Default.Wifi,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    label    = "Pending Payments",
                    value    = formatKsh(data.pendingPayments),
                    sub      = "",
                    subColor = RedColor,
                    iconBg   = Color(0xFFFFF8E1),
                    iconTint = AmberColor,
                    icon     = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label    = "Monthly Revenue",
                    value    = formatKsh(data.monthlyRevenue),
                    sub      = "",
                    subColor = ActiveGreen,
                    iconBg   = Color(0xFFE3F2FD),
                    iconTint = CorporateBlue,
                    icon     = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Secondary stat cards ──────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    label    = "Suspended Institutions",
                    value    = data.suspendedInstitutions.toString(),
                    sub      = "",
                    subColor = RedColor,
                    iconBg   = Color(0xFFFFEBEE),
                    iconTint = RedColor,
                    icon     = Icons.Default.Block,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label    = "Active Technicians",
                    value    = data.activeTechnicians.toString(),
                    sub      = "",
                    subColor = ActiveGreen,
                    iconBg   = Color(0xFFE8F5E9),
                    iconTint = ActiveGreen,
                    icon     = Icons.Default.Engineering,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Quick Actions ─────────────────────────────────────────────────
        item {
            Text(
                text       = "Quick Actions",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1A1A)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding        = PaddingValues(horizontal = 2.dp)
            ) {
                val actions = listOf(
                    Triple("Register\nInstitution", Icons.Default.AddBusiness,    onInstitutions),
                    Triple("Create\nInternet Plan", Icons.Default.Wifi,           onPlans),
                    Triple("Manage\nStaff",          Icons.Default.People,         onStaff),
                    Triple("Billing\nOverview",       Icons.Default.Receipt,        onPayments),
                    Triple("View\nReports",           Icons.Default.BarChart,       onAnalytics)
                )
                items(actions) { (label, icon, onClick) ->
                    QuickActionCard(label = label, icon = icon, onClick = onClick)
                }
            }
        }

        // ── Revenue Trend Chart ───────────────────────────────────────────
        item {
            RevenueChart(trend = data.monthlyRevenueTrend)
        }

        // ── Collection Rate + Popular Plan ────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CollectionRateCard(
                    rate        = data.collectionRate,
                    collected   = data.collected,
                    outstanding = data.outstanding,
                    modifier    = Modifier.weight(1f)
                )
                PopularPlanCard(
                    plan     = data.mostPopularPlan,
                    subs     = data.mostPopularPlanSubs,
                    percent  = data.mostPopularPlanPercent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Defaulter Rate ────────────────────────────────────────────────
        item {
            DefaulterCard(rate = data.defaulterRate)
        }

        // ── Recent Institutions ───────────────────────────────────────────
        item {
            RecentInstitutionsTable(institutions = data.recentInstitutions)
        }

        // ── Recent Payments ───────────────────────────────────────────────
        item {
            RecentPaymentsTable(payments = data.recentPayments)
        }

        // ── Footer ────────────────────────────────────────────────────────
        item {
            Text(
                text      = "© 2026 ISP-NEXUS. All rights reserved.",
                fontSize  = 11.sp,
                color     = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

// ── Stat Metric Card ──────────────────────────────────────────────────────────

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    iconBg: Color,
    iconTint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1A1A),
                maxLines   = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = sub,
                fontSize = 10.sp,
                color    = subColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick         = onClick,
        shape           = RoundedCornerShape(16.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.width(100.dp)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = CorporateBlue,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = label,
                fontSize  = 11.sp,
                fontWeight = FontWeight.Medium,
                color     = Color(0xFF374151),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Revenue Chart ─────────────────────────────────────────────────────────────

@Composable
private fun RevenueChart(trend: List<RevenueStat>) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Monthly Revenue Trend",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color(0xFF1A1A1A)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text     = "This Year",
                        fontSize = 11.sp,
                        color    = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = trend.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                trend.forEach { stat ->
                    val fraction = (stat.amount / maxVal).toFloat()
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((fraction * 100f).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (stat.amount > 0) CorporateBlue.copy(alpha = 0.8f)
                                    else Color(0xFFE5E7EB)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text      = stat.month,
                            fontSize  = 9.sp,
                            color     = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Collection Rate Card ──────────────────────────────────────────────────────

@Composable
private fun CollectionRateCard(
    rate: Float,
    collected: Double,
    outstanding: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text       = "Payment Collection Rate",
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                color      = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Circular progress
            Box(
                modifier         = Modifier
                    .size(90.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress        = { rate / 100f },
                    modifier        = Modifier.fillMaxSize(),
                    color           = ActiveGreen,
                    trackColor      = Color(0xFFE5E7EB),
                    strokeWidth     = 10.dp,
                    strokeCap       = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${"%.0f".format(rate)}%",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1A1A1A)
                    )
                    Text(
                        text     = "Collected",
                        fontSize = 9.sp,
                        color    = Color(0xFF9CA3AF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ActiveGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = formatKsh(collected), fontSize = 11.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
            }
            Text(text = "Collected", fontSize = 10.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(start = 12.dp))

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RedColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = formatKsh(outstanding), fontSize = 11.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
            }
            Text(text = "Outstanding", fontSize = 10.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(start = 12.dp))
        }
    }
}

// ── Popular Plan Card ─────────────────────────────────────────────────────────

@Composable
private fun PopularPlanCard(
    plan: String,
    subs: Int,
    percent: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text       = "Most Popular Plan",
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                color      = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE7F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = PurpleColor,
                    modifier           = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text       = plan,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = "$subs Subscriptions",
                fontSize = 12.sp,
                color    = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFC8E6C9)
            ) {
                Text(
                    text       = "${"%.0f".format(percent)}% of total",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ActiveGreen,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Defaulter Card ────────────────────────────────────────────────────────────

@Composable
private fun DefaulterCard(rate: Float) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.PersonOff,
                    contentDescription = null,
                    tint               = RedColor,
                    modifier           = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = "Defaulter Rate",
                    fontSize = 12.sp,
                    color    = Color(0xFF9CA3AF)
                )
                Text(
                    text       = "${"%.1f".format(rate)}%",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RedColor
                )
            }
            Text(
                text     = "",
                fontSize = 11.sp,
                color    = ActiveGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Recent Institutions Table ─────────────────────────────────────────────────

@Composable
private fun RecentInstitutionsTable(institutions: List<RecentInstitution>) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Recent Institutions",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
                TextButton(onClick = {}) {
                    Text("View All", color = CorporateBlue, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Institution Name", fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(2f))
                Text("Plan",            fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1.5f))
                Text("Status",          fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))

            if (institutions.isEmpty()) {
                Text(
                    text      = "No institutions yet",
                    fontSize  = 12.sp,
                    color     = Color(0xFF9CA3AF),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                institutions.forEach { inst ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier          = Modifier.weight(2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Business,
                                contentDescription = null,
                                tint               = Color(0xFF9CA3AF),
                                modifier           = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text     = inst.name,
                                fontSize = 12.sp,
                                color    = Color(0xFF1A1A1A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text     = inst.plan,
                            fontSize = 12.sp,
                            color    = Color(0xFF4B5563),
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            StatusBadge(status = inst.status)
                        }
                    }
                }
            }
        }
    }
}

// ── Recent Payments Table ─────────────────────────────────────────────────────

@Composable
private fun RecentPaymentsTable(payments: List<RecentPayment>) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Recent Payments",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
                TextButton(onClick = {}) {
                    Text("View All", color = CorporateBlue, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Institution", fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(2f))
                Text("Amount",      fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1.5f))
                Text("Status",      fontSize = 11.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))

            if (payments.isEmpty()) {
                Text(
                    text      = "No payments yet",
                    fontSize  = 12.sp,
                    color     = Color(0xFF9CA3AF),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                payments.forEach { payment ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier          = Modifier.weight(2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Business,
                                contentDescription = null,
                                tint               = Color(0xFF9CA3AF),
                                modifier           = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text     = payment.institution,
                                fontSize = 12.sp,
                                color    = Color(0xFF1A1A1A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text     = formatKsh(payment.amount),
                            fontSize = 12.sp,
                            color    = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.5f)
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            StatusBadge(status = payment.status)
                        }
                    }
                }
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val (bg, textColor) = when (status.lowercase()) {
        "active", "paid", "approved" -> Color(0xFFC8E6C9) to ActiveGreen
        "pending"                    -> Color(0xFFFFF4E5) to AmberColor
        "suspended", "overdue"       -> Color(0xFFFFEBEE) to RedColor
        else                         -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            text       = status.replaceFirstChar { it.uppercase() },
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
