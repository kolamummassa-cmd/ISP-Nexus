package com.example.ispnexus.ui.theme.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ispnexus.R

private val NavyBlue    = Color(0xFF0D47A1)
private val SidebarBg   = Color(0xFF0D1B3E)
private val PageBg      = Color(0xFFEAF1FB)
private val CardWhite   = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF0D1B3E)
private val TextGray    = Color(0xFF6B7280)

@Composable
fun RoleSelectionScreen(
    onLoginClick: () -> Unit,
    onRegisterCompany: () -> Unit,
    onRegisterStaff: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SidebarBg, Color(0xFF1A3A6B), NavyBlue.copy(alpha = 0.85f))
                )
            )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top section with logo ─────────────────────────────────────
            Spacer(Modifier.height(60.dp))

            Image(
                painter            = painterResource(id = R.drawable.isp_nexus),
                contentDescription = "ISP Nexus Logo",
                modifier           = Modifier.size(100.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "ISP NEXUS",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text     = "Connect. Manage. Grow.",
                fontSize = 14.sp,
                color    = Color.White.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(50.dp))

            // ── Bottom card ───────────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color  = PageBg,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text       = "Welcome! How would you\nlike to get started?",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text      = "Choose your role to continue",
                        fontSize  = 13.sp,
                        color     = TextGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Register as ISP Company ───────────────────────────
                    RoleCard(
                        icon        = Icons.Default.Business,
                        iconBg      = Color(0xFFE3F2FD),
                        iconTint    = NavyBlue,
                        title       = "Register ISP Company",
                        subtitle    = "Create and manage your ISP business on the platform",
                        onClick     = onRegisterCompany
                    )

                    Spacer(Modifier.height(14.dp))

                    // ── Join as Staff Member ──────────────────────────────
                    RoleCard(
                        icon        = Icons.Default.Group,
                        iconBg      = Color(0xFFE8F5E9),
                        iconTint    = Color(0xFF2E7D32),
                        title       = "Join as Staff Member",
                        subtitle    = "Register using your company code to join your team",
                        onClick     = onRegisterStaff
                    )

                    Spacer(Modifier.height(28.dp))

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    Spacer(Modifier.height(20.dp))

                    // ── Already have account ──────────────────────────────
                    Text(
                        text      = "Already have an account?",
                        fontSize  = 13.sp,
                        color     = TextGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick  = onLoginClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Login to your account", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text      = "© 2024 ISP Nexus. All rights reserved.",
                        fontSize  = 11.sp,
                        color     = TextGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Role Card ─────────────────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape           = RoundedCornerShape(18.dp),
        color           = CardWhite,
        shadowElevation = 3.dp,
        modifier        = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier         = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = iconTint, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.sp, color = TextGray, lineHeight = 18.sp)
            }

            Spacer(Modifier.width(8.dp))

            // Arrow
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = Color(0xFFB0BEC5), modifier = Modifier.size(22.dp))
        }
    }
}
