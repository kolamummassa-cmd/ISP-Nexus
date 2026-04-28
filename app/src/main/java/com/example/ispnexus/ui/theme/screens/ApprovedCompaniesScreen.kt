package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ispnexus.models.Company
import com.example.ispnexus.viewmodels.CompanyListState
import com.example.ispnexus.viewmodels.SuperAdminViewModel

private val CorporateBlue = Color(0xFF0D47A1)
private val ApproveGreen  = Color(0xFF2E7D32)
private val PageBg        = Color(0xFFF0F2F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovedCompaniesScreen(
    onBack: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel()
) {
    val approvedState by viewModel.approvedCompanies.collectAsState()

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = CorporateBlue,
                    titleContentColor      = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text       = "Approved Companies",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp
                        )
                        Text(
                            text  = "All verified ISP providers",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadApprovedCompanies() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = approvedState) {

                // ── Loading ───────────────────────────────────────────────
                is CompanyListState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = CorporateBlue
                    )
                }

                // ── Empty ─────────────────────────────────────────────────
                is CompanyListState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Business,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text  = "No approved companies yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Text(
                            text      = "Approved ISPs will appear here",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── Error ─────────────────────────────────────────────────
                is CompanyListState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text      = state.message,
                            color     = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadApprovedCompanies() },
                            colors  = ButtonDefaults.buttonColors(containerColor = CorporateBlue)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                // ── Success ───────────────────────────────────────────────
                is CompanyListState.Success -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text  = "${state.companies.size} active ISP(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                        }

                        items(
                            items = state.companies,
                            key   = { it.adminUid }
                        ) { company ->
                            ApprovedCompanyCard(company = company)
                        }
                    }
                }
            }
        }
    }
}

// ── Approved Company Card ─────────────────────────────────────────────────────

@Composable
fun ApprovedCompanyCard(company: Company) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = Color.White,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: logo + name + badge ───────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Logo or fallback
                Box(
                    modifier         = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (company.logoUrl.isNotEmpty()) {
                        AsyncImage(
                            model              = company.logoUrl,
                            contentDescription = "Company Logo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Business,
                            contentDescription = null,
                            tint               = ApproveGreen,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = company.companyName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = Color(0xFF1A1A1A)
                    )
                    Text(
                        text     = company.adminName,
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }

                // Approved badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFC8E6C9)
                ) {
                    Text(
                        text       = "✓ Active",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ApproveGreen,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // ── Contact details ───────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Email,
                        contentDescription = null,
                        tint               = CorporateBlue,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = company.email,
                        fontSize = 11.sp,
                        color    = Color(0xFF4B5563),
                        maxLines = 1
                    )
                }

                // Phone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.Phone,
                        contentDescription = null,
                        tint               = CorporateBlue,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = company.phoneNumber,
                        fontSize = 11.sp,
                        color    = Color(0xFF4B5563)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Registration details ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailChip(label = "Reg No.", value = company.registrationNumber)
                DetailChip(label = "Tax PIN", value = company.taxPin)
            }
        }
    }
}

// ── Detail Chip ───────────────────────────────────────────────────────────────

@Composable
private fun DetailChip(label: String, value: String) {
    if (value.isBlank()) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF3F4F6)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text     = label,
                fontSize = 10.sp,
                color    = Color.Gray
            )
            Text(
                text       = value,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF1A1A1A)
            )
        }
    }
}
