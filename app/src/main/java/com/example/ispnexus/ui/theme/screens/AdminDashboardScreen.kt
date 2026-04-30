package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import java.text.NumberFormat
import kotlinx.coroutines.launch
import java.util.Locale

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue     = Color(0xFF0D47A1)
private val SidebarBg    = Color(0xFF0D1B3E)
private val PageBg       = Color(0xFFF4F6FA)
private val CardWhite    = Color(0xFFFFFFFF)
private val ActiveGreen  = Color(0xFF2E7D32)
private val PendingAmber = Color(0xFFB7791F)
private val RedAccent    = Color(0xFFC62828)
private val PurpleAccent = Color(0xFF6A1B9A)
private val TextPrimary  = Color(0xFF1A1A2E)
private val TextGray     = Color(0xFF6B7280)

// ── Currency formatter ────────────────────────────────────────────────────────

private fun formatKsh(amount: Double): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())
    return "Ksh $formatted"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onInstitutions: () -> Unit = {},
    onSubscriptions: () -> Unit = {},
    onPlans: () -> Unit = {},
    onPayments: () -> Unit = {},
    onInvoices: () -> Unit = {},
    onStaff: () -> Unit = {},
    onTechnicians: () -> Unit = {},
    onSupportTickets: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    onReports: () -> Unit = {},
    onRevenue: () -> Unit = {},
    onCompanySettings: () -> Unit = {},
    onProfile: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // ── Drawer state ──────────────────────────────────────────────────────────
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    when (val s = state) {
        is AdminDashboardState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }
        is AdminDashboardState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message, color = RedAccent, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp))
                    Button(onClick = { viewModel.loadDashboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) {
                        Text("Retry")
                    }
                }
            }
        }
        is AdminDashboardState.Success -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = SidebarBg,
                        modifier             = Modifier.width(260.dp)
                    ) {
                        AdminSidebarContent(
                            companyName      = s.data.companyName,
                            onLogout         = {
                                scope.launch { drawerState.close() }
                                onLogout()
                            },
                            onItemClick      = { scope.launch { drawerState.close() } },
                            onInstitutions   = onInstitutions,
                            onSubscriptions  = onSubscriptions,
                            onPlans          = onPlans,
                            onPayments       = onPayments,
                            onInvoices       = onInvoices,
                            onStaff          = onStaff,
                            onTechnicians    = onTechnicians,
                            onSupportTickets = onSupportTickets,
                            onAnalytics      = onAnalytics,
                            onReports        = onReports,
                            onRevenue        = onRevenue,
                            onCompanySettings = onCompanySettings,
                            onProfile        = onProfile
                        )
                    }
                }
            ) {
                AdminDashboardContent(
                    data         = s.data,
                    onMenuClick  = { scope.launch { drawerState.open() } },
                    onLogout     = onLogout,
                    onInstitutions   = onInstitutions,
                    onSubscriptions  = onSubscriptions,
                    onPlans          = onPlans,
                    onPayments       = onPayments,
                    onStaff          = onStaff,
                    onReports        = onReports
                )
            }
        }
    }
}

// ── Dashboard Content ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardContent(
    data: AdminDashboardData,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit,
    onInstitutions: () -> Unit,
    onSubscriptions: () -> Unit,
    onPlans: () -> Unit,
    onPayments: () -> Unit,
    onStaff: () -> Unit,
    onReports: () -> Unit,
) {
    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = NavyBlue,
                    titleContentColor      = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    // ☰ Hamburger button — opens drawer
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector        = Icons.Default.Menu,
                            contentDescription = "Open menu"
                        )
                    }
                },
                title = {
                    Column {
                        Text("ISP Nexus", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Admin Dashboard", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    // Company chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(data.companyName, fontSize = 11.sp, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout",
                            tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Welcome header ────────────────────────────────────────────
            item {
                Text(
                    text       = "Welcome, ${data.adminName} ",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    text     = "Here's what's happening with your ISP today.",
                    fontSize = 13.sp,
                    color    = TextGray
                )
            }

            // ── Top 4 metric cards ────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        label    = "Total Institutions",
                        value    = data.totalInstitutions.toString(),
                        subLabel = "+${(data.totalInstitutions * 0.09).toInt()} this month",
                        icon     = Icons.Default.Business,
                        iconBg   = Color(0xFFE8EAF6), iconTint = Color(0xFF3949AB),
                        subColor = ActiveGreen, modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label    = "Active Subscriptions",
                        value    = data.activeSubscriptions.toString(),
                        subLabel = "+${(data.activeSubscriptions * 0.05).toInt()} this month",
                        icon     = Icons.Default.Wifi,
                        iconBg   = Color(0xFFE8F5E9), iconTint = ActiveGreen,
                        subColor = ActiveGreen, modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        label    = "Pending Payments",
                        value    = formatKsh(data.pendingPayments),
                        subLabel = "↑ 8.5% from last month",
                        icon     = Icons.Default.AccountBalanceWallet,
                        iconBg   = Color(0xFFFFF8E1), iconTint = PendingAmber,
                        subColor = RedAccent, modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label    = "Monthly Revenue",
                        value    = formatKsh(data.monthlyRevenue),
                        subLabel = "↑ 15.3% from last month",
                        icon     = Icons.Default.MonetizationOn,
                        iconBg   = Color(0xFFE3F2FD), iconTint = NavyBlue,
                        subColor = ActiveGreen, modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        label    = "Suspended Institutions",
                        value    = data.suspendedInstitutions.toString(),
                        subLabel = "↑ 2 this month",
                        icon     = Icons.Default.Block,
                        iconBg   = Color(0xFFFFEBEE), iconTint = RedAccent,
                        subColor = RedAccent, modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label    = "Active Technicians",
                        value    = data.activeTechnicians.toString(),
                        subLabel = "↑ 3 this month",
                        icon     = Icons.Default.Engineering,
                        iconBg   = Color(0xFFF3E5F5), iconTint = PurpleAccent,
                        subColor = ActiveGreen, modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────
            item {
                Text("Quick Actions", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard("Register\nInstitution", Icons.Default.AddBusiness,
                        Color(0xFFE8EAF6), Color(0xFF3949AB), onInstitutions)
                    QuickActionCard("Create\nInternet Plan", Icons.Default.Wifi,
                        Color(0xFFE8F5E9), ActiveGreen, onPlans)
                    QuickActionCard("Manage\nStaff", Icons.Default.Group,
                        Color(0xFFF3E5F5), PurpleAccent, onStaff)
                    QuickActionCard("Billing\nOverview", Icons.Default.Receipt,
                        Color(0xFFFFF8E1), PendingAmber, onPayments)
                    QuickActionCard("View\nReports", Icons.Default.BarChart,
                        Color(0xFFE3F2FD), NavyBlue, onReports)
                }
            }

            // ── Revenue Trend Chart ───────────────────────────────────────
            item { AdminRevenueChart(trend = data.monthlyRevenueTrend) }

            // ── Most Popular Plan + Defaulter Rate ────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
                        shadowElevation = 2.dp, modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(Color(0xFFE8EAF6)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Stars, contentDescription = null,
                                    tint = Color(0xFF3949AB), modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Most Popular Plan", fontSize = 11.sp, color = TextGray)
                                Text(data.mostPopularPlan.ifEmpty { "N/A" },
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${data.mostPopularPlanCount} Subscriptions",
                                        fontSize = 11.sp, color = TextGray)
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFFE8F5E9)) {
                                        Text("${"%.0f".format(data.mostPopularPlanPercent)}% of total",
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = ActiveGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }

                    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
                        shadowElevation = 2.dp, modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonOff, contentDescription = null,
                                    tint = RedAccent, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Defaulter Rate", fontSize = 11.sp, color = TextGray)
                                Text("${"%.1f".format(data.defaulterRate)}%",
                                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                                Text("↓ 1.2% from last month", fontSize = 11.sp, color = ActiveGreen)
                            }
                        }
                    }
                }
            }

            // ── Recent Institutions + Payments ────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecentInstitutionsTable(
                        institutions = data.recentInstitutions,
                        modifier     = Modifier.weight(1f)
                    )
                    RecentPaymentsTable(
                        payments = data.recentPayments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Footer ────────────────────────────────────────────────────
            item {
                Text(
                    text      = "© 2024 ISP-NEXUS. All rights reserved.",
                    fontSize  = 11.sp,
                    color     = TextGray,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

// ── Sidebar Content ───────────────────────────────────────────────────────────

@Composable
private fun AdminSidebarContent(
    companyName: String,
    onLogout: () -> Unit,
    onItemClick: () -> Unit,
    onInstitutions: () -> Unit,
    onSubscriptions: () -> Unit,
    onPlans: () -> Unit,
    onPayments: () -> Unit,
    onInvoices: () -> Unit,
    onStaff: () -> Unit,
    onTechnicians: () -> Unit,
    onSupportTickets: () -> Unit,
    onAnalytics: () -> Unit,
    onReports: () -> Unit,
    onRevenue: () -> Unit,
    onCompanySettings: () -> Unit,
    onProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(SidebarBg)
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Logo
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("ISP-NEXUS", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color.White)
                Text("Connect. Manage. Grow.", fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f))
            }

            Spacer(Modifier.height(24.dp))

            // Active item
            SidebarItem("Dashboard", Icons.Default.Dashboard, true) {}

            Spacer(Modifier.height(6.dp))
            SidebarSectionLabel("MANAGEMENT")
            SidebarItem("Institutions",    Icons.Default.Business,           false) { onItemClick(); onInstitutions() }
            SidebarItem("Subscriptions",   Icons.Default.Subscriptions,      false) { onItemClick(); onSubscriptions() }
            SidebarItem("Plans",           Icons.Default.List,               false) { onItemClick(); onPlans() }
            SidebarItem("Payments",        Icons.Default.Payments,           false) { onItemClick(); onPayments() }
            SidebarItem("Invoices",        Icons.Default.Receipt,            false) { onItemClick(); onInvoices() }
            SidebarItem("Staff Mgmt",      Icons.Default.Group,              false) { onItemClick(); onStaff() }
            SidebarItem("Technicians",     Icons.Default.Engineering,        false) { onItemClick(); onTechnicians() }
            SidebarItem("Support Tickets", Icons.Default.ConfirmationNumber, false) { onItemClick(); onSupportTickets() }

            Spacer(Modifier.height(6.dp))
            SidebarSectionLabel("REPORTS")
            SidebarItem("Analytics", Icons.Default.Analytics,  false) { onItemClick(); onAnalytics() }
            SidebarItem("Reports",   Icons.Default.BarChart,   false) { onItemClick(); onReports() }
            SidebarItem("Revenue",   Icons.Default.TrendingUp, false) { onItemClick(); onRevenue() }

            Spacer(Modifier.height(6.dp))
            SidebarSectionLabel("SETTINGS")
            SidebarItem("Company Settings", Icons.Default.Settings, false) { onItemClick(); onCompanySettings() }
            SidebarItem("Profile",          Icons.Default.Person,   false) { onItemClick(); onProfile() }
            SidebarItem("Activity Logs",    Icons.Default.History,  false) { onItemClick() }
        }

        // Logout at bottom
        SidebarItem("Log Out", Icons.Default.ExitToApp, false) { onLogout() }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text     = text,
        fontSize = 9.sp,
        color    = Color.White.copy(alpha = 0.4f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        color    = if (active) NavyBlue else Color.Transparent,
        shape    = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint     = if (active) Color.White else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 12.sp,
                color      = if (active) Color.White else Color.White.copy(alpha = 0.75f),
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines   = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Metric Card ───────────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    label: String, value: String, subLabel: String,
    icon: ImageVector, iconBg: Color, iconTint: Color,
    subColor: Color, modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
        shadowElevation = 2.dp, modifier = modifier) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null,
                    tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 10.sp, color = TextGray)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subLabel, fontSize = 10.sp, color = subColor)
            }
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    label: String, icon: ImageVector,
    iconBg: Color, iconTint: Color, onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp),
        color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.width(90.dp)) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null,
                    tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, textAlign = TextAlign.Center)
        }
    }
}

// ── Revenue Chart ─────────────────────────────────────────────────────────────

@Composable
private fun AdminRevenueChart(trend: List<RevenueStat>) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
        shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly Revenue Trend", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("This Year", fontSize = 11.sp, color = NavyBlue)
            }
            Spacer(Modifier.height(16.dp))
            val maxVal = trend.maxOfOrNull { it.amount }.let {
                if (it != null && it > 0.0) it else 1.0
            }
            Row(modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom) {
                trend.forEach { stat ->
                    val fraction = (stat.amount / maxVal).toFloat()
                    Column(modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom) {
                        Box(modifier = Modifier.fillMaxWidth()
                            .height((fraction * 100f).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(NavyBlue.copy(alpha = if (fraction > 0.8f) 1f else 0.4f)))
                        Spacer(Modifier.height(4.dp))
                        Text(stat.month, fontSize = 9.sp, color = TextGray,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Recent Institutions Table ─────────────────────────────────────────────────

@Composable
private fun RecentInstitutionsTable(
    institutions: List<RecentInstitution>, modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
        shadowElevation = 2.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent Institutions", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("View All", fontSize = 11.sp, color = NavyBlue)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Institution", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1.5f))
                Text("Plan",        fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                Text("Status",      fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(0.8f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF3F4F6))
            if (institutions.isEmpty()) {
                Text("No institutions yet", fontSize = 11.sp, color = TextGray,
                    modifier = Modifier.padding(vertical = 8.dp))
            } else {
                institutions.forEach { inst ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(inst.name, fontSize = 11.sp, color = TextPrimary,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(inst.plan, fontSize = 11.sp, color = TextGray,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        StatusChip(inst.status, modifier = Modifier.weight(0.8f))
                    }
                }
            }
        }
    }
}

// ── Recent Payments Table ─────────────────────────────────────────────────────

@Composable
private fun RecentPaymentsTable(
    payments: List<RecentPayment>, modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite,
        shadowElevation = 2.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent Payments", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("View All", fontSize = 11.sp, color = NavyBlue)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Institution", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1.5f))
                Text("Amount",      fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                Text("Status",      fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(0.8f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF3F4F6))
            if (payments.isEmpty()) {
                Text("No payments yet", fontSize = 11.sp, color = TextGray,
                    modifier = Modifier.padding(vertical = 8.dp))
            } else {
                payments.forEach { payment ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(payment.institution, fontSize = 11.sp, color = TextPrimary,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatKsh(payment.amount), fontSize = 11.sp, color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        StatusChip(payment.status, modifier = Modifier.weight(0.8f))
                    }
                }
            }
        }
    }
}

// ── Status Chip ───────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bg, textColor) = when (status.lowercase()) {
        "active", "paid", "approved" -> Color(0xFFE8F5E9) to ActiveGreen
        "pending"                    -> Color(0xFFFFF8E1) to PendingAmber
        "suspended", "rejected"      -> Color(0xFFFFEBEE) to RedAccent
        else                         -> Color(0xFFF3F4F6) to TextGray
    }
    Surface(shape = RoundedCornerShape(999.dp), color = bg, modifier = modifier) {
        Text(
            text       = status.replaceFirstChar { it.uppercase() },
            fontSize   = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor,
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines   = 1
        )
    }
}