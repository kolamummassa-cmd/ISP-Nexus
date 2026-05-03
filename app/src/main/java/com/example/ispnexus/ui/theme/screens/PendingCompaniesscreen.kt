package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ispnexus.models.Company
import com.example.ispnexus.viewmodels.*
import com.example.ispnexus.viewmodels.SuperAdminViewModel

private val CorporateBlue = Color(0xFF0D47A1)
private val ApproveGreen  = Color(0xFF2E7D32)
private val RejectRed     = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingCompaniesScreen(
    onBack: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel()
) {
    val pendingState by viewModel.pendingCompanies.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as ActionState.Success).message)
                viewModel.resetActionState()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as ActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CorporateBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                title = { Text("Pending Companies", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                actions = { IconButton(onClick = { viewModel.loadPendingCompanies() }) { Icon(Icons.Default.Refresh, "Refresh") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = pendingState) {
                is CompanyListState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = CorporateBlue)
                is CompanyListState.Empty -> Text("No Pending companies", Modifier.align(Alignment.Center), color = Color.Gray)
                is CompanyListState.Success -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(state.companies, key = { it.adminUid }) { company ->
                            PendingCompanyCard(
                                company = company,
                                isLoading = actionState is ActionState.Loading,
                                onApprove = { viewModel.approveCompany(company.adminUid) },
                                onReject = { viewModel.rejectCompany(company.adminUid) }
                            )
                        }
                    }
                }
                is CompanyListState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun PendingCompanyCard(company: Company, isLoading: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    var showRejectDialog by remember { mutableStateOf(false) }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Company?") },
            text = {
                Column {
                    Text(
                        "You are about to reject \"${company.companyName}\".",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("This will permanently delete the company and their account. They will need to register again.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    onReject()
                }) {
                    Text("Reject", color = RejectRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                    if (company.logoUrl.isNotEmpty()) {
                        AsyncImage(model = company.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Business, null, tint = CorporateBlue)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(company.companyName, fontWeight = FontWeight.SemiBold)
                    Text(company.email, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))
            CompanyDetailRow("Admin", company.adminName)
            CompanyDetailRow("Phone", company.phoneNumber)
            CompanyDetailRow("Reg. No", company.registrationNumber)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showRejectDialog = true }, enabled = !isLoading, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed)) {
                    Text("Reject")
                }
                Button(onClick = onApprove, enabled = !isLoading, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ApproveGreen)) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun CompanyDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(100.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}