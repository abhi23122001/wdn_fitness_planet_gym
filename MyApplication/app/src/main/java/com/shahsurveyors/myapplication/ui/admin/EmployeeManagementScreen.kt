package com.shahsurveyors.myapplication.ui.admin

import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.shahsurveyors.myapplication.data.SalaryRepository
import com.shahsurveyors.myapplication.models.Employee360Report
import com.shahsurveyors.myapplication.models.SalaryProfileModel
import com.shahsurveyors.myapplication.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmployeeItem(
    val uid: String = "",
    val name: String = "",
    val id: String = "",
    val dept: String = "SURVEY",
    val role: String = "STAFF",
    val access: String = "ATTENDANCE,CHAT",
    val phone: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val active: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }
    val salaryRepository = remember { SalaryRepository() }
    val syncRepository = remember { com.shahsurveyors.myapplication.data.DataSyncRepository() }

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val employeeList = remember { mutableStateListOf<EmployeeItem>() }

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

    // Dialog states
    var selectedEmployeeForSettings by remember { mutableStateOf<EmployeeItem?>(null) }
    var selectedEmployeeSalaryHistory by remember { mutableStateOf<List<SalaryProfileModel>>(emptyList()) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    // 360 Degree Detail Dialog state
    var selected360Report by remember { mutableStateOf<Employee360Report?>(null) }
    var is360Loading by remember { mutableStateOf(false) }

    fun loadEmployees() {
        coroutineScope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("users").get().await()
                employeeList.clear()
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val name = doc.getString("name") ?: "Staff User"
                    val id = doc.getString("employeeId") ?: doc.getString("id") ?: uid.take(6).uppercase()
                    val dept = doc.getString("department") ?: doc.getString("dept") ?: "SURVEY"
                    val role = doc.getString("role") ?: "STAFF"
                    val access = doc.getString("access") ?: "ATTENDANCE,CHAT"
                    val phone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
                    val photoUrl = doc.getString("photoUrl") ?: doc.getString("dpUrl") ?: ""
                    val active = doc.getBoolean("active") ?: true

                    employeeList.add(
                        EmployeeItem(
                            uid = uid,
                            name = name,
                            id = id,
                            dept = dept,
                            role = role,
                            access = access,
                            phone = phone,
                            email = email,
                            photoUrl = photoUrl,
                            active = active
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading staff: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadEmployees()
    }

    val filteredEmployees = remember(employeeList, searchQuery) {
        if (searchQuery.isBlank()) {
            employeeList.toList()
        } else {
            employeeList.filter { employee ->
                employee.name.contains(searchQuery, ignoreCase = true) ||
                        employee.id.contains(searchQuery, ignoreCase = true) ||
                        employee.dept.contains(searchQuery, ignoreCase = true) ||
                        employee.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Employee & Staff Hub",
                        fontWeight = FontWeight.Bold,
                        color = ShahWhite,
                        fontSize = 18.sp
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
                    IconButton(onClick = { startGoogleSheetsSync() }) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync All Data to Google Sheet",
                            tint = ShahWhite
                        )
                    }
                    IconButton(onClick = { loadEmployees() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ShahWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShahDarkGreen,
                    titleContentColor = ShahWhite
                )
            )
        },
        containerColor = ShahBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name, ID, role or department...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ShahMediumGrey)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ShahMediumGrey)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Info banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = ShahGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = ShahDarkGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tap employee to open 360° Monthly Profile (Attendance, Leaves, GPS Map, Salary). Tap gear icon to change salary/permissions.",
                        fontSize = 11.sp,
                        color = ShahDarkGreen,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ShahGreen)
                }
            } else if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No employees found.",
                        color = ShahMediumGrey,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEmployees, key = { it.uid }) { employee ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        is360Loading = true
                                        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
                                        val report = salaryRepository.getEmployee360Report(employee.uid, currentMonth)
                                        selected360Report = report
                                        is360Loading = false
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ShahWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (employee.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = employee.photoUrl,
                                        contentDescription = employee.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, ShahGreen, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        color = ShahGreen.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = employee.name.trim().firstOrNull()?.uppercase() ?: "?",
                                                fontWeight = FontWeight.Bold,
                                                color = ShahGreen,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = employee.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ShahBlack
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "View 360",
                                            tint = ShahGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ID: ${employee.id} • ${employee.role.uppercase()}",
                                        fontSize = 12.sp,
                                        color = ShahMediumGrey
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${employee.dept} • Permissions: ${employee.access}",
                                        color = ShahGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val history = salaryRepository.getSalaryProfilesForEmployee(employee.uid)
                                            selectedEmployeeSalaryHistory = history
                                            selectedEmployeeForSettings = employee
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Configure Settings",
                                        tint = ShahGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 360 Degree Detail Dialog
    if (selected360Report != null || is360Loading) {
        Employee360DetailDialog(
            report = selected360Report,
            isLoading = is360Loading,
            onDismiss = { selected360Report = null }
        )
    }

    // Employee Settings Dialog (Salary & Permissions)
    selectedEmployeeForSettings?.let { emp ->
        EmployeeSettingsDialog(
            employeeUid = emp.uid,
            employeeName = emp.name,
            employeeId = emp.id,
            department = emp.dept,
            currentAccess = emp.access,
            salaryHistory = selectedEmployeeSalaryHistory,
            onDismiss = { selectedEmployeeForSettings = null },
            onSaveSalary = { monthly, daily, ot, effectiveFrom, note ->
                coroutineScope.launch {
                    val profile = SalaryProfileModel(
                        employeeUid = emp.uid,
                        employeeName = emp.name,
                        employeeId = emp.id,
                        department = emp.dept,
                        monthlySalary = monthly,
                        dailyRate = daily,
                        overtimeRatePerHour = ot,
                        effectiveFrom = effectiveFrom,
                        note = note,
                        setByName = "Admin",
                        active = true
                    )
                    val success = salaryRepository.saveSalaryProfile(profile)
                    if (success) {
                        Toast.makeText(context, "Salary updated for ${emp.name}", Toast.LENGTH_SHORT).show()
                        selectedEmployeeForSettings = null
                    } else {
                        Toast.makeText(context, "Failed to save salary", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSavePermissions = { newAccess ->
                coroutineScope.launch {
                    try {
                        firestore.collection("users").document(emp.uid)
                            .update("access", newAccess)
                        Toast.makeText(context, "Permissions updated for ${emp.name}", Toast.LENGTH_SHORT).show()
                        selectedEmployeeForSettings = null
                        loadEmployees()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Google Sheets Sync Dialog
    if (showSyncDialog) {
        com.shahsurveyors.myapplication.ui.components.GoogleSheetsSyncDialog(
            isSyncing = isSyncingToSheets,
            statusText = syncProgressStatus,
            progressPercent = syncProgressPercent,
            syncResult = syncResult,
            onDismiss = { showSyncDialog = false }
        )
    }
}