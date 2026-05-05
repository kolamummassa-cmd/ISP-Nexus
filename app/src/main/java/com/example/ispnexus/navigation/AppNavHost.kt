package com.example.ispnexus.navigation

import com.example.ispnexus.ui.theme.screens.PendingApprovalScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.ispnexus.ui.theme.screens.auth.RoleSelectionScreen
import com.example.ispnexus.viewmodels.AdminDashboardState
import com.example.ispnexus.viewmodels.AdminViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ispnexus.ui.theme.screens.ManageStaffScreen
import com.example.ispnexus.ui.theme.screens.StaffRejectedScreen
import com.example.ispnexus.ui.theme.screens.StaffWaitingScreen
import com.example.ispnexus.ui.theme.screens.TechnicianDashboardScreen
import com.example.ispnexus.ui.theme.screens.auth.StaffRegisterScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "role_selection"
    ) {

        composable("role_selection"){
            RoleSelectionScreen(
                onLoginClick = {navController.navigate("login")},
                onRegisterCompany = {navController.navigate("register")},
                onRegisterStaff = {navController.navigate("staff_register")}
            )
        }


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
                onNavigateToTechnician = {                                    // ← add this
                    navController.navigate("technician_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToFinance = {                                       // ← add this
                    navController.navigate("finance_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("role_selection")
                }
            )
        }


        composable("register") {
            RegisterCompanyScreen(
                onBackToLogin = { navController.popBackStack() },
                onRegistrationSuccess = { companyName ->   // ← receives companyName
                    navController.navigate("pending_approval/$companyName") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("pending_approval/{companyName}") { backStackEntry ->
            val companyName = backStackEntry.arguments?.getString("companyName") ?: ""
            PendingApprovalScreen(
                companyName = companyName,
                submittedAt = System.currentTimeMillis(),
                onLogout    = {
                    navController.navigate("login") {
                        popUpTo("pending_approval/{companyName}") { inclusive = true }
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
            val adminViewModel: AdminViewModel = viewModel()
            val state by adminViewModel.state.collectAsState()

            when (val s = state) {

                is AdminDashboardState.Loading -> {
                    // ← fixes white screen during loading
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0D47A1))
                    }
                }

                is AdminDashboardState.Error -> {
                    // ← fixes white screen on error — shows actual error message
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text      = s.message,
                                color     = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { adminViewModel.loadDashboard() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is AdminDashboardState.Success -> {
                    if (s.data.companyStatus == "Approved") {
                        AdminDashboardScreen(
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo("admin") { inclusive = true }
                                }
                            },
                            onStaff = {navController.navigate("staff")}
                        )
                    } else {
                        PendingApprovalScreen(
                            companyName = s.data.companyName,
                            submittedAt = s.data.submittedAt ?: System.currentTimeMillis(),
                            onLogout    = {
                                navController.navigate("login") {
                                    popUpTo("admin") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Staff Register ────────────────────────────────────────────────────────
        composable("staff_register") {
            StaffRegisterScreen(
                onBackToLogin          = { navController.popBackStack() },
                onRegistrationSuccess  = {
                    navController.navigate("staff_waiting") {
                        popUpTo("staff_register") { inclusive = true }
                    }
                }
            )
        }

// ── Staff Waiting ─────────────────────────────────────────────────────────
        composable("staff_waiting") {
            StaffWaitingScreen(
                onLogout  = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRefresh = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

// ── Staff Rejected ────────────────────────────────────────────────────────
        composable("staff_rejected") {
            StaffRejectedScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("staff") {
            ManageStaffScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Technician Dashboard ──────────────────────────────────────────────────────
        composable("technician_dashboard") {
            TechnicianDashboardScreen(
                onTicketClick    = { },
                onViewAllTickets = { },
                onViewAllVisits  = { },
                onViewAllDevices = { }
            )
        }

// ── Finance Dashboard ─────────────────────────────────────────────────────────
        composable("finance_dashboard") {
            // TODO: replace with FinanceDashboardScreen() once built
            Text("Finance Dashboard")
        }

    }
}