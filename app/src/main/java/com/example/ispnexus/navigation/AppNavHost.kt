package com.example.ispnexus.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ispnexus.ui.theme.screens.AdminDashboardScreen
import com.example.ispnexus.ui.theme.screens.ApprovedCompaniesScreen
import com.example.ispnexus.ui.theme.screens.PendingCompaniesScreen
import com.example.ispnexus.ui.theme.screens.SuperAdminScreen
import com.example.ispnexus.ui.theme.screens.SystemAnalyticsScreen
import com.example.ispnexus.ui.theme.screens.auth.LoginScreen
import com.example.ispnexus.ui.theme.screens.auth.RegisterCompanyScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {


        composable("login") {
            LoginScreen(
                onNavigateToSuperAdmin = {
                    navController.navigate("super_admin") {
                        popUpTo("login") { inclusive = true }
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


        composable("register") {
            RegisterCompanyScreen(
                onBackToLogin = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }


        composable("super_admin") {
            SuperAdminScreen(
                onPendingClick = { navController.navigate("pending_companies") },
                onApprovedClick = { navController.navigate("approved_companies") },
                onAnalyticsClick = { navController.navigate("system_analytics") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("super_admin") { inclusive = true }
                    }
                }
            )
        }


        composable("pending_companies") {
            PendingCompaniesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Approved Companies ────────────
        composable("approved_companies") {
            ApprovedCompaniesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── System Analytics ──────────────
        composable("system_analytics") {
            SystemAnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Admin Dashboard ───────────────
        composable("admin") {
            AdminDashboardScreen(
//                companyId = "test_company_id",
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("admin") { inclusive = true }
                    }
                }
            )
        }

    }
}