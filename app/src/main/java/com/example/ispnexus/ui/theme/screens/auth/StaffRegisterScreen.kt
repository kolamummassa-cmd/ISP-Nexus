package com.example.ispnexus.ui.theme.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ispnexus.viewmodels.StaffAuthViewModel
import com.example.ispnexus.viewmodels.StaffRegisterState

private val NavyBlue        = Color(0xFF0D47A1)
private val LightBackground = Color(0xFFF4F6F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: StaffAuthViewModel = viewModel()
) {
    var fullName       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var companyCode    by remember { mutableStateOf("") }
    var position       by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()
    val isLoading      = registerState is StaffRegisterState.Loading
    val errorMessage   = (registerState as? StaffRegisterState.Error)?.message

    LaunchedEffect(registerState) {
        if (registerState is StaffRegisterState.Success) {
            onRegistrationSuccess()
            viewModel.resetRegisterState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyBlue.copy(alpha = 0.10f), LightBackground)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor             = NavyBlue,
                        titleContentColor          = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Text("Staff Registration", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Join your company on ISP Nexus", fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.80f))
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Logo
                Image(
                    painter            = painterResource(id = R.drawable.isp_nexus),
                    contentDescription = "ISP Nexus Logo",
                    modifier           = Modifier.size(80.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text      = "Enter your company code to join your team",
                    fontSize  = 13.sp,
                    color     = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // ── Personal Details ──────────────────────────────────────
                SectionCard(title = "Personal Details") {
                    RegField(fullName, { fullName = it }, "Full Name")
                    Spacer(Modifier.height(10.dp))
                    RegField(email, { email = it }, "Email Address")
                    Spacer(Modifier.height(10.dp))

                    // Password with toggle
                    OutlinedTextField(
                        value               = password,
                        onValueChange       = { password = it },
                        label               = { Text("Password") },
                        singleLine          = true,
                        isError             = registerState is StaffRegisterState.Error,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = NavyBlue
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Company & Position ────────────────────────────────────
                SectionCard(title = "Company Details") {

                    // Company code with info
                    OutlinedTextField(
                        value         = companyCode,
                        onValueChange = { companyCode = it.uppercase() },
                        label         = { Text("Company Join Code") },
                        singleLine    = true,
                        isError       = registerState is StaffRegisterState.Error,
                        supportingText = {
                            Text("Ask your company admin for this code",
                                fontSize = 11.sp, color = Color.Gray)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("Select Your Position", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    val positions = listOf("Technician", "Finance")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        positions.forEach { p ->
                            FilterChip(
                                selected = position == p,
                                onClick  = { position = p },
                                label    = { Text(p) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyBlue,
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Error message
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = errorMessage,
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Submit button
                Button(
                    onClick  = {
                        viewModel.registerStaff(fullName, email, password, companyCode, position)
                    },
                    enabled  = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp))
                    } else {
                        Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                TextButton(onClick = onBackToLogin) {
                    Text("Already registered? Login", color = NavyBlue, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavyBlue)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RegField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp)
    )
}
