package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.AnalyticsData
import com.example.ispnexus.viewmodels.AnalyticsState
import com.example.ispnexus.viewmodels.AnalyticsViewModel
import com.example.ispnexus.viewmodels.MonthStat

// ── Colors ────────────────────────────────────────────────────────────────────

private val CorporateBlue = Color(0xFF0D47A1)
private val PageBg        = Color(0xFFF0F2F5)
private val CardWhite     = Color(0xFFFFFFFF)
private val ActiveGreen   = Color(0xFF2E7D32)
private val PendingAmber  = Color(0xFFB7791F)
private val SuspendedRed  = Color(0xFFC62828)
private val RejectedRed   = Color(0xFFB71C1C)
private val PurpleAccent  = Color(0xFF6A1B9A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = CorporateBlue,
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
                        Text(
                            text       = "System Analytics",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp
                        )
                        Text(
                            text     = "Platform performance overview",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.80f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {

                is AnalyticsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = CorporateBlue
                    )
                }

                is AnalyticsState.Error -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text      = s.message,
                            color     = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadAnalytics() },
                            colors  = ButtonDefaults.buttonColors(containerColor = CorporateBlue)
                        ) { Text("Retry") }
                    }
                }

                is AnalyticsState.Success -> {
                    AnalyticsContent(data = s.data)
                }
            }
        }
    }
}

// ── Analytics Content ─────────────────────────────────────────────────────────

@Composable
private fun AnalyticsContent(data: AnalyticsData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        SectionLabel("Key Metrics")

        // Row 1: Total Companies | Active Companies
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label    = "Total Companies",
                value    = data.totalCompanies.toString(),
                color    = CorporateBlue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label    = "Active Companies",
                value    = data.activeCompanies.toString(),
                color    = ActiveGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Pending | Suspended
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label    = "Pending",
                value    = data.pendingCompanies.toString(),
                color    = PendingAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label    = "Suspended",
                value    = data.suspendedCompanies.toString(),
                color    = SuspendedRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Total Institutions | Growth Rate
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label    = "Total Institutions",
                value    = data.totalInstitutions.toString(),
                color    = PurpleAccent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label    = "Growth Rate",
                value    = "${"%.1f".format(data.growthRate)}%",
                color    = if (data.growthRate >= 0) ActiveGreen else SuspendedRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 4: Rejected Companies (full width — stands out as a warning)
        RejectedCard(count = data.rejectedCompanies)

        Spacer(modifier = Modifier.height(4.dp))

        // ── Bar Chart ─────────────────────────────────────────────────────
        SectionLabel("Company Growth (Last 6 Months)")
        BarChart(monthStats = data.monthlyGrowth)

        Spacer(modifier = Modifier.height(4.dp))

        // ── Platform Health ───────────────────────────────────────────────
        SectionLabel("Platform Health")
        PlatformHealthCard(data = data)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ── Metric Card ───────────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = label,
                fontSize   = 11.sp,
                color      = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = value,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ── Rejected Card (full width, red tinted background) ────────────────────────

@Composable
private fun RejectedCard(count: Int) {
    Surface(
        shape           = RoundedCornerShape(16.dp),
        color           = Color(0xFFFFEBEE),   // light red background
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Rejected Companies",
                    fontSize   = 11.sp,
                    color      = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = count.toString(),
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RejectedRed
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFFCDD2)
            ) {
                Text(
                    text       = if (count == 0) "None" else "$count rejected",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RejectedRed,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Section Label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Color(0xFF374151)
    )
}

// ── Bar Chart ─────────────────────────────────────────────────────────────────

@Composable
private fun BarChart(monthStats: List<MonthStat>) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            val maxVal = monthStats.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                monthStats.forEach { stat ->
                    val fraction = stat.count.toFloat() / maxVal
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (stat.count > 0) {
                            Text(
                                text       = stat.count.toString(),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = CorporateBlue,
                                textAlign  = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((fraction * 120f).coerceAtLeast(6f).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (stat.count > 0) CorporateBlue else Color(0xFFE5E7EB)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text      = stat.month,
                            fontSize  = 10.sp,
                            color     = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CorporateBlue)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text     = "New companies registered",
                    fontSize = 11.sp,
                    color    = Color(0xFF6B7280)
                )
            }
        }
    }
}

// ── Platform Health Card ──────────────────────────────────────────────────────

@Composable
private fun PlatformHealthCard(data: AnalyticsData) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            HealthRow(
                label      = "Approval Rate",
                value      = if (data.totalCompanies > 0)
                    "${"%.0f".format(data.activeCompanies.toFloat() / data.totalCompanies * 100)}%"
                else "0%",
                valueColor = ActiveGreen
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF3F4F6))

            HealthRow(
                label      = "Rejection Rate",
                value      = if (data.totalCompanies > 0)
                    "${"%.0f".format(data.rejectedCompanies.toFloat() / data.totalCompanies * 100)}%"
                else "0%",
                valueColor = RejectedRed
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF3F4F6))

            HealthRow(
                label      = "Pending Rate",
                value      = if (data.totalCompanies > 0)
                    "${"%.0f".format(data.pendingCompanies.toFloat() / data.totalCompanies * 100)}%"
                else "0%",
                valueColor = PendingAmber
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF3F4F6))

            HealthRow(
                label      = "Total Subscriptions",
                value      = data.totalSubscriptions.toString(),
                valueColor = CorporateBlue
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF3F4F6))

            HealthRow(
                label      = "Month-on-Month Growth",
                value      = "${"%.1f".format(data.growthRate)}%",
                valueColor = if (data.growthRate >= 0) ActiveGreen else SuspendedRed
            )
        }
    }
}

// ── Health Row ────────────────────────────────────────────────────────────────

@Composable
private fun HealthRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF4B5563))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}