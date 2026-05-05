package com.example.ispnexus.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Color
import com.example.ispnexus.ui.theme.screens.AssignedTicket
import com.example.ispnexus.ui.theme.screens.DeviceType
import com.example.ispnexus.ui.theme.screens.FieldVisit
//import com.example.ispnexus.ui.theme.screens.GreenAccent
//import com.example.ispnexus.ui.theme.screens.LightBlue
//import com.example.ispnexus.ui.theme.screens.LightGreen
//import com.example.ispnexus.ui.theme.screens.LightOrange
//import com.example.ispnexus.ui.theme.screens.LightRed
import com.example.ispnexus.ui.theme.screens.NetworkDevice
//import com.example.ispnexus.ui.theme.screens.OrangeAccent
//import com.example.ispnexus.ui.theme.screens.PrimaryBlue
//import com.example.ispnexus.ui.theme.screens.RedAccent
import com.example.ispnexus.ui.theme.screens.TicketPriority
import com.example.ispnexus.ui.theme.screens.TicketStatus
import com.example.ispnexus.ui.theme.screens.TicketSummary

// ─── UI State ─────────────────────────────────────────────────────────────────
data class TechnicianDashboardUiState(
    val isLoading: Boolean = true,
    val technicianName: String = "",
    val notificationCount: Int = 0,
    val ticketStats: List<TicketSummary> = emptyList(),
    val assignedTickets: List<AssignedTicket> = emptyList(),
    val fieldVisits: List<FieldVisit> = emptyList(),
    val networkDevices: List<NetworkDevice> = emptyList(),
    val error: String? = null,
    val companyName: String
)

// ─── ViewModel ────────────────────────────────────────────────────────────────
class TechnicianDashboardViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(TechnicianDashboardUiState(companyName = ""))
    val uiState: StateFlow<TechnicianDashboardUiState> = _uiState.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Fetch technician profile
                val userDoc = firestore.collection("users").document(uid).get().await()
                val companyId = userDoc.getString("companyId") ?: return@launch
                val fullName = userDoc.getString("fullName") ?: "Technician"
                val companyDoc  = firestore.collection("companies").document(companyId).get().await()
                val companyName = companyDoc.getString("companyName") ?: ""

                _uiState.value = _uiState.value.copy(
                    technicianName = fullName,
                    companyName = companyName,
                    isLoading = false
                )

                // Listen to real-time ticket updates
                listenToTickets(uid, companyId)

                // Listen to field visits for today
                listenToFieldVisits(uid, companyId)

                // Listen to assigned network devices
                listenToNetworkDevices(uid, companyId)

                // Listen to notifications count
                listenToNotifications(uid, companyId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // ─── Tickets Listener ─────────────────────────────────────────────────────
    private fun listenToTickets(technicianId: String, companyId: String) {
        val listener = firestore
            .collection("companies")
            .document(companyId)
            .collection("tickets")
            .whereEqualTo("assignedTechnicianId", technicianId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val allTickets = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("ticketNumber") ?: return@mapNotNull null
                    val title = doc.getString("issueTitle") ?: return@mapNotNull null
                    val customer = doc.getString("customerName") ?: ""
                    val priorityStr = doc.getString("priority") ?: "MEDIUM"
                    val statusStr = doc.getString("status") ?: "OPEN"

                    val priority = when (priorityStr.uppercase()) {
                        "HIGH" -> TicketPriority.HIGH
                        "LOW" -> TicketPriority.LOW
                        else -> TicketPriority.MEDIUM
                    }
                    val status = when (statusStr.uppercase()) {
                        "IN_PROGRESS", "INPROGRESS" -> TicketStatus.IN_PROGRESS
                        "RESOLVED" -> TicketStatus.RESOLVED
                        else -> TicketStatus.OPEN
                    }

                    AssignedTicket(id, title, customer, priority, status)
                }

                // Build stats
                val openCount = allTickets.count { it.status == TicketStatus.OPEN }
                val inProgressCount = allTickets.count { it.status == TicketStatus.IN_PROGRESS }
                val resolvedCount = allTickets.count { it.status == TicketStatus.RESOLVED }
                val overdueCount = snapshot.documents.count { doc ->
                    doc.getString("status") == "OVERDUE"
                }

                val stats = listOf(
                    TicketSummary(
                        label         = "Open Tickets",
                        count         = openCount,
                        delta         = "+2 from yesterday",
                        deltaPositive = false,
                        bgColor       = Color(0xFFEAF2FF),
                        textColor     = Color(0xFF1A73E8),
                        iconColor     = Color(0xFF1A73E8)
                    ),
                    TicketSummary(
                        label         = "In-Progress",
                        count         = inProgressCount,
                        delta         = "-1 from yesterday",
                        deltaPositive = true,
                        bgColor       = Color(0xFFFFF8EE),
                        textColor     = Color(0xFFF5A623),
                        iconColor     = Color(0xFFF5A623)
                    ),
                    TicketSummary(
                        label         = "Resolved Today",
                        count         = resolvedCount,
                        delta         = "+5 from yesterday",
                        deltaPositive = true,
                        bgColor       = Color(0xFFEEF9F1),
                        textColor     = Color(0xFF34A853),
                        iconColor     = Color(0xFF34A853)
                    ),
                    TicketSummary(
                        label         = "Overdue",
                        count         = overdueCount,
                        delta         = "Needs Attention",
                        deltaPositive = false,
                        bgColor       = Color(0xFFFFF0EF),
                        textColor     = Color(0xFFEA4335),
                        iconColor     = Color(0xFFEA4335)
                    )
                )

                _uiState.value = _uiState.value.copy(
                    ticketStats = stats,
                    // Show only top 5 for dashboard
                    assignedTickets = allTickets
                        .filter { it.status != TicketStatus.RESOLVED }
                        .take(5)
                )
            }
        listeners.add(listener)
    }

    // ─── Field Visits Listener ────────────────────────────────────────────────
    private fun listenToFieldVisits(technicianId: String, companyId: String) {
        val todayStart = com.google.firebase.Timestamp(
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
            }.time
        )

        val listener = firestore
            .collection("companies")
            .document(companyId)
            .collection("fieldVisits")
            .whereEqualTo("technicianId", technicianId)
            .whereGreaterThanOrEqualTo("scheduledAt", todayStart)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val visits = snapshot.documents.mapNotNull { doc ->
                    val location = doc.getString("customerName") ?: return@mapNotNull null
                    val task = doc.getString("taskDescription") ?: ""
                    val statusStr = doc.getString("visitStatus") ?: "Upcoming"
                    val timestamp = doc.getTimestamp("scheduledAt") ?: return@mapNotNull null

                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    val time = sdf.format(timestamp.toDate())

                    FieldVisit(time, location, task, statusStr)
                }

                _uiState.value = _uiState.value.copy(
                    fieldVisits = visits.take(3)
                )
            }
        listeners.add(listener)
    }

    // ─── Network Devices Listener ─────────────────────────────────────────────
    private fun listenToNetworkDevices(technicianId: String, companyId: String) {
        val listener = firestore
            .collection("companies")
            .document(companyId)
            .collection("networkDevices")
            .whereEqualTo("assignedTechnicianId", technicianId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val devices = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("deviceName") ?: return@mapNotNull null
                    val location = doc.getString("customerName") ?: ""
                    val isOnline = doc.getBoolean("isOnline") ?: false
                    val uptime = doc.getString("uptimeDisplay") ?: ""
                    val typeStr = doc.getString("deviceType") ?: "ROUTER"

                    val deviceType = when (typeStr.uppercase()) {
                        "SWITCH" -> DeviceType.SWITCH
                        "ACCESS_POINT" -> DeviceType.ACCESS_POINT
                        else -> DeviceType.ROUTER
                    }

                    NetworkDevice(
                        name = "$name - $location",
                        location = location,
                        isOnline = isOnline,
                        uptimeOrLastSeen = uptime,
                        iconType = deviceType
                    )
                }

                _uiState.value = _uiState.value.copy(
                    networkDevices = devices.take(3)
                )
            }
        listeners.add(listener)
    }

    // ─── Notifications Listener ───────────────────────────────────────────────
    private fun listenToNotifications(technicianId: String, companyId: String) {
        val listener = firestore
            .collection("companies")
            .document(companyId)
            .collection("notifications")
            .whereEqualTo("recipientId", technicianId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _uiState.value = _uiState.value.copy(
                    notificationCount = snapshot.size()
                )
            }
        listeners.add(listener)
    }

    // ─── Mark notification as read ────────────────────────────────────────────
    fun markNotificationsRead(companyId: String, notificationIds: List<String>) {
        viewModelScope.launch {
            val batch = firestore.batch()
            notificationIds.forEach { id ->
                val ref = firestore
                    .collection("companies")
                    .document(companyId)
                    .collection("notifications")
                    .document(id)
                batch.update(ref, "isRead", true)
            }
            batch.commit().await()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}