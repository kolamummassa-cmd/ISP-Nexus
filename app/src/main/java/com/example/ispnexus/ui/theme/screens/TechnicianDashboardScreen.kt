package com.example.ispnexus.ui.theme.screens



import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ispnexus.viewmodels.TechnicianDashboardViewModel
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ─── Color Palette ───────────────────────────────────────────────────────────
private val PrimaryBlue   = Color(0xFF1A73E8)
private val LightBlue     = Color(0xFFEAF2FF)
private val OrangeAccent  = Color(0xFFF5A623)
private val LightOrange   = Color(0xFFFFF8EE)
private val GreenAccent   = Color(0xFF34A853)
private val LightGreen    = Color(0xFFEEF9F1)
private val RedAccent     = Color(0xFFEA4335)
private val LightRed      = Color(0xFFFFF0EF)
private val TextPrimary   = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)
private val DividerColor  = Color(0xFFF0F0F0)
private val BackgroundColor = Color(0xFFF8F9FA)
private val CardBackground  = Color.White
private val OnlineGreen   = Color(0xFF00C853)
private val OfflineRed    = Color(0xFFD32F2F)

// ─── Data Models ─────────────────────────────────────────────────────────────
data class TicketSummary(
    val label: String,
    val count: Int,
    val delta: String,
    val deltaPositive: Boolean,
    val bgColor: Color,
    val textColor: Color,
    val iconColor: Color
)

data class AssignedTicket(
    val id: String,
    val title: String,
    val customer: String,
    val priority: TicketPriority,
    val status: TicketStatus
)

enum class TicketPriority(val label: String, val color: Color, val bgColor: Color) {
    HIGH("High", Color(0xFFD32F2F), Color(0xFFFFEBEE)),
    MEDIUM("Medium", Color(0xFFF57C00), Color(0xFFFFF3E0)),
    LOW("Low", Color(0xFF388E3C), Color(0xFFE8F5E9))
}

enum class TicketStatus(val label: String, val color: Color, val bgColor: Color) {
    OPEN("Open", Color(0xFFD32F2F), Color(0xFFFFEBEE)),
    IN_PROGRESS("In Progress", Color(0xFFF57C00), Color(0xFFFFF3E0)),
    RESOLVED("Resolved", Color(0xFF388E3C), Color(0xFFE8F5E9))
}

data class FieldVisit(
    val time: String,
    val location: String,
    val task: String,
    val status: String
)

data class NetworkDevice(
    val name: String,
    val location: String,
    val isOnline: Boolean,
    val uptimeOrLastSeen: String,
    val iconType: DeviceType
)

enum class DeviceType { ROUTER, SWITCH, ACCESS_POINT }

enum class BottomNavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Home, Icons.Filled.Home),
    TICKETS("My Tickets", Icons.Outlined.List, Icons.Filled.List),
    FIELD_VISITS("Field Visits", Icons.Outlined.LocationOn, Icons.Filled.LocationOn),
    NETWORK("Network Devices", Icons.Outlined.Router, Icons.Filled.Router),
    PROFILE("Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

// ─── Main Dashboard Screen ────────────────────────────────────────────────────
@Composable
fun TechnicianDashboardScreen(
    viewModel: TechnicianDashboardViewModel = viewModel(),
    onTicketClick: (String) -> Unit = {},
    onViewAllTickets: () -> Unit = {},
    onViewAllVisits: () -> Unit = {},
    onViewAllDevices: () -> Unit = {},
    onNavItemClick: (BottomNavItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNav by remember { mutableStateOf(BottomNavItem.DASHBOARD) }

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            TechnicianBottomNav(
                selected = selectedNav,
                onItemClick = {
                    selectedNav = it
                    onNavItemClick(it)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Top Bar
            item {
                TechnicianTopBar(
                    notificationCount = uiState.notificationCount,
                    onMenuClick = { /* handle drawer */ },
                    onNotificationClick = { /* handle notifications */ },
                    onProfileClick = { /* handle profile */ }
                )
            }

            // Welcome Header
            item {
                WelcomeHeader(technicianName = uiState.technicianName)
            }

            // Stats Cards
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TicketStatsRow(stats = uiState.ticketStats)
            }

            // Assigned Tickets
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "My Assigned Tickets", onViewAll = onViewAllTickets)
                Spacer(modifier = Modifier.height(8.dp))
                AssignedTicketsCard(
                    tickets = uiState.assignedTickets,
                    onTicketClick = onTicketClick
                )
            }

            // Today's Field Visits
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Today's Field Visits", onViewAll = onViewAllVisits)
                Spacer(modifier = Modifier.height(8.dp))
                FieldVisitsCard(visits = uiState.fieldVisits)
            }

            // Network Devices
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Network Devices (Assigned)", onViewAll = onViewAllDevices)
                Spacer(modifier = Modifier.height(8.dp))
                NetworkDevicesCard(
                    devices = uiState.networkDevices,
                    onDeviceClick = { /* handle device click */ }
                )
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────
@Composable
fun TechnicianTopBar(
    companyName: String = "",
    notificationCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Hamburger menu
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = TextPrimary
            )
        }

        // Logo
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text         = companyName.ifEmpty { "ISP NEXUS" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                letterSpacing = 1.sp
            )
            Text(
                text = "CONNECT • MANAGE • GROW",
                fontSize = 7.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        // Right icons
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notification Bell with badge
            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(RedAccent, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notificationCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Profile avatar with online indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                // Online dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(OnlineGreen, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}

// ─── Welcome Header ───────────────────────────────────────────────────────────
@Composable
private fun WelcomeHeader(technicianName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row {
            Text(
                text = "Welcome back, ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = technicianName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "👋", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Here's what's happening with your work today.",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

// ─── Ticket Stats Row ─────────────────────────────────────────────────────────
@Composable
fun TicketStatsRow(stats: List<TicketSummary>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            TicketStatCard(
                modifier = Modifier.weight(1f),
                stat = stat
            )
        }
    }
}

@Composable
fun TicketStatCard(modifier: Modifier = Modifier, stat: TicketSummary) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = stat.bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stat.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = stat.textColor,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(stat.iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (stat.label) {
                        "Open Tickets" -> Icons.Default.Description
                        "In-Progress" -> Icons.Default.AccessTime
                        "Resolved Today" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = stat.iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stat.count.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat.delta,
                fontSize = 10.sp,
                color = if (stat.deltaPositive) GreenAccent else RedAccent,
                lineHeight = 12.sp
            )
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "View All",
            fontSize = 13.sp,
            color = PrimaryBlue,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
}

// ─── Assigned Tickets Card ────────────────────────────────────────────────────
@Composable
fun AssignedTicketsCard(
    tickets: List<AssignedTicket>,
    onTicketClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            tickets.forEachIndexed { index, ticket ->
                TicketRow(ticket = ticket, onClick = { onTicketClick(ticket.id) })
                if (index < tickets.lastIndex) {
                    HorizontalDivider(color = DividerColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun TicketRow(ticket: AssignedTicket, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ticket ID
        Text(
            text = ticket.id,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryBlue,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        // Title & Customer
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticket.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ticket.customer,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Priority Badge
        PriorityBadge(priority = ticket.priority)
        Spacer(modifier = Modifier.width(6.dp))

        // Status Badge
        StatusBadge(status = ticket.status)
        Spacer(modifier = Modifier.width(6.dp))

        // Arrow
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: TicketPriority) {
    Box(
        modifier = Modifier
            .background(priority.bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = priority.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = priority.color
        )
    }
}

@Composable
fun StatusBadge(status: TicketStatus) {
    Box(
        modifier = Modifier
            .background(status.bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = status.color
        )
    }
}

// ─── Field Visits Card ────────────────────────────────────────────────────────
@Composable
fun FieldVisitsCard(visits: List<FieldVisit>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            visits.forEachIndexed { index, visit ->
                FieldVisitRow(
                    visit = visit,
                    isLast = index == visits.lastIndex
                )
            }
        }
    }
}

@Composable
fun FieldVisitRow(visit: FieldVisit, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(PrimaryBlue, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(PrimaryBlue.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Visit Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (!isLast) 8.dp else 0.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visit.time,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Text(
                    text = visit.location,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = visit.task,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(LightBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = visit.status,
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Network Devices Card ─────────────────────────────────────────────────────
@Composable
fun NetworkDevicesCard(
    devices: List<NetworkDevice>,
    onDeviceClick: (NetworkDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            devices.forEachIndexed { index, device ->
                NetworkDeviceRow(device = device, onClick = { onDeviceClick(device) })
                if (index < devices.lastIndex) {
                    HorizontalDivider(color = DividerColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun NetworkDeviceRow(device: NetworkDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (device.isOnline) LightGreen else LightRed,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (device.iconType) {
                    DeviceType.ROUTER -> Icons.Default.Router
                    DeviceType.SWITCH -> Icons.Default.DeviceHub
                    DeviceType.ACCESS_POINT -> Icons.Default.Wifi
                },
                contentDescription = null,
                tint = if (device.isOnline) GreenAccent else RedAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Device Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (device.isOnline) OnlineGreen else OfflineRed,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (device.isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = if (device.isOnline) OnlineGreen else OfflineRed,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " • ${device.uptimeOrLastSeen}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
fun TechnicianBottomNav(
    selected: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = CardBackground,
        tonalElevation = 8.dp
    ) {
        BottomNavItem.entries.forEach { item ->
            val isSelected = selected == item
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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