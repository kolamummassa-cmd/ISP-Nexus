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

// ── Colors ────────────────────────────────────────────────────────────────────

private val NavyBlue     = Color(0xFF0D47A1)
private val PageBg       = Color(0xFFF4F6FA)
private val ActiveGreen  = Color(0xFF2E7D32)
private val RejectRed    = Color(0xFFC62828)
private val PendingAmber = Color(0xFFB7791F)

// ── Staff Model ───────────────────────────────────────────────────────────────

data class StaffMember(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val position: String = "",
    val status: String = "",
    val createdAt: Long = 0L
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class ManageStaffState {
    object Loading : ManageStaffState()
    object Empty : ManageStaffState()
    data class Success(val staffList: List<StaffMember>) : ManageStaffState()
    data class Error(val message: String) : ManageStaffState()
}

sealed class ManageStaffAction {
    object Idle : ManageStaffAction()
    object Loading : ManageStaffAction()
    data class Success(val message: String) : ManageStaffAction()
    data class Error(val message: String) : ManageStaffAction()
}

class ManageStaffViewModel : ViewModel() {

    private val db        = FirebaseFirestore.getInstance()
    private val companyId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _state  = MutableStateFlow<ManageStaffState>(ManageStaffState.Loading)
    val state: StateFlow<ManageStaffState> = _state.asStateFlow()

    private val _action = MutableStateFlow<ManageStaffAction>(ManageStaffAction.Idle)
    val action: StateFlow<ManageStaffAction> = _action.asStateFlow()

    private val _selectedTab = MutableStateFlow("Pending")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    init { observeStaff() }

    // ── Real-time listener ────────────────────────────────────────────────────
    private fun observeStaff() {
        viewModelScope.launch {
            callbackFlow {
                val listener = db.collection("users")
                    .whereEqualTo("role", "staff")
                    .whereEqualTo("companyId", companyId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { close(error); return@addSnapshotListener }
                        val list = snapshot?.documents?.mapNotNull { doc ->
                            StaffMember(
                                id        = doc.id,
                                fullName  = doc.getString("fullName") ?: "",
                                email     = doc.getString("email") ?: "",
                                position  = doc.getString("position") ?: "",
                                status    = doc.getString("status") ?: "",
                                createdAt = (doc.get("createdAt") as? Long) ?: 0L
                            )
                        } ?: emptyList()
                        trySend(list)
                    }
                awaitClose { listener.remove() }
            }.collect { list ->
                _state.value = if (list.isEmpty()) ManageStaffState.Empty
                else ManageStaffState.Success(list)
            }
        }
    }

    fun setTab(tab: String) { _selectedTab.value = tab }

    // ── Approve ───────────────────────────────────────────────────────────────
    fun approveStaff(staffId: String) {
        viewModelScope.launch {
            _action.value = ManageStaffAction.Loading
            try {
                db.collection("users").document(staffId)
                    .update("status", "active").await()
                _action.value = ManageStaffAction.Success("Staff member approved successfully")
            } catch (e: Exception) {
                _action.value = ManageStaffAction.Error(e.message ?: "Failed to approve")
            }
        }
    }

    // ── Reject ────────────────────────────────────────────────────────────────
    fun rejectStaff(staffId: String) {
        viewModelScope.launch {
            _action.value = ManageStaffAction.Loading
            try {
                db.collection("users").document(staffId)
                    .update("status", "rejected").await()
                _action.value = ManageStaffAction.Success("Staff member rejected")
            } catch (e: Exception) {
                _action.value = ManageStaffAction.Error(e.message ?: "Failed to reject")
            }
        }
    }

    fun resetAction() { _action.value = ManageStaffAction.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffScreen(
    onBack: () -> Unit,
    viewModel: ManageStaffViewModel = viewModel()
) {
    val state       by viewModel.state.collectAsState()
    val action      by viewModel.action.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val snackbar     = remember { SnackbarHostState() }

    LaunchedEffect(action) {
        when (action) {
            is ManageStaffAction.Success -> {
                snackbar.showSnackbar((action as ManageStaffAction.Success).message)
                viewModel.resetAction()
            }
            is ManageStaffAction.Error -> {
                snackbar.showSnackbar((action as ManageStaffAction.Error).message)
                viewModel.resetAction()
            }
            else -> Unit
        }
    }

    // Filter staff by selected tab
    val allStaff = (state as? ManageStaffState.Success)?.staffList ?: emptyList()
    val pending  = allStaff.filter { it.status == "pending" }
    val active   = allStaff.filter { it.status == "active" }
    val rejected = allStaff.filter { it.status == "rejected" }

    val displayList = when (selectedTab) {
        "Pending"  -> pending
        "Active"   -> active
        "Rejected" -> rejected
        else       -> allStaff
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NavyBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Manage Staff", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Review and manage your team", fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f))
                    }
                }
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Summary stat strip ────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(NavyBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StaffStatChip("Total",    allStaff.size.toString(), Color.White,
                    Color.White.copy(alpha = 0.2f), Modifier.weight(1f))
                StaffStatChip("Pending",  pending.size.toString(),  Color(0xFFFFE0B2),
                    Color(0xFFB7791F).copy(alpha = 0.3f), Modifier.weight(1f))
                StaffStatChip("Active",   active.size.toString(),   Color(0xFFC8E6C9),
                    Color(0xFF2E7D32).copy(alpha = 0.3f), Modifier.weight(1f))
                StaffStatChip("Rejected", rejected.size.toString(), Color(0xFFFFCDD2),
                    Color(0xFFC62828).copy(alpha = 0.3f), Modifier.weight(1f))
            }

            // ── Tabs ──────────────────────────────────────────────────────
            val tabs = listOf("Pending", "Active", "Rejected", "All")
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor   = Color.White,
                contentColor     = NavyBlue
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick  = { viewModel.setTab(tab) },
                        text     = {
                            Text(tab, fontWeight = if (selectedTab == tab)
                                FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state is ManageStaffState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color    = NavyBlue
                        )
                    }
                    displayList.isEmpty() -> {
                        Column(
                            modifier            = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null,
                                modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text("No $selectedTab staff", fontSize = 15.sp,
                                color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text("${displayList.size} $selectedTab staff member(s)",
                                    fontSize = 12.sp, color = Color.Gray)
                            }
                            items(displayList, key = { it.id }) { staff ->
                                StaffMemberCard(
                                    staff     = staff,
                                    isLoading = action is ManageStaffAction.Loading,
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
}

// ── Staff Stat Chip ───────────────────────────────────────────────────────────

@Composable
private fun StaffStatChip(
    label: String,
    value: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(10.dp), color = bgColor, modifier = modifier) {
        Column(
            modifier            = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(label, fontSize = 10.sp, color = textColor.copy(alpha = 0.8f))
        }
    }
}

// ── Staff Member Card ─────────────────────────────────────────────────────────

@Composable
private fun StaffMemberCard(
    staff: StaffMember,
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
                        color = RejectRed, fontSize = 13.sp)
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

            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Avatar
                Box(
                    modifier         = Modifier.size(48.dp).clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = staff.fullName.take(2).uppercase(),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NavyBlue
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.fullName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(staff.position, fontSize = 12.sp, color = Color.Gray)
                    Text(staff.email, fontSize = 11.sp, color = Color.Gray)
                }

                // Status badge
                val (badgeBg, badgeColor, badgeText) = when (staff.status) {
                    "active"   -> Triple(Color(0xFFC8E6C9), ActiveGreen,  "● Active")
                    "pending"  -> Triple(Color(0xFFFFE0B2), PendingAmber, "◐ Pending")
                    "rejected" -> Triple(Color(0xFFFFCDD2), RejectRed,    "✕ Rejected")
                    else       -> Triple(Color(0xFFF3F4F6), Color.Gray,   staff.status)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
                    Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            // ── Action buttons — only show for pending staff ───────────────
            if (staff.status == "pending") {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showRejectDialog = true },
                        enabled  = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick  = onApprove,
                        enabled  = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White,
                                strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
