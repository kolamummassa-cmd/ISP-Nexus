package com.example.ispnexus.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ispnexus.ui.theme.screens.SuperAdminScreen
import com.example.ispnexus.ui.theme.screens.auth.LoginScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onNavigateToSuperAdmin = {
                    navController.navigate("super_admin") {
                        popUpTo("login") { inclusive = true } // ← clears login from back stack
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate("admin") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToUser = {
                    navController.navigate("user") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // ── Super Admin ───────────────────────────────────────────────────────
        composable("super_admin") {
            SuperAdminScreen(
                onPendingClick   = { navController.navigate("pending_companies") },
                onApprovedClick  = { navController.navigate("approved_companies") },
                onAnalyticsClick = { navController.navigate("system_analytics") },
                onLogout         = {
                    navController.navigate("login") {
                        popUpTo("super_admin") { inclusive = true } // ← clears dashboard from back stack
                    }
                }
            )
        }

        // ── Admin ─────────────────────────────────────────────────────────────
        composable("admin") {
            Text("Admin Dashboard") // TODO: replace with AdminScreen()
        }

        // ── User ──────────────────────────────────────────────────────────────
        composable("user") {
            Text("User Dashboard") // TODO: replace with UserScreen()
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable("register") {
            Text("Register Screen") // TODO: replace with RegisterScreen()
        }

        // ── Super Admin Sub-screens ───────────────────────────────────────────
        composable("pending_companies") {
            Text("Pending Companies") // TODO: replace with PendingCompaniesScreen()
        }

        composable("approved_companies") {
            Text("Approved Companies") // TODO: replace with ApprovedCompaniesScreen()
        }

        composable("system_analytics") {
            Text("System Analytics") // TODO: replace with SystemAnalyticsScreen()
        }
    }
}