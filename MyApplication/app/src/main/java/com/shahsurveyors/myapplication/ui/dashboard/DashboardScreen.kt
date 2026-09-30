package com.shahsurveyors.myapplication.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shahsurveyors.myapplication.R
import com.shahsurveyors.myapplication.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUid: String = "",
    userName: String = "User",
    userEmail: String = "",
    userRole: String = "Staff",
    userAccess: String = "ALL",
    userDepartment: String = "",
    userEmployeeId: String = "",
    userPhotoUrl: String? = null,

    onNavigateToAttendance: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToSurvey: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToDsr: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToSalary: () -> Unit = {},

    onUploadProfilePicture: ((Uri, (Boolean, String?) -> Unit) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,

    isAdmin: Boolean = false,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncRepository = remember { com.shahsurveyors.myapplication.data.DataSyncRepository() }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Google Sheets Sync State
    var isSyncingToSheets by remember { mutableStateOf(false) }
    var syncProgressStatus by remember { mutableStateOf("Ready to sync") }
    var syncProgressPercent by remember { mutableFloatStateOf(0f) }
    var syncResult by remember { mutableStateOf<com.shahsurveyors.myapplication.data.SyncResult?>(null) }
    var showSyncDialog by remember { mutableStateOf(false) }

    fun startGoogleSheetsSync() {
        if (isSyncingToSheets) return
        coroutineScope.launch {
            isSyncingToSheets = true
            showSyncDialog = true
            syncProgressPercent = 0.05f
            syncProgressStatus = "Connecting to Firestore..."
            try {
                val res = syncRepository.syncAllFirestoreDataToGoogleSheets { status, progress ->
                    syncProgressStatus = status
                    syncProgressPercent = progress
                }
                syncResult = res
                viewModel.fetchDashboardData()
            } catch (e: Exception) {
                syncResult = com.shahsurveyors.myapplication.data.SyncResult(
                    isSuccess = false,
                    message = e.localizedMessage ?: "Sync error occurred"
                )
            } finally {
                isSyncingToSheets = false
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(currentUid, isAdmin) {
        viewModel.startListeningToNotifications(currentUid, isAdmin)
        viewModel.fetchDashboardData()
    }

    LaunchedEffect(isSyncing) {
        if (!isSyncing) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.fetchDashboardData()
            onRefresh()
        }
    }

    // Live IST Clock
    LaunchedEffect(Unit) {
        while (true) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+5:30")
            }
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+5:30")
            }
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "SHAH Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "SHAH ERP",
                            style = MaterialTheme.typography.titleLarge,
                            color = ShahWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Prominent Cloud Sync Button for Admin & Fast Sync
                    if (isAdmin) {
                        IconButton(
                            onClick = { startGoogleSheetsSync() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync All Data to Google Sheet",
                                tint = ShahWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showNotificationDialog = true }) {
                        val count = viewModel.notificationList.count { !it.isRead }
                        if (count > 0) {
                            BadgedBox(badge = { Badge { Text(count.toString()) } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ShahWhite)
                            }
                        } else {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ShahWhite)
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Profile Picture Avatar / Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ShahWhite)
                            .border(1.5.dp, ShahLightGreen, CircleShape)
                            .clickable { showProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile DP",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase().ifBlank { "U" },
                                color = ShahDarkGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShahDarkGreen
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(ShahGrey)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Welcome Card
                item {
                    Column {
                        Text(
                            text = "Welcome back, $userRole",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ShahDarkGrey
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShahBlack,
                                modifier = Modifier.weight(1f)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currentTime,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ShahGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShahMediumGrey
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // Summary Section
                item {
                    SummarySection(
                        viewModel = viewModel,
                        isAdmin = isAdmin
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // Admin Broadcast Notice Card
                item {
                    DashboardBroadcastNoticeCard(
                        notice = viewModel.noticeMessage,
                        isAdmin = isAdmin,
                        onNoticeUpdated = { newNotice ->
                            viewModel.updateNotice(newNotice)
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // Quick Actions
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ShahBlack
                    )

                    Spacer(Modifier.height(12.dp))

                    QuickActionsGrid(
                        onAttendance = onNavigateToAttendance,
                        onExpense = onNavigateToExpense,
                        onAdmin = onNavigateToAdmin,
                        onBilling = onNavigateToBilling,
                        onTasks = onNavigateToTasks,
                        onSalary = onNavigateToSalary,
                        onSurvey = onNavigateToSurvey,
                        onDsr = onNavigateToDsr,
                        onClients = onNavigateToClients,
                        onEquipment = onNavigateToEquipment,
                        onSyncSheets = { startGoogleSheetsSync() },
                        userAccess = userAccess,
                        isAdmin = isAdmin
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = ShahWhite,
                contentColor = ShahGreen
            )
        }
    }

    if (showSyncDialog) {
        com.shahsurveyors.myapplication.ui.components.GoogleSheetsSyncDialog(
            isSyncing = isSyncingToSheets,
            statusText = syncProgressStatus,
            progressPercent = syncProgressPercent,
            syncResult = syncResult,
            onDismiss = { showSyncDialog = false }
        )
    }

    if (showNotificationDialog) {
        NotificationCenterDialog(
            notifications = viewModel.notificationList,
            onDismiss = { showNotificationDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            name = userName,
            email = userEmail,
            role = userRole,
            empId = userEmployeeId,
            department = userDepartment,
            access = userAccess,
            photoUrl = userPhotoUrl,
            isAdmin = isAdmin,
            onDismiss = { showProfileDialog = false },
            onUploadPhoto = { uri ->
                onUploadProfilePicture?.invoke(uri) { success, msg ->
                    if (success) {
                        Toast.makeText(context, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onLogout = onLogout
        )
    }
}

@Composable
fun SummarySection(
    viewModel: DashboardViewModel,
    isAdmin: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = if (isAdmin) "Total Staff" else "Team Active",
                value = viewModel.totalEmployees.toString(),
                icon = Icons.Default.People,
                color = ShahGreen,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Present Today",
                value = "${viewModel.presentToday} Present",
                icon = Icons.Default.CheckCircle,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Active Projects",
                value = viewModel.activeProjects.toString(),
                icon = Icons.Default.Work,
                color = ShahDarkGreen,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = if (isAdmin) "Company Expenses" else "My Claims",
                value = viewModel.monthlyExpenses,
                icon = Icons.Default.AccountBalanceWallet,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
        }

        if (isAdmin) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Salary Liability",
                    value = viewModel.salaryLiability,
                    icon = Icons.Default.Payments,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Estimated Margin",
                    value = viewModel.estimatedProfit,
                    icon = Icons.Default.TrendingUp,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShahWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = ShahMediumGrey
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ShahBlack
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onAttendance: () -> Unit,
    onExpense: () -> Unit,
    onAdmin: () -> Unit,
    onBilling: () -> Unit,
    onTasks: () -> Unit,
    onSalary: () -> Unit,
    onSurvey: () -> Unit = {},
    onDsr: () -> Unit = {},
    onClients: () -> Unit = {},
    onEquipment: () -> Unit = {},
    onSyncSheets: () -> Unit = {},
    userAccess: String = "ALL",
    isAdmin: Boolean = false
) {
    val accessUpper = userAccess.uppercase()
    val hasFullAccess = isAdmin || accessUpper.contains("ALL")

    val actions = buildList {
        if (hasFullAccess || accessUpper.contains("ATTENDANCE")) {
            add(QuickAction("Punch IN/OUT", onAttendance))
        }
        if (hasFullAccess || accessUpper.contains("TASK")) {
            add(QuickAction("My Tasks", onTasks))
        }
        if (hasFullAccess || accessUpper.contains("SALARY")) {
            add(QuickAction(if (isAdmin) "Payroll Hub" else "Salary / Slip", onSalary))
        }
        if (hasFullAccess || accessUpper.contains("EXPENSE")) {
            add(QuickAction("Claim Expense", onExpense))
        }
        if (hasFullAccess || accessUpper.contains("SURVEY")) {
            add(QuickAction("Survey Engine", onSurvey))
        }
        if (hasFullAccess || accessUpper.contains("DSR")) {
            add(QuickAction("Daily Status (DSR)", onDsr))
        }
        if (hasFullAccess || accessUpper.contains("CRM")) {
            add(QuickAction("Clients CRM", onClients))
        }
        if (isAdmin) {
            add(QuickAction("Staff & Settings", onAdmin))
            add(QuickAction("Create Invoice", onBilling))
            add(QuickAction("Equipment Tracker", onEquipment))
            add(QuickAction("Sync Google Sheet ☁️", onSyncSheets))
        }
    }

    val rows = (actions.size + 1) / 2
    val gridHeight = when {
        rows == 0 -> 0.dp
        rows == 1 -> 65.dp
        rows == 2 -> 130.dp
        rows == 3 -> 195.dp
        else -> 260.dp
    }

    if (actions.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(actions) { action ->
                Button(
                    onClick = action.action,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShahWhite,
                        contentColor = ShahDarkGreen
                    ),
                    border = BorderStroke(1.dp, ShahGreen.copy(alpha = 0.3f)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = action.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileDialog(
    name: String,
    email: String,
    role: String,
    empId: String,
    department: String,
    access: String,
    photoUrl: String?,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onUploadPhoto: (Uri) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhoto(uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ShahWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Profile & Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShahDarkGreen
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ShahDarkGrey)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile DP with Camera Badge
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(ShahGreen.copy(alpha = 0.1f))
                        .border(2.5.dp, ShahGreen, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = name.take(1).uppercase().ifBlank { "U" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShahDarkGreen
                        )
                    }

                    // Camera Icon Overlay at Bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ShahDarkGreen)
                            .border(1.5.dp, ShahWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = ShahWhite,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { launcher.launch("image/*") }
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = ShahGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Profile Picture", fontSize = 12.sp, color = ShahGreen, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // User Info Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ShahGrey)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileInfoRow(label = "Full Name", value = name)
                        ProfileInfoRow(label = "Employee ID", value = empId.ifBlank { "EMP001" })
                        ProfileInfoRow(label = "Department", value = department.ifBlank { "SURVEY" })
                        ProfileInfoRow(label = "Designation / Role", value = role.uppercase())
                        if (email.isNotBlank()) {
                            ProfileInfoRow(label = "Email", value = email)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Assigned Permissions List
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Assigned Modules & Permissions:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShahDarkGrey
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val permissionList = if (access.contains("ALL", ignoreCase = true) || isAdmin) {
                        listOf("FULL ACCESS (ALL MODULES)")
                    } else {
                        access.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        permissionList.take(4).forEach { perm ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShahLightGreen.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = perm,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShahDarkGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (onLogout != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = ShahMediumGrey)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ShahBlack)
    }
}

private data class QuickAction(
    val title: String,
    val action: () -> Unit
)