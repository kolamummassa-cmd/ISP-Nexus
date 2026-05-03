package com.example.ispnexus.ui.theme.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ispnexus.R
import com.example.ispnexus.viewmodels.AuthState
import com.example.ispnexus.viewmodels.AuthViewModel

private val CorporateBlue = Color(0xFF185FA5)
private val LightBackground = Color(0xFFF4F8FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSuperAdmin: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToUser: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe login state from ViewModel
    val loginState by viewModel.loginState.collectAsState()

    val isLoading = loginState is AuthState.Loading
    val errorMessage = (loginState as? AuthState.Error)?.message

    // React to successful login — navigate based on role
    LaunchedEffect(loginState) {
        if (loginState is AuthState.Success) {
            val role = (loginState as AuthState.Success).role
            when (role.lowercase().trim()) {
                "super_admin" -> onNavigateToSuperAdmin()
                "admin"       -> onNavigateToAdmin()
                else          -> onNavigateToUser()
            }
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CorporateBlue.copy(alpha = 0.15f),
                        LightBackground
                    )
                )
            )
    ) {

        // Soft blur background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
                .background(CorporateBlue.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {

                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.isp_nexus),
                        contentDescription = "ISP Nexus Logo",
                        modifier = Modifier.size(160.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Multi-tenant ISP Management Platform",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (loginState is AuthState.Error) viewModel.resetState()
                        },
                        label = { Text("Email Address") },
                        singleLine = true,
                        isError = loginState is AuthState.Error,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (loginState is AuthState.Error) viewModel.resetState()
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = loginState is AuthState.Error,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Error message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Login button
                    Button(
                        onClick = { viewModel.login(email, password) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CorporateBlue
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Register link
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = "Don't Have an Account? Register ",
                            color = CorporateBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}