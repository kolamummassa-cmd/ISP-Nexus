package com.example.ispnexus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.viewmodels.AuthViewModel

// ── Allowed roles per screen ──────────────────────────────────────────────────

object ScreenAccess {
    val adminOnly    = listOf("admin")
    val financeOnly  = listOf("finance")
    val techOnly     = listOf("technician")
    val shared       = listOf("admin", "finance")
    val allStaff     = listOf("admin", "finance", "technician")
}

// ── Role Guard ────────────────────────────────────────────────────────────────

@Composable
fun RoleGuard(
    allowedRoles: List<String>,
    userRole: String,
    userPosition: String = "",
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    // Determine the effective role
    val effectiveRole = when (userRole.lowercase()) {
        "admin", "company_admin" -> "admin"
        "super_admin"            -> "super_admin"
        "staff"                  -> userPosition.lowercase()  // technician or finance
        else                     -> ""
    }

    LaunchedEffect(effectiveRole) {
        if (effectiveRole !in allowedRoles) {
            onAccessDenied()
        }
    }

    if (effectiveRole in allowedRoles) {
        content()
    }
}

