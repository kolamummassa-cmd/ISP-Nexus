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
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
import com.example.ispnexus.R
import java.util.Calendar

// ─────────────────────────────────────────────
// Corporate Theme Colors
// ─────────────────────────────────────────────

val CorporateBlue = Color(0xFF0D47A1)
val LightBackground = Color(0xFFF4F6F8)

// ─────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────

@Immutable
data class DashboardCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val badge: String? = null,
    val badgeIsAlert: Boolean = false,
    val onClick: () -> Unit
)

@Immutable
data class StatItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

// ─────────────────────────────────────────────
// Greeting Helper
// ─────────────────────────────────────────────

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning, Admin"
        in 12..16 -> "Good afternoon, Admin"
        else -> "Good evening, Admin"
    }
}

// ─────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    totalIsps: Int = 142,
    pendingCount: Int = 8,
    approvedCount: Int = 134,
    onPendingClick: () -> Unit = {},
    onApprovedClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {}
) {

    val stats = remember {
        listOf(
            StatItem("Total ISPs", "$totalIsps"),
            StatItem("Pending", "$pendingCount", Color(0xFFB7791F)),
            StatItem("Approved", "$approvedCount", Color(0xFF2E7D32))
        )
    }

    val cards = remember {
        listOf(
            DashboardCardData(
                title = "Pending Companies",
                subtitle = "Approve or reject registrations",
                icon = Icons.Default.PendingActions,
                iconTint = Color(0xFFB7791F),
                iconBackground = Color(0xFFFFF4E5),
                badge = if (pendingCount > 0) "$pendingCount new" else null,
                badgeIsAlert = true,
                onClick = onPendingClick
            ),
            DashboardCardData(
                title = "Approved Companies",
                subtitle = "Manage verified ISPs",
                icon = Icons.Default.Business,
                iconTint = CorporateBlue,
                iconBackground = Color(0xFFE3F2FD),
                badge = "$approvedCount",
                onClick = onApprovedClick
            ),
            DashboardCardData(
                title = "System Analytics",
                subtitle = "View revenue and usage reports",
                icon = Icons.Default.Analytics,
                iconTint = CorporateBlue,
                iconBackground = Color(0xFFE8EAF6),
                onClick = onAnalyticsClick
            )
        )
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CorporateBlue,
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Image(
                            painter = painterResource(id = R.drawable.isp_nexus),
                            contentDescription = "ISP Nexus Logo",
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "ISP Nexus",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = greeting(),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 18.dp)
        ) {

            item { StatStrip(stats) }

            item {
                Text(
                    text = "Management",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            items(cards) { card ->
                DashboardCard(card)
            }

            item {
                Text(
                    text = "ISP Nexus • System Status: Online",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = Color.Gray
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Stat Strip
// ─────────────────────────────────────────────

@Composable
fun StatStrip(stats: List<StatItem>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach {
            StatCard(it, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(stat: StatItem, modifier: Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(stat.label, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stat.value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = stat.valueColor ?: Color.Black
            )
        }
    }
}

// ─────────────────────────────────────────────
// Dashboard Card
// ─────────────────────────────────────────────

@Composable
fun DashboardCard(card: DashboardCardData) {
    ElevatedCard(
        onClick = card.onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(card.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = card.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(card.title, fontWeight = FontWeight.SemiBold)
                Text(
                    card.subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            card.badge?.let {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (card.badgeIsAlert) Color(0xFFFFE0B2) else Color(0xFFC8E6C9)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}