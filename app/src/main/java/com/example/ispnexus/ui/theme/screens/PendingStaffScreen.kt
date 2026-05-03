package com.example.ispnexus.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Staff Model ───────────────────────────────────────────────────────────────

data class PendingStaff(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val position: String = "",
    val companyId: String = "",
    val createdAt: Long = 0L
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class PendingStaffState {
    object Loading : PendingStaffState()
    object Empty : PendingStaffState()
    data class Success(val staffList: List<PendingStaff>) : PendingStaffState()
    data class Error(val message: String) : PendingStaffState()
}

sealed class StaffApprovalState {
    object Idle : StaffApprovalState()
    object Loading : StaffApprovalState()
    data class Success(val message: String) : StaffApprovalState()
    data class Error(val message: String) : StaffApprovalState()
}

class PendingStaffViewModel : ViewModel() {

    private val db        = FirebaseFirestore.getInstance()
    private val companyId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _staffState    = MutableStateFlow<PendingStaffState>(PendingStaffState.Loading)
    val staffState: StateFlow<PendingStaffState> = _staffState.asStateFlow()

    private val _approvalState = MutableStateFlow<StaffApprovalState>(StaffApprovalState.Idle)
    val approvalState: StateFlow<StaffApprovalState> = _approvalState.asStateFlow()

    init { observePendingStaff() }

    // ── Real-time listener for pending staff ──────────────────────────────────
    private fun observePendingStaff() {
        viewModelScope.launch {
            callbackFlow {
                val listener = db.collection("users")
                    .whereEqualTo("role", "staff")
                    .whereEqualTo("companyId", companyId)
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { close(error); return@addSnapshotListener }
                        val list = snapshot?.documents?.mapNotNull { doc ->
                            PendingStaff(
                                id        = doc.id,
                                fullName  = doc.getString("fullName") ?: "",
                                email     = doc.getString("email") ?: "",
                                position  = doc.getString("position") ?: "",
                                companyId = doc.getString("companyId") ?: "",
                                createdAt = (doc.get("createdAt") as? Long) ?: 0L
                            )
                        } ?: emptyList()
                        trySend(list)
                    }
                awaitClose { listener.remove() }
            }.collect { list ->
                _staffState.value = if (list.isEmpty()) PendingStaffState.Empty
                else PendingStaffState.Success(list)
            }
        }
    }

    // ── Approve staff ─────────────────────────────────────────────────────────
    fun approveStaff(staffId: String) {
        viewModelScope.launch {
            _approvalState.value = StaffApprovalState.Loading
            try {
                db.collection("users").document(staffId)
                    .update("status", "active").await()
                _approvalState.value = StaffApprovalState.Success("Staff member approved")
            } catch (e: Exception) {
                _approvalState.value = StaffApprovalState.Error(e.message ?: "Failed to approve")
            }
        }
    }

    // ── Reject staff — updates status to rejected ─────────────────────────────
    fun rejectStaff(staffId: String) {
        viewModelScope.launch {
            _approvalState.value = StaffApprovalState.Loading
            try {
                db.collection("users").document(staffId)
                    .update("status", "rejected").await()
                _approvalState.value = StaffApprovalState.Success("Staff member rejected")
            } catch (e: Exception) {
                _approvalState.value = StaffApprovalState.Error(e.message ?: "Failed to reject")
            }
        }
    }

    fun resetApprovalState() { _approvalState.value = StaffApprovalState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val NavyBlue    = Color(0xFF0D47A1)
private val ApproveGreen = Color(0xFF2E7D32)
private val RejectRed   = Color(0xFFC62828)
private val PageBg      = Color(0xFFF4F6FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingStaffScreen(
    onBack: () -> Unit,
    viewModel: PendingStaffViewModel = viewModel()
) {
    val staffState    by viewModel.staffState.collectAsState()
    val approvalState by viewModel.approvalState.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }

    LaunchedEffect(approvalState) {
        when (approvalState) {
            is StaffApprovalState.Success -> {
                snackbarState.showSnackbar((approvalState as StaffApprovalState.Success).message)
                viewModel.resetApprovalState()
            }
            is StaffApprovalState.Error -> {
                snackbarState.showSnackbar((approvalState as StaffApprovalState.Error).message)
                viewModel.resetApprovalState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost   = { SnackbarHost(snackbarState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NavyBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor     = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Pending Staff", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Approve or reject staff registrations", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                },
                actions = {
                    IconButton(onClick = { /* real-time — no manual refresh needed */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = staffState) {

                is PendingStaffState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center),
                        color = NavyBlue)
                }

                is PendingStaffState.Empty -> {
                    Column(modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text("No pending staff", fontSize = 16.sp, color = Color.Gray)
                        Text("All registrations have been reviewed",
                            fontSize = 13.sp, color = Color.LightGray)
                    }
                }

                is PendingStaffState.Error -> {
                    Text(state.message, color = RejectRed, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }

                is PendingStaffState.Success -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("${state.staffList.size} pending registration(s)",
                                fontSize = 12.sp, color = Color.Gray)
                        }
                        items(state.staffList, key = { it.id }) { staff ->
                            PendingStaffCard(
                                staff     = staff,
                                isLoading = approvalState is StaffApprovalState.Loading,
                                onApprove = { viewModel.approveStaff(staff.id) },
                                onReject  = { viewModel.rejectStaff(staff.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pending Staff Card ────────────────────────────────────────────────────────

@Composable
fun PendingStaffCard(
    staff: PendingStaff,
    isLoading: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Staff Member?") },
            text  = {
                Column {
                    Text("You are about to reject ${staff.fullName}.",
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Their account will be marked as rejected and they will not be able to access the system.",
                        color = RejectRed)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRejectDialog = false; onReject() }) {
                    Text("Reject", color = RejectRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(50.dp).clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(staff.fullName.take(2).uppercase(), fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = NavyBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.fullName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(staff.position, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFF4E5)) {
                    Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = Color(0xFFB7791F),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(10.dp))

            Text("Email: ${staff.email}", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(14.dp))

            // Approve / Reject buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = { showRejectDialog = true },
                    enabled  = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = onApprove,
                    enabled  = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ApproveGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}