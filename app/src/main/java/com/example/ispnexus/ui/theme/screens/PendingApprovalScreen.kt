package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.ispnexus.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue     = Color(0xFF0D47A1)
private val SidebarBg    = Color(0xFF0D1B3E)
private val PageBg       = Color(0xFFEAF1FB)
private val CardWhite    = Color(0xFFFFFFFF)
private val AmberOrange  = Color(0xFFE65100)
private val AmberBg      = Color(0xFFFFF3E0)
private val InfoBlue     = Color(0xFF1565C0)
private val InfoBg       = Color(0xFFE3F2FD)
private val GreenSafe    = Color(0xFF2E7D32)
private val TextPrimary  = Color(0xFF0D1B3E)
private val TextGray     = Color(0xFF6B7280)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PendingApprovalScreen(
    companyName: String = "",
    submittedAt: Long = System.currentTimeMillis(),
    onLogout: () -> Unit,
    onContactSupport: () -> Unit = {}
) {
    val formattedDate = remember(submittedAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(submittedAt))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PageBg,
                        Color(0xFFDCEAF9),
                        PageBg
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ── Top Bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InfoBlue)
                    .padding(horizontal = 20.dp, vertical = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter            = painterResource(id = R.drawable.isp_nexus),
                        contentDescription = "ISP Nexus Logo",
                        modifier           = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text       = companyName.ifEmpty { "ISP NEXUS" },
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            text     = "Connect. Manage. Grow.",
                            fontSize = 18.sp,
                            color    = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // ── Main Content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Card ──────────────────────────────────────────────────
                Surface(
                    shape           = RoundedCornerShape(24.dp),
                    color           = CardWhite,
                    shadowElevation = 6.dp,
                    modifier        = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // ── Pending icon ──────────────────────────────────
                        Box(
                            modifier         = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(AmberBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.PendingActions,
                                contentDescription = null,
                                tint               = AmberOrange,
                                modifier           = Modifier.size(60.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Title ─────────────────────────────────────────
                        Text(
                            text       = "Account Under Review",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        // ── Pending badge ─────────────────────────────────
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = AmberBg
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint               = AmberOrange,
                                    modifier           = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text       = "PENDING APPROVAL",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = AmberOrange,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(20.dp))

                        // ── Description ───────────────────────────────────
                        Text(
                            text      = "Thank you for registering with ISP Nexus.\nOur Super Admin team is currently reviewing your company details and documents.",
                            fontSize  = 14.sp,
                            color     = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── What happens next box ─────────────────────────
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = InfoBg
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Info,
                                    contentDescription = null,
                                    tint               = InfoBlue,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text       = "What happens next?",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = InfoBlue
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text      = "Once your account is approved, you will receive an email notification and you will be able to access your dashboard.",
                                        fontSize  = 13.sp,
                                        color     = TextGray,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(16.dp))

                        // ── Info rows ─────────────────────────────────────
                        InfoRow(
                            icon  = Icons.Default.CalendarToday,
                            label = "Submitted On",
                            value = formattedDate
                        )

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        Spacer(Modifier.height(14.dp))

                        InfoRow(
                            icon  = Icons.Default.Business,
                            label = "Company Name",
                            value = companyName.ifEmpty { "—" }
                        )

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        Spacer(Modifier.height(14.dp))

                        // Need help row
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(InfoBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null,
                                    tint = InfoBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Need Help?", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = TextPrimary,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = onContactSupport) {
                                Text("Contact Support", fontSize = 13.sp,
                                    color = NavyBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(16.dp))

                        // ── Safety note ───────────────────────────────────
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null,
                                tint = GreenSafe, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text     = "Your data is safe with us. We appreciate your patience.",
                                fontSize = 12.sp,
                                color    = TextGray
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Logout button ─────────────────────────────────
                        Button(
                            onClick  = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Logout", fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Footer ────────────────────────────────────────────────
                Text(
                    text      = "© 2024 ISP Nexus. All rights reserved.",
                    fontSize  = 11.sp,
                    color     = TextGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Info Row ──────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(InfoBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null,
                tint = InfoBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, color = TextPrimary,
            modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextGray,
            textAlign = TextAlign.End)
    }
}

@Preview(showBackground = true, showSystemUi = true )
@Composable
fun PendingApprovalScreenPreview() {
    PendingApprovalScreen(
        companyName = "Nairobi Fiber Ltd",
        submittedAt = System.currentTimeMillis(),
        onLogout = {},
        onContactSupport = {}
    )
}
