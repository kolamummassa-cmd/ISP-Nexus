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
import com.example.ispnexus.ui.theme.screens.DefaultersScreen
import com.example.ispnexus.ui.theme.screens.FinanceDashboardScreen
import com.example.ispnexus.ui.theme.screens.InstitutionsScreen
import com.example.ispnexus.ui.theme.screens.InvoicesScreen
import com.example.ispnexus.ui.theme.screens.ManageStaffScreen
import com.example.ispnexus.ui.theme.screens.PaymentsScreen
import com.example.ispnexus.ui.theme.screens.PlansScreen
import com.example.ispnexus.ui.theme.screens.ReportsScreen
import com.example.ispnexus.ui.theme.screens.StaffRejectedScreen
import com.example.ispnexus.ui.theme.screens.StaffWaitingScreen
import com.example.ispnexus.ui.theme.screens.SubscriptionsScreen
import com.example.ispnexus.ui.theme.screens.TechnicianDashboardScreen
import com.example.ispnexus.ui.theme.screens.auth.RecordPaymentScreen
import com.example.ispnexus.ui.theme.screens.auth.StaffRegisterScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = ROLE_SELECTION
    ) {

        // ── Role Selection ────────────────────────────────────────────────────
        composable(ROLE_SELECTION) {
            RoleSelectionScreen(
                onLoginClick      = { navController.navigate(LOG_IN) },
                onRegisterCompany = { navController.navigate(REGISTER_COMPANY) },
                onRegisterStaff   = { navController.navigate(STAFF_REGISTER) }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(LOG_IN) {
            LoginScreen(
                onNavigateToSuperAdmin = {
                    navController.navigate(SUPER_ADMIN) {
                        popUpTo(LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(ADMIN) {
                        popUpTo(LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToUser = {
                    navController.navigate("user") {
                        popUpTo(LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToTechnician = {
                    navController.navigate(TECHNICIAN_DASHBOARD) {
                        popUpTo(LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToFinance = {
                    navController.navigate(FINANCE_DASHBOARD) {
                        popUpTo(LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(ROLE_SELECTION)
                }
            )
        }

        // ── Register Company ──────────────────────────────────────────────────
        composable(REGISTER_COMPANY) {
            RegisterCompanyScreen(
                onBackToLogin         = { navController.popBackStack() },
                onRegistrationSuccess = { companyName ->
                    navController.navigate("pending_approval/$companyName") {
                        popUpTo(REGISTER_COMPANY) { inclusive = true }
                    }
                }
            )
        }

        // ── Pending Approval ──────────────────────────────────────────────────
        composable("pending_approval/{companyName}") { backStackEntry ->
            val companyName = backStackEntry.arguments?.getString("companyName") ?: ""
            PendingApprovalScreen(
                companyName = companyName,
                submittedAt = System.currentTimeMillis(),
                onLogout    = {
                    navController.navigate(LOG_IN) {
                        popUpTo("pending_approval/{companyName}") { inclusive = true }
                    }
                }
            )
        }

        // ── Super Admin ───────────────────────────────────────────────────────
        composable(SUPER_ADMIN) {
            SuperAdminScreen(
                onPendingClick   = { navController.navigate("pending_companies") },
                onApprovedClick  = { navController.navigate(APPROVED_COMPANIES) },
                onAnalyticsClick = { navController.navigate(SYSTEM_ANALYTICS) },
                onLogout         = {
                    navController.navigate(LOG_IN) {
                        popUpTo(SUPER_ADMIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Pending Companies ─────────────────────────────────────────────────
        composable("pending_companies") {
            PendingCompaniesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Approved Companies ────────────────────────────────────────────────
        composable(APPROVED_COMPANIES) {
            ApprovedCompaniesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── System Analytics ──────────────────────────────────────────────────
        composable(SYSTEM_ANALYTICS) {
            SystemAnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Admin Dashboard ───────────────────────────────────────────────────
        composable(ADMIN) {
            val adminViewModel: AdminViewModel = viewModel()
            val state by adminViewModel.state.collectAsState()

            when (val s = state) {

                is AdminDashboardState.Loading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0D47A1))
                    }
                }

                is AdminDashboardState.Error -> {
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
                            onLogout          = {
                                navController.navigate(LOG_IN) {
                                    popUpTo(ADMIN) { inclusive = true }
                                }
                            },
                            onInstitutions    = { navController.navigate(INSTITUTIONS) },
                            onSubscriptions   = { navController.navigate(SUBSCRIPTIONS) },
                            onPlans           = { navController.navigate(PLANS) },
                            onPayments        = { navController.navigate(PAYMENTS) },
                            onInvoices        = { navController.navigate(INVOICES) },
                            onStaff           = { navController.navigate(STAFF) },
                            onTechnicians     = { navController.navigate("technicians") },
                            onSupportTickets  = { navController.navigate("tickets") },
                            onAnalytics       = { navController.navigate("analytics") },
                            onReports         = { navController.navigate(REPORTS) },
                            onRevenue         = { navController.navigate("revenue") },
                            onCompanySettings = { navController.navigate("company_settings") },
                            onProfile         = { navController.navigate("profile") }
                        )
                    } else {
                        PendingApprovalScreen(
                            companyName = s.data.companyName,
                            submittedAt = s.data.submittedAt ?: System.currentTimeMillis(),
                            onLogout    = {
                                navController.navigate(LOG_IN) {
                                    popUpTo(ADMIN) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Staff Register ────────────────────────────────────────────────────
        composable(STAFF_REGISTER) {
            StaffRegisterScreen(
                onBackToLogin         = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate(STAFF_WAITING) {
                        popUpTo(STAFF_REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // ── Staff Waiting ─────────────────────────────────────────────────────
        composable(STAFF_WAITING) {
            StaffWaitingScreen(
                onLogout  = {
                    navController.navigate(LOG_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRefresh = {
                    navController.navigate(LOG_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Staff Rejected ────────────────────────────────────────────────────
        composable(STAFF_REJECTED) {
            StaffRejectedScreen(
                onLogout = {
                    navController.navigate(LOG_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Manage Staff ──────────────────────────────────────────────────────
        composable(STAFF) {
            ManageStaffScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Technician Dashboard ──────────────────────────────────────────────
        composable(TECHNICIAN_DASHBOARD) {
            TechnicianDashboardScreen(
                onTicketClick    = { },
                onViewAllTickets = { },
                onViewAllVisits  = { },
                onViewAllDevices = { }
            )
        }

        // ── Finance Dashboard ─────────────────────────────────────────────────
        composable(FINANCE_DASHBOARD) {
            FinanceDashboardScreen(
                onNavigateToPayments  = { navController.navigate(PAYMENTS) },
                onNavigateToInvoices  = { navController.navigate(INVOICES) },
                onNavigateToReports   = { navController.navigate(REPORTS) },
                onNavigateToMore      = { navController.navigate(MORE) },
                onBack                = { navController.popBackStack() },
                onPaymentClick        = { payment ->
                    navController.navigate("payment_detail/${payment.id}")
                },
                onViewAllPayments     = { navController.navigate(PAYMENTS) },
                onViewAllDefaulters   = { navController.navigate(DEFAULTERS) },
                onRecordPayment       = { navController.navigate(RECORD_PAYMENT) },
                onGenerateInvoice     = { navController.navigate(GENERATE_INVOICE) },
                onExportReport        = { navController.navigate(EXPORT_REPORT) },
                onViewDefaulters      = { navController.navigate(DEFAULTERS) }
            )
        }

        // ── Plans ─────────────────────────────────────────────────────────────
        composable(PLANS) {
            PlansScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Institutions ──────────────────────────────────────────────────────
        composable(INSTITUTIONS) {
            InstitutionsScreen(
                onBack      = { navController.popBackStack() },
                onMenuClick = { navController.popBackStack() }
            )
        }

        // ── Subscriptions ─────────────────────────────────────────────────────
        composable(SUBSCRIPTIONS) {
            SubscriptionsScreen(
                onBack      = { navController.popBackStack() },
                onMenuClick = { navController.popBackStack() }
            )
        }

        // ── Record Payment ────────────────────────────────────────────────────
        composable(RECORD_PAYMENT) {
            RecordPaymentScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack()   // goes back to Finance Dashboard
                }
            )
        }

        // ── Payments ──────────────────────────────────────────────────────────
        composable(PAYMENTS) {
            PaymentsScreen(onBack    = { navController.popBackStack() })
        }

        // ── Invoices ──────────────────────────────────────────────────────────
        composable(INVOICES) {
            InvoicesScreen(onBack    = { navController.popBackStack() })
        }

        // ── Reports ───────────────────────────────────────────────────────────
        composable(REPORTS) {
            ReportsScreen(onBack    = { navController.popBackStack() })

        }

        // ── More ──────────────────────────────────────────────────────────────
        composable(MORE) {
            // TODO: MoreScreen — replace Box with MoreScreen() when ready
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("More Screen — Coming Soon") }
        }

        // ── Defaulters ────────────────────────────────────────────────────────

        composable(DEFAULTERS) {
            DefaultersScreen(
                onMenuClick = { navController.popBackStack() }
            )
        }

        // ── Generate Invoice ──────────────────────────────────────────────────
        composable(GENERATE_INVOICE) {
            // TODO: GenerateInvoiceScreen — replace Box when ready
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Generate Invoice — Coming Soon") }
        }

        // ── Export Report ─────────────────────────────────────────────────────
        composable(EXPORT_REPORT) {
            // TODO: ExportReportScreen — replace Box when ready
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Export Report — Coming Soon") }
        }

        // ── Payment Detail ────────────────────────────────────────────────────
        composable("payment_detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            // TODO: PaymentDetailScreen(paymentId = id)
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Payment Detail: $id — Coming Soon") }
        }
    }
}