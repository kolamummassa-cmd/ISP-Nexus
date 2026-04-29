package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.R
import com.example.ispnexus.viewmodels.AuthViewModel
import com.example.ispnexus.viewmodels.SuperAdminViewModel
import java.util.Calendar

// ── Colors ────────────────────────────────────────────────────────────────────

private val CorporateBlue  = Color(0xFF0D47A1)
private val PageBackground = Color(0xFFF0F2F5)
private val CardWhite      = Color(0xFFFFFFFF)

// ── Data Model ────────────────────────────────────────────────────────────────

@Immutable
data class DashboardCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val badge: String? = null,
    val badgeIsAlert: Boolean = false,
    val onClick: () -> Unit
)

// ── Greeting ──────────────────────────────────────────────────────────────────

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11  -> "Good morning, Super Admin"
        in 12..16 -> "Good afternoon, Super Admin"
        else      -> "Good evening, Super Admin"
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminTopBar(onLogout: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = CorporateBlue,
            titleContentColor      = Color.White,
            actionIconContentColor = Color.White
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter            = painterResource(id = R.drawable.isp_nexus),
                    contentDescription = "ISP Nexus Logo",
                    modifier           = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text       = "ISP Nexus",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color.White
                    )
                    Text(
                        text     = greeting(),
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector        = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint               = Color.White
                )
            }
        }
    )
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun SuperAdminScreen(
    onPendingClick: () -> Unit,
    onApprovedClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {                                                          // ← function opens here

    val pendingCount  by viewModel.pendingCount.collectAsState()
    val approvedCount by viewModel.approvedCount.collectAsState()
    val totalCount    = pendingCount + approvedCount

    // No remember() — list rebuilds on every recomposition so counts stay live
    val cards = listOf(
        DashboardCardData(
            title          = "Pending Companies",
            subtitle       = "Approve or reject registrations",
            icon           = Icons.Default.PendingActions,
            iconBackground = Color(0xFFFFF4E5),
            iconTint       = Color(0xFFB7791F),
            badge          = if (pendingCount > 0) "$pendingCount new" else null,
            badgeIsAlert   = true,
            onClick        = onPendingClick
        ),
        DashboardCardData(
            title          = "Approved Companies",
            subtitle       = "Manage verified ISPs",
            icon           = Icons.Default.Business,
            iconBackground = Color(0xFFE8F5E9),
            iconTint       = Color(0xFF2E7D32),
            badge          = "$approvedCount",
            badgeIsAlert   = false,
            onClick        = onApprovedClick
        ),
        DashboardCardData(
            title          = "System Analytics",
            subtitle       = "View revenue and usage reports",
            icon           = Icons.Default.Analytics,
            iconBackground = Color(0xFFE3F2FD),
            iconTint       = CorporateBlue,
            onClick        = onAnalyticsClick
        )
    )

    Scaffold(                                                // ← Scaffold is INSIDE the function
        topBar         = { SuperAdminTopBar(onLogout = onLogout) },
        containerColor = PageBackground
    ) { padding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding      = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Stat strip ────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label    = "Total ISPs",
                        value    = totalCount.toString(),
                        color    = Color(0xFF1A1A1A),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label    = "Pending",
                        value    = pendingCount.toString(),
                        color    = Color(0xFFB7791F),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label    = "Approved",
                        value    = approvedCount.toString(),
                        color    = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Section label ─────────────────────────────────────────────
            item {
                Text(
                    text       = "Management",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF6B7280),
                    modifier   = Modifier.padding(bottom = 12.dp)
                )
            }

            // ── Action cards ──────────────────────────────────────────────
            items(
                items = cards,
                key   = { it.title }
            ) { card ->
                ActionCard(card = card)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Footer ────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text      = "ISP Nexus • System Status: Online",
                    fontSize  = 11.sp,
                    color     = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}                                                            // ← function closes here

// ── Stat Card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = CardWhite,
        tonalElevation  = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text       = label,
                fontSize   = 11.sp,
                color      = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = value,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ── Action Card ───────────────────────────────────────────────────────────────

@Composable
private fun ActionCard(card: DashboardCardData) {
    Surface(
        onClick         = card.onClick,
        shape           = RoundedCornerShape(20.dp),
        color           = CardWhite,
        tonalElevation  = 0.dp,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(card.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = card.icon,
                    contentDescription = null,
                    tint               = card.iconTint,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = card.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = card.subtitle,
                    fontSize = 12.sp,
                    color    = Color(0xFF6B7280)
                )
            }

            card.badge?.let { label ->
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (card.badgeIsAlert) Color(0xFFFFE0B2) else Color(0xFFC8E6C9)
                ) {
                    Text(
                        text       = label,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (card.badgeIsAlert) Color(0xFFB7791F) else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}