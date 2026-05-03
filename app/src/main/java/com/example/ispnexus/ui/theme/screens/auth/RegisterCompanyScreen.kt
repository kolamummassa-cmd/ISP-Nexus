package com.example.ispnexus.ui.theme.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.ispnexus.R
import com.example.ispnexus.viewmodels.RegisterState
import com.example.ispnexus.viewmodels.RegisterViewModel

private val CorporateBlue = Color(0xFF0D47A1)
private val SoftBlue = Color(0xFFE3F2FD)
private val LightBackground = Color(0xFFF4F6F8)
private val DividerGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCompanyScreen(
    onBackToLogin: () -> Unit,
    onRegistrationSuccess: (String) -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var adminName          by remember { mutableStateOf("") }
    var email              by remember { mutableStateOf("") }
    var password           by remember { mutableStateOf("") }
    var passwordVisible    by remember { mutableStateOf(false) }
    var companyName        by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var taxPin             by remember { mutableStateOf("") }
    var phoneNumber        by remember { mutableStateOf("") }
    var logoUri            by remember { mutableStateOf<Uri?>(null) }

    val registerState by viewModel.registerState.collectAsState()
    val isLoading = registerState is RegisterState.Loading

    // Image picker launcher
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        logoUri = uri
    }

    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            onRegistrationSuccess(companyName)
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CorporateBlue.copy(alpha = 0.10f),
                        LightBackground
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CorporateBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Register ISP Company",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Fill in your company details below",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.80f)
                            )
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
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Company Logo Upload ───────────────────────────────────
                Text(
                    text = "Company Logo",
                    style = MaterialTheme.typography.labelMedium,
                    color = CorporateBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(SoftBlue)
                        .border(2.dp, CorporateBlue.copy(alpha = 0.4f), CircleShape)
                        .clickable { logoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(logoUri),
                            contentDescription = "Company Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = CorporateBlue,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload Logo",
                                fontSize = 11.sp,
                                color = CorporateBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (logoUri != null) {
                    TextButton(onClick = { logoUri = null }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ── Admin Details Section ─────────────────────────────────
                FormSectionCard(title = "Admin Details") {

                    // Row 1: Admin Name | Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GridField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = "Admin Full Name",
                            modifier = Modifier.weight(1f)
                        )
                        GridField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Company Email",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Password | Phone Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontSize = 12.sp) },
                            singleLine = true,
                            isError = registerState is RegisterState.Error,
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = CorporateBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )

                        GridField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = "Phone Number",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Company Details Section ───────────────────────────────
                FormSectionCard(title = "Company Details") {

                    // Row 1: Company Name | Registration Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GridField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = "Company Name",
                            modifier = Modifier.weight(1f)
                        )
                        GridField(
                            value = registrationNumber,
                            onValueChange = { registrationNumber = it },
                            label = "Reg. Number",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Tax PIN (full width — can add more fields later)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GridField(
                            value = taxPin,
                            onValueChange = { taxPin = it },
                            label = "Tax PIN",
                            modifier = Modifier.weight(1f)
                        )
                        // Placeholder for future field
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Error message ─────────────────────────────────────────
                if (registerState is RegisterState.Error) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (registerState as RegisterState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Submit Button ─────────────────────────────────────────
                Button(
                    onClick = {
                        viewModel.registerCompany(
                            adminName          = adminName,
                            email              = email,
                            password           = password,
                            companyName        = companyName,
                            registrationNumber = registrationNumber,
                            taxPin             = taxPin,
                            phoneNumber        = phoneNumber,
                            logoUri            = logoUri?.toString()
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Submit Registration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onBackToLogin) {
                    Text(
                        text = "Already registered? Back to Login",
                        color = CorporateBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ── Form Section Card ─────────────────────────────────────────────────────────

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CorporateBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = SoftBlue, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

// ── Grid Field ────────────────────────────────────────────────────────────────

@Composable
private fun GridField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 12.sp) },
        singleLine    = true,
        modifier      = modifier,
        shape         = RoundedCornerShape(12.dp),
        textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}