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

        composable("login") {
            LoginScreen(
             navController
            )
        }

        composable("admin") {
            Text("Admin Dashboard")
        }
        composable("super_admin") {
            SuperAdminScreen(
                onPendingClick = { navController.navigate("pending_companies") },
                onApprovedClick = { navController.navigate("approved_companies") },
                onAnalyticsClick = { navController.navigate("system_analytics") }
            )
        }
    }
}