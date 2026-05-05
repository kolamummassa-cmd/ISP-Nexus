package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispnexus.R

private val NavyBlue    = Color(0xFF0D47A1)
private val SidebarBg   = Color(0xFF0D1B3E)
private val PageBg      = Color(0xFFEAF1FB)
private val AmberOrange = Color(0xFFE65100)
private val AmberBg     = Color(0xFFFFF3E0)
private val InfoBlue    = Color(0xFF1565C0)
private val InfoBg      = Color(0xFFE3F2FD)
private val RejectRed   = Color(0xFFC62828)
private val RejectBg    = Color(0xFFFFEBEE)

// ── Staff Waiting Screen ──────────────────────────────────────────────────────

@Composable
fun StaffWaitingScreen(
    staffName: String = "",
    companyName: String = "",
    onLogout: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(PageBg, Color(0xFFDCEAF9), PageBg))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SidebarBg)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.isp_nexus),
                        contentDescription = "Logo", modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ISP NEXUS", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color.White)
                        Text("Connect. Manage. Grow.", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }

            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape           = RoundedCornerShape(24.dp),
                    color           = Color.White,
                    shadowElevation = 6.dp,
                    modifier        = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Icon
                        Box(
                            modifier         = Modifier.size(100.dp).clip(CircleShape)
                                .background(AmberBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null,
                                tint = AmberOrange, modifier = Modifier.size(54.dp))
                        }

                        Spacer(Modifier.height(20.dp))

                        Text("Waiting for Approval", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E),
                            textAlign = TextAlign.Center)

                        Spacer(Modifier.height(10.dp))

                        // Pending badge
                        Surface(shape = RoundedCornerShape(999.dp), color = AmberBg) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null,
                                    tint = AmberOrange, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PENDING APPROVAL", fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, color = AmberOrange,
                                    letterSpacing = 1.sp)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text      = "Your account is waiting for approval from your company admin.\n\nYou will gain access to your dashboard once your account is approved.",
                            fontSize  = 14.sp,
                            color     = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        // Info box
                        Surface(shape = RoundedCornerShape(14.dp), color = InfoBg) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, contentDescription = null,
                                    tint = InfoBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("What happens next?", fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold, color = InfoBlue)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Your company admin will review your registration and either approve or reject your account. You'll be able to login once approved.",
                                        fontSize  = 12.sp, color = Color.Gray, lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (staffName.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            Spacer(Modifier.height(12.dp))
                            InfoDetailRow("Staff Name",   staffName)
                            Spacer(Modifier.height(8.dp))
                            InfoDetailRow("Company",      companyName.ifEmpty { "—" })
                        }

                        Spacer(Modifier.height(20.dp))

                        // Refresh button
                        OutlinedButton(
                            onClick  = onRefresh,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check Approval Status")
                        }

                        Spacer(Modifier.height(10.dp))

                        // Logout button
                        Button(
                            onClick  = onLogout,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White)
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("© 2024 ISP Nexus. All rights reserved.",
                            fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Staff Rejected Screen ─────────────────────────────────────────────────────

@Composable
fun StaffRejectedScreen(
    staffName: String = "",
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(PageBg, Color(0xFFDCEAF9), PageBg))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SidebarBg)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.isp_nexus),
                        contentDescription = "Logo", modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {

                        Text(
                            text       = companyName.ifEmpty { "ISP NEXUS" },  // ← shows company name
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text("Connect. Manage. Grow.", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }

            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape           = RoundedCornerShape(24.dp),
                    color           = Color.White,
                    shadowElevation = 6.dp,
                    modifier        = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Icon
                        Box(
                            modifier         = Modifier.size(100.dp).clip(CircleShape)
                                .background(RejectBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null,
                                tint = RejectRed, modifier = Modifier.size(54.dp))
                        }

                        Spacer(Modifier.height(20.dp))

                        Text("Registration Rejected", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E),
                            textAlign = TextAlign.Center)

                        Spacer(Modifier.height(10.dp))

                        Surface(shape = RoundedCornerShape(999.dp), color = RejectBg) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null,
                                    tint = RejectRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ACCESS DENIED", fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, color = RejectRed,
                                    letterSpacing = 1.sp)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text      = "Unfortunately your registration has been rejected by your company admin.\n\nPlease contact your company admin for more information.",
                            fontSize  = 14.sp,
                            color     = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick  = onLogout,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Info Detail Row ───────────────────────────────────────────────────────────

@Composable
private fun InfoDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B3E))
    }
}
