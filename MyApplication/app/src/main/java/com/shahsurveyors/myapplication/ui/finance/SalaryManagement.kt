package com.shahsurveyors.myapplication.ui.finance

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.shahsurveyors.myapplication.models.AdvanceSalaryRequest
import com.shahsurveyors.myapplication.models.PayrollRecord
import com.shahsurveyors.myapplication.ui.admin.Employee360DetailDialog
import com.shahsurveyors.myapplication.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryManagementScreen(
    viewModel: SalaryViewModel,
    currentUid: String = "",
    currentUserName: String = "Staff User",
    currentUserEmpId: String = "EMP001",
    currentUserDept: String = "SURVEY",
    isAdmin: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRequestAdvanceDialog by remember { mutableStateOf(false) }
    var selectedAdvanceForApproval by remember { mutableStateOf<AdvanceSalaryRequest?>(null) }

    LaunchedEffect(viewModel.selectedYearMonth, currentUid, isAdmin) {
        viewModel.loadPayrollData(currentUid, isAdmin)
    }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val filteredRecords = remember(viewModel.payrollRecords, viewModel.searchQuery) {
        if (viewModel.searchQuery.isBlank()) {
            viewModel.payrollRecords.toList()
        } else {
            viewModel.payrollRecords.filter {
                it.name.contains(viewModel.searchQuery, ignoreCase = true) ||
                        it.employeeId.contains(viewModel.searchQuery, ignoreCase = true) ||
                        it.dept.contains(viewModel.searchQuery, ignoreCase = true)
            }
        }
    }

    val formattedMonth = remember(viewModel.selectedYearMonth) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
            val date = sdf.parse(viewModel.selectedYearMonth)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(date)
            } else {
                viewModel.selectedYearMonth
            }
        } catch (e: Exception) {
            viewModel.selectedYearMonth
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isAdmin) "Salary & Payroll Management" else "My Salary & Payslip",
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
                    if (isAdmin) {
                        IconButton(onClick = {
                            viewModel.syncAllDataToGoogleSheets(currentUid, isAdmin)
                        }) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync All Data to Google Sheets",
                                tint = ShahWhite
                            )
                        }
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
            // Month Selector Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ShahWhite,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousMonth(currentUid, isAdmin) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = ShahDarkGreen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = ShahDarkGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedMonth,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ShahDarkGreen
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextMonth(currentUid, isAdmin) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = ShahDarkGreen
                        )
                    }
                }
            }

            if (isAdmin) {
                // Admin Tabs: Payroll Records vs Advance Requests
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ShahWhite,
                    contentColor = ShahDarkGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ShahGreen
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Monthly Payroll (${viewModel.payrollRecords.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Advance Requests (${viewModel.pendingAdvanceRequests.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    // Search bar
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search by name, ID or department...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ShahMediumGrey)
                        },
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ShahMediumGrey)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Summary Stats Header
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = ShahGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val totalNet = filteredRecords.sumOf { it.netSalary }
                            val totalPresent = filteredRecords.sumOf { it.presentDays }
                            val totalHalf = filteredRecords.sumOf { it.halfDays }
                            val totalAbsent = filteredRecords.sumOf { it.absentDays }

                            PayrollSummaryStat("Total Staff", "${filteredRecords.size}")
                            PayrollSummaryStat("Present (Full)", "$totalPresent")
                            PayrollSummaryStat("Half Days", "$totalHalf")
                            PayrollSummaryStat("Absent", "$totalAbsent")
                            PayrollSummaryStat("Total Payout", "₹ ${totalNet.toInt()}")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (viewModel.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ShahGreen)
                        }
                    } else if (filteredRecords.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No staff records found for $formattedMonth.\nAdd employees in Employee Management.",
                                color = ShahMediumGrey,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredRecords, key = { it.id }) { record ->
                                PayrollRecordCard(
                                    record = record,
                                    onOpen360Details = {
                                        viewModel.loadEmployee360(record.employeeUid, viewModel.selectedYearMonth)
                                    },
                                    onGenerateSlip = {
                                        val file = viewModel.generateSalarySlipPdf(context, record)
                                        if (file != null) {
                                            shareOrViewPdf(context, file)
                                        }
                                    },
                                    onSyncSheet = {
                                        viewModel.syncPayrollToGoogleSheets(record)
                                        Toast.makeText(context, "Synced ${record.name} to Google Sheet", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                } else {
                    // Admin Advance Requests tab
                    if (viewModel.pendingAdvanceRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending advance salary requests", color = ShahMediumGrey)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.pendingAdvanceRequests, key = { it.id }) { request ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = ShahWhite)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(request.employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ShahBlack)
                                                Text("ID: ${request.employeeId} • ${request.department}", fontSize = 12.sp, color = ShahMediumGrey)
                                            }

                                            Text(
                                                "₹ ${request.requestedAmount.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = ShahGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Installments: ${request.installments} months (₹ ${(request.requestedAmount / request.installments).toInt()}/mo)", fontSize = 12.sp, color = ShahDarkGrey)
                                        Text("Starting Month: ${request.requestedMonth}", fontSize = 12.sp, color = ShahDarkGrey)
                                        Text("Reason: ${request.reason}", fontSize = 12.sp, color = ShahDarkGrey)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = { selectedAdvanceForApproval = request },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ShahGreen, contentColor = ShahWhite),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("REVIEW & DECIDE", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // Non-Admin Employee View: Personal Salary Slip & Advance status
                val myRecord = viewModel.myPayrollRecord

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (myRecord != null) {
                        item {
                            PayrollRecordCard(
                                record = myRecord,
                                onOpen360Details = {
                                    viewModel.loadEmployee360(myRecord.employeeUid, viewModel.selectedYearMonth)
                                },
                                onGenerateSlip = {
                                    val file = viewModel.generateSalarySlipPdf(context, myRecord)
                                    if (file != null) {
                                        shareOrViewPdf(context, file)
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ShahWhite)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No salary calculated for $formattedMonth yet.",
                                        color = ShahMediumGrey,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Request Advance Salary Button
                    item {
                        Button(
                            onClick = { showRequestAdvanceDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ShahGreen, contentColor = ShahWhite),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REQUEST ADVANCE SALARY", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Employee Advance Requests History
                    item {
                        Text(
                            text = "My Advance Salary Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ShahBlack
                        )
                    }

                    if (viewModel.myAdvanceRequests.isEmpty()) {
                        item {
                            Text(
                                text = "No advance requests submitted yet.",
                                fontSize = 12.sp,
                                color = ShahMediumGrey
                            )
                        }
                    } else {
                        items(viewModel.myAdvanceRequests, key = { it.id }) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ShahWhite)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("₹ ${req.requestedAmount.toInt()} (${req.installments} EMIs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Month: ${req.requestedMonth} • ${req.reason}", fontSize = 11.sp, color = ShahMediumGrey)
                                    }

                                    Surface(
                                        color = when (req.status) {
                                            "APPROVED" -> SuccessGreen.copy(alpha = 0.15f)
                                            "REJECTED" -> ErrorRed.copy(alpha = 0.15f)
                                            else -> WarningAmber.copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = req.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (req.status) {
                                                "APPROVED" -> SuccessGreen
                                                "REJECTED" -> ErrorRed
                                                else -> WarningAmber
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 360 Degree Detail Dialog
    if (viewModel.selected360Report != null || viewModel.is360Loading) {
        Employee360DetailDialog(
            report = viewModel.selected360Report,
            isLoading = viewModel.is360Loading,
            onDismiss = { viewModel.clear360Report() },
            onGenerateSlip = {
                val rep = viewModel.selected360Report?.payroll
                if (rep != null) {
                    val file = viewModel.generateSalarySlipPdf(context, rep)
                    if (file != null) shareOrViewPdf(context, file)
                }
            }
        )
    }

    // Advance Dialogs
    if (showRequestAdvanceDialog) {
        RequestAdvanceSalaryDialog(
            currentYearMonth = viewModel.selectedYearMonth,
            onDismiss = { showRequestAdvanceDialog = false },
            onSubmit = { amount, installments, month, reason ->
                viewModel.submitAdvanceRequest(
                    employeeUid = currentUid,
                    employeeName = currentUserName,
                    employeeId = currentUserEmpId,
                    department = currentUserDept,
                    amount = amount,
                    installments = installments,
                    month = month,
                    reason = reason
                )
            }
        )
    }

    selectedAdvanceForApproval?.let { req ->
        AdminAdvanceDecisionDialog(
            request = req,
            onDismiss = { selectedAdvanceForApproval = null },
            onDecide = { status, approvedAmount, installments, note ->
                viewModel.decideAdvanceRequest(
                    requestId = req.id,
                    status = status,
                    approvedAmount = approvedAmount,
                    installments = installments,
                    adminUid = currentUid,
                    adminName = currentUserName,
                    note = note,
                    currentUid = currentUid
                )
            }
        )
    }

    // Google Sheets Sync Dialog
    if (viewModel.showSyncDialog) {
        com.shahsurveyors.myapplication.ui.components.GoogleSheetsSyncDialog(
            isSyncing = viewModel.isSyncingToSheets,
            statusText = viewModel.syncProgressStatus,
            progressPercent = viewModel.syncProgressPercent,
            syncResult = viewModel.lastSyncResult,
            onDismiss = { viewModel.dismissSyncDialog() }
        )
    }
}

@Composable
fun PayrollRecordCard(
    record: PayrollRecord,
    onOpen360Details: () -> Unit,
    onGenerateSlip: () -> Unit,
    onSyncSheet: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen360Details() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShahWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Name, Dept, Net Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ShahBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "View 360 Summary",
                            tint = ShahGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "ID: ${record.employeeId} • ${record.dept} • ${record.role}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShahMediumGrey
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET SALARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShahMediumGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹ ${String.format(Locale.ENGLISH, "%,.0f", record.netSalary)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ShahGreen
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = ShahLightGrey
            )

            // Attendance & Days Breakdown (Present, Half Days, Absent, Leaves)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PayrollInfoItem("Present", "${record.presentDays}d")
                PayrollInfoItem("Half Day", "${record.halfDays}d")
                PayrollInfoItem("Absent", "${record.absentDays}d")
                PayrollInfoItem("Leaves", "${record.approvedLeaveDays}d")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Earnings vs Deductions summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PayrollInfoItem("Base Rate", "₹ ${record.baseMonthlySalary.toInt()}")
                PayrollInfoItem("Gross Earned", "₹ ${record.grossSalaryEarned.toInt()}")
                PayrollInfoItem("Advance Ded.", "-₹ ${record.advanceDeduction.toInt()}")
                PayrollInfoItem("Absence Ded.", "-₹ ${record.absenceDeduction.toInt()}")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen360Details,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShahDarkGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("360° SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onGenerateSlip,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ShahGreen, contentColor = ShahWhite),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SLIP (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PayrollInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = ShahMediumGrey)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ShahBlack)
    }
}

@Composable
private fun PayrollSummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ShahDarkGreen)
        Text(label, fontSize = 10.sp, color = ShahDarkGrey)
    }
}



private fun shareOrViewPdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open Salary Slip"))
    } catch (e: Exception) {
        Toast.makeText(context, "Salary slip saved at: ${file.name}", Toast.LENGTH_LONG).show()
    }
}