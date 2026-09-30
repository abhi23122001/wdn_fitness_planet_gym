package com.shahsurveyors.myapplication.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shahsurveyors.myapplication.ui.components.GlobalAsyncLoader
import com.shahsurveyors.myapplication.ui.theme.*

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AdminHubScreen(
    viewModel: AdminViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToCompanySettings: () -> Unit = {},
    onNavigateToBankDetails: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {}
) {
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchAdminData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Hub",
                        fontWeight = FontWeight.Bold,
                        color = ShahWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ShahWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncAllDataToGoogleSheets() }) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync All Data to Google Sheet",
                            tint = ShahWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShahDarkGreen
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showCreateUserDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text("Add Employee")
                    },
                    containerColor = ShahGreen,
                    contentColor = ShahWhite
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(ShahGrey)
        ) {

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = ShahWhite,
                contentColor = ShahGreen,
                edgePadding = 16.dp,
                indicator = { tabPositions ->

                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[selectedTab]
                            ),
                            color = ShahGreen
                        )
                    }
                }
            ) {

                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            "Logins",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            "Expenses",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    text = {
                        Text(
                            "Attendance",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                    },
                    text = {
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            when (selectedTab) {

                0 -> PendingLoginsList(viewModel)

                1 -> PendingExpensesList(viewModel)

                2 -> AdminAttendanceList(viewModel)

                3 -> AdminSettingsList(
                    viewModel = viewModel,
                    onNavigateToCompanySettings = onNavigateToCompanySettings,
                    onNavigateToBankDetails = onNavigateToBankDetails,
                    onNavigateToTerms = onNavigateToTerms
                )
            }
        }
    }

    if (showCreateUserDialog) {

        CreateUserDialog(
            onDismiss = {
                showCreateUserDialog = false
            },
            onCreate = { name, phone, pass, dept, role, access ->

                viewModel.createUser(
                    name,
                    phone,
                    pass,
                    dept,
                    role,
                    access
                )

                showCreateUserDialog = false
            }
        )
    }

    if (viewModel.showSyncDialog) {
        com.shahsurveyors.myapplication.ui.components.GoogleSheetsSyncDialog(
            isSyncing = viewModel.isSyncingToSheets,
            statusText = viewModel.syncProgressStatus,
            progressPercent = viewModel.syncProgressPercent,
            syncResult = viewModel.lastSyncResult,
            onDismiss = { viewModel.dismissSyncDialog() }
        )
    }

    GlobalAsyncLoader(
        isLoading = viewModel.isLoading
    )
}


/* ============================================================
   PENDING LOGIN REQUESTS
   ============================================================ */

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun PendingLoginsList(
    viewModel: AdminViewModel
) {

    if (
        viewModel.pendingUsers.isEmpty() &&
        !viewModel.isLoading
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pending login requests",
                color = ShahMediumGrey
            )
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {

        items(
            items = viewModel.pendingUsers,
            key = { it.phone }
        ) { user ->

            Card(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ShahWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = user.name,
                                color = ShahBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                text = user.phone,
                                color = ShahMediumGrey,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "Dept: ${user.dept}",
                                color = ShahGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                // Reject user can be added in AdminViewModel
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reject",
                                tint = ErrorRed
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.approveUser(
                                    user.phone,
                                    user.access
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approve",
                                tint = SuccessGreen
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Module Access Permissions",
                        color = ShahMediumGrey,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val modules = listOf(
                        "ATTENDANCE",
                        "TASKS",
                        "CALCULATOR",
                        "DSR",
                        "CLIENTS",
                        "CHAT",
                        "BILLING",
                        "EQUIPMENT",
                        "EXPENSE"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        modules.forEach { module ->

                            val selected =
                                user.access
                                    .split(",")
                                    .map { it.trim() }
                                    .contains(module)

                            FilterChip(
                                selected = selected,

                                onClick = {
                                    val current =
                                        user.access
                                            .split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                            .toMutableSet()

                                    if (selected) {
                                        current.remove(module)
                                    } else {
                                        current.add(module)
                                    }

                                    /*
                                     * Access value is displayed locally.
                                     * Actual persistence should be handled
                                     * by AdminViewModel/backend.
                                     */
                                },

                                label = {
                                    Text(
                                        text = module,
                                        fontSize = 9.sp
                                    )
                                },

                                modifier = Modifier.padding(
                                    end = 4.dp
                                ),

                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShahGreen,
                                    selectedLabelColor = ShahWhite
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


/* ============================================================
   PENDING EXPENSES
   ============================================================ */

@Composable
fun PendingExpensesList(
    viewModel: AdminViewModel
) {

    if (
        viewModel.pendingExpenses.isEmpty() &&
        !viewModel.isLoading
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pending expense claims",
                color = ShahMediumGrey
            )
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {

        items(viewModel.pendingExpenses) { expense ->

            Card(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ShahWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = expense.employee,
                            fontWeight = FontWeight.Bold,
                            color = ShahBlack
                        )

                        Text(
                            text = expense.category,
                            color = ShahGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = expense.remarks,
                            color = ShahMediumGrey,
                            fontSize = 11.sp
                        )

                        Text(
                            text = expense.amount,
                            color = ShahBlack,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.approveExpense(
                                expense.id,
                                "REJECTED"
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Reject",
                            tint = ErrorRed
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.approveExpense(
                                expense.id,
                                "APPROVED"
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Approve",
                            tint = SuccessGreen
                        )
                    }
                }
            }
        }
    }
}


/* ============================================================
   ATTENDANCE
   ============================================================ */

@Composable
fun AdminAttendanceList(
    viewModel: AdminViewModel
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            StatItem(
                label = "Present",
                value = viewModel.presentCount.toString(),
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatItem(
                label = "Absent",
                value = viewModel.absentCount.toString(),
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )

            StatItem(
                label = "Leave",
                value = viewModel.leaveCount.toString(),
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )

            StatItem(
                label = "Pending",
                value = viewModel.notPunchedCount.toString(),
                color = ShahMediumGrey,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            color = ShahLightGrey
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {

            items(viewModel.attendanceRecords) { record ->

                Card(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ShahWhite
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = record.name,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = record.location,
                                fontSize = 11.sp,
                                color = ShahMediumGrey
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {

                            val statusColor =
                                when (record.status) {

                                    "PRESENT" ->
                                        SuccessGreen

                                    "LEAVE" ->
                                        WarningAmber

                                    else ->
                                        ErrorRed
                                }

                            Text(
                                text = record.status,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Text(
                                text = "IN: ${record.inTime}",
                                fontSize = 10.sp,
                                color = ShahMediumGrey
                            )
                        }
                    }
                }
            }
        }
    }
}


/* ============================================================
   STAT ITEM
   ============================================================ */

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {

        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ShahBlack
            )
        }
    }
}


/* ============================================================
   ADMIN SETTINGS
   ============================================================ */

@Composable
fun AdminSettingsList(
    viewModel: AdminViewModel,
    onNavigateToCompanySettings: () -> Unit,
    onNavigateToBankDetails: () -> Unit,
    onNavigateToTerms: () -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {

        item {

            // 1. Prominent Google Sheets Multi-Sheet Sync Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ShahDarkGreen
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(ShahGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ShahWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Sheets Full Backfill",
                                fontWeight = FontWeight.Bold,
                                color = ShahWhite,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Sync all Firestore records into Master sheets & Employee tabs",
                                color = ShahLightGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Master Sheets: Attendance, Payroll, Expenses, Leaves, Advances, DSR\n• Automatic Per-Employee individual tabs (e.g. Abhijeet Shah)\n• Google Maps clickable GPS locations",
                        color = ShahLightGrey.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.syncAllDataToGoogleSheets()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShahGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = ShahWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNC ALL PAST DATA TO GOOGLE SHEETS",
                            fontWeight = FontWeight.Bold,
                            color = ShahWhite,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            AdminSettingItem(
                title = "Company Profile",
                subtitle = "Logo, Address, Contact Info",
                icon = Icons.Default.Business,
                onClick = onNavigateToCompanySettings
            )

            AdminSettingItem(
                title = "Bank Details",
                subtitle = "Account No, IFSC, GSTIN",
                icon = Icons.Default.AccountBalance,
                onClick = onNavigateToBankDetails
            )

            AdminSettingItem(
                title = "Terms & Conditions",
                subtitle = "Reusable document terms",
                icon = Icons.Default.Gavel,
                onClick = onNavigateToTerms
            )

            AdminSettingItem(
                title = "Document Numbering",
                subtitle = "Prefixes and sequences",
                icon = Icons.Default.Numbers,
                onClick = {
                    // Future implementation
                }
            )

            AdminSettingItem(
                title = "Geo-Fence Settings",
                subtitle = "Radius and allowed sites",
                icon = Icons.Default.Map,
                onClick = {
                    // Future implementation
                }
            )
        }
    }
}


/* ============================================================
   ADMIN SETTING ITEM
   ============================================================ */

@Composable
fun AdminSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ShahWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ShahGreen,
                modifier = Modifier.size(24.dp)
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = ShahBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = subtitle,
                    color = ShahMediumGrey,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ShahLightGrey
            )
        }
    }
}


/* ============================================================
   CREATE USER DIALOG
   ============================================================ */

@Composable
fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var department by remember {
        mutableStateOf("SURVEY")
    }

    var role by remember {
        mutableStateOf("STAFF")
    }

    var access by remember {
        mutableStateOf("ATTENDANCE,TASKS")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Add New Employee")
        },

        text = {

            Column {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Full Name")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = {
                        Text("Mobile/Phone")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = {
                        department = it
                    },
                    label = {
                        Text("Department")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = {
                        role = it
                    },
                    label = {
                        Text("Role (ADMIN/STAFF)")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        name.isNotBlank() &&
                        phone.isNotBlank() &&
                        password.isNotBlank()
                    ) {

                        onCreate(
                            name.trim(),
                            phone.trim(),
                            password,
                            department.trim(),
                            role.trim().uppercase(),
                            access
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShahGreen
                )
            ) {

                Text("ADD")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "CANCEL",
                    color = ShahGreen
                )
            }
        }
    )
}