/**
 * =========================================================================
 * SHAH SURVEYORS ERP - ENTERPRISE GOOGLE SHEETS BACKEND & REAL-TIME WEBHOOK
 * =========================================================================
 * 
 * Features:
 * 1. Automatic Per-Employee Tabs: Format `EMPID_Employee_Name` (e.g. EMP001_Rahul_Kumar)
 * 2. Employee Tab Layout:
 *    - Top Header Banner (Row 1)
 *    - Employee Profile Info Card (Rows 3-6 Left)
 *    - Dynamic Monthly Attendance KPI Card with LIVE Excel Formulas (Rows 3-7 Right):
 *        • Total Present: =COUNTIF(C11:C60, "P")
 *        • Total Absent: =COUNTIF(C11:C60, "A")
 *        • Total Half Day: =COUNTIF(C11:C60, "HF")
 *        • Total Working Days: =COUNTIF(C11:C60, "P") + (COUNTIF(C11:C60, "HF")*0.5)
 *        • Attendance %: =IF(COUNTA(A11:A60)>0, ROUND(((COUNTIF(C11:C60, "P") + (COUNTIF(C11:C60, "HF")*0.5))/COUNTA(A11:A60))*100, 1) & "%", "0%")
 *    - Attendance Table (Row 10): Date | Day | Status (P/A/HF) | Check In | Check Out | Working Hours | Site | Remarks
 *    - Conditional Formatting on Status (P = Light Green, A = Light Red, HF = Light Amber)
 *    - Expense Table (Row 63): Date | Expense ID | Category | Description | Amount | Payment Mode | Status | Receipt Link
 * 3. Master Sheets Retention:
 *    - Master_Attendance / Attendance
 *    - Master_Expenses / Expenses
 *    - Master_Payroll / Payroll
 *    - Master_Leaves / Leaves
 *    - Master_Advances / Advances
 *    - Master_DSR / DSR
 * 4. Idempotency & Upsert:
 *    - Attendance: Match Date in Employee tab & (Employee ID + Date) in Master sheet
 *    - Expense: Match Expense ID in Employee tab & Master sheet
 * 5. Actions Handled:
 *    - CREATE_EMPLOYEE / SYNC_EMPLOYEE
 *    - ATTENDANCE_PUNCH / ATTENDANCE_SYNC / PUNCH_IN / PUNCH_OUT / ATTENDANCE_DELETE
 *    - EXPENSE_SYNC / EXPENSE_ADD / EXPENSE_DELETE
 *    - LEAVE_SYNC, ADVANCE_SALARY_SYNC, SYNC_PAYROLL, DSR_SYNC, BULK_SYNC
 *    - doGet: Health check & FETCH_ALL_SYNC_DATA
 */

// =========================================================================
// 1. GET HANDLER (Health Check & Data Fetch)
// =========================================================================

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "";
  var ss = SpreadsheetApp.getActiveSpreadsheet();

  if (action === "FETCH_ALL_SYNC_DATA") {
    var attSheet = ss.getSheetByName("Attendance") || ss.getSheetByName("Master_Attendance");
    var expSheet = ss.getSheetByName("Expenses") || ss.getSheetByName("Master_Expenses");
    
    var attendanceData = attSheet ? attSheet.getDataRange().getValues() : [];
    var expensesData = expSheet ? expSheet.getDataRange().getValues() : [];
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "SUCCESS",
      attendanceCount: attendanceData.length > 1 ? attendanceData.length - 1 : 0,
      expenseCount: expensesData.length > 1 ? expensesData.length - 1 : 0,
      attendance: attendanceData,
      expenses: expensesData
    })).setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput(JSON.stringify({
    status: "SUCCESS",
    message: "Shah Surveyors ERP Google Apps Script Webhook is Active",
    spreadsheetName: ss.getName(),
    sheetsCount: ss.getSheets().length,
    timestamp: new Date().toISOString()
  })).setMimeType(ContentService.MimeType.JSON);
}

// =========================================================================
// 2. POST HANDLER (Main Action Router)
// =========================================================================

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000); // 30 seconds wait lock for thread safety
  } catch (lockErr) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: "Server busy, please retry in a moment"
    })).setMimeType(ContentService.MimeType.JSON);
  }

  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var payload = {};

    if (e && e.postData && e.postData.contents) {
      payload = JSON.parse(e.postData.contents);
    } else {
      payload = e.parameter || {};
    }

    var action = (payload.action || "").toString().trim().toUpperCase();
    var result = { status: "SUCCESS", action: action };

    if (action === "BULK_SYNC" && payload.records && Array.isArray(payload.records)) {
      var processed = 0;
      for (var i = 0; i < payload.records.length; i++) {
        var record = payload.records[i];
        routeAction(ss, record);
        processed++;
      }
      result.processedCount = processed;
      result.message = "Successfully processed " + processed + " records in bulk sync.";
    } else {
      routeAction(ss, payload);
      result.message = "Action " + action + " executed successfully.";
    }

    return ContentService.createTextOutput(JSON.stringify(result))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.toString(),
      stack: err.stack
    })).setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}

// =========================================================================
// 3. ACTION ROUTER
// =========================================================================

function routeAction(ss, data) {
  var action = (data.action || "").toString().trim().toUpperCase();

  switch (action) {
    case "CREATE_EMPLOYEE":
    case "SYNC_EMPLOYEE":
      handleCreateEmployee(ss, data);
      break;

    case "ATTENDANCE_PUNCH":
    case "ATTENDANCE_SYNC":
    case "PUNCH_IN":
    case "PUNCH_OUT":
      handleAttendancePunch(ss, data);
      break;

    case "ATTENDANCE_DELETE":
      handleAttendanceDelete(ss, data);
      break;

    case "EXPENSE_SYNC":
    case "EXPENSE_ADD":
      handleExpenseSync(ss, data);
      break;

    case "EXPENSE_DELETE":
      handleExpenseDelete(ss, data);
      break;

    case "LEAVE_SYNC":
      handleLeaveSync(ss, data);
      break;

    case "ADVANCE_SALARY_SYNC":
      handleAdvanceSalarySync(ss, data);
      break;

    case "SYNC_PAYROLL":
      handlePayrollSync(ss, data);
      break;

    case "DSR_SYNC":
      handleDsrSync(ss, data);
      break;

    default:
      // Fallback for legacy attendance punch
      if (data.staffName || data.EmployeeName || data.punchType) {
        handleAttendancePunch(ss, data);
      }
      break;
  }
}

// =========================================================================
// 4. EMPLOYEE TAB GENERATOR & LAYOUT BUILDER
// =========================================================================

/**
 * Sanitizes and generates tab name: EMPID_Employee_Name (e.g. EMP001_Rahul_Kumar)
 */
function sanitizeEmployeeTabName(empId, empName) {
  var cleanId = (empId || "EMP001").toString().trim().replace(/[:\\\/\?\*\[\]]/g, "_");
  var cleanName = (empName || "Staff").toString().trim()
    .replace(/[^a-zA-Z0-9]/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_|_$/g, "");
  
  var fullName = cleanId + "_" + cleanName;
  if (fullName.length > 95) {
    fullName = fullName.substring(0, 95);
  }
  return fullName;
}

/**
 * Finds or creates employee tab with full headers, profile info card, live KPI formulas & conditional formatting.
 */
function getOrCreateEmployeeSheet(ss, empId, empName, metadata) {
  metadata = metadata || {};
  var targetTabName = sanitizeEmployeeTabName(empId, empName);
  var targetPrefix = (empId || "EMP001").toString().trim().toUpperCase() + "_";

  // Check if sheet exists by exact name or prefix match
  var sheet = ss.getSheetByName(targetTabName);
  if (!sheet) {
    var allSheets = ss.getSheets();
    for (var i = 0; i < allSheets.length; i++) {
      if (allSheets[i].getName().toUpperCase().indexOf(targetPrefix) === 0) {
        sheet = allSheets[i];
        break;
      }
    }
  }

  if (!sheet) {
    sheet = ss.insertSheet(targetTabName);
    buildEmployeeSheetLayout(sheet, empId, empName, metadata);
  } else if (metadata && metadata.forceUpdateInfo) {
    updateEmployeeInfoCard(sheet, empId, empName, metadata);
  }

  return sheet;
}

/**
 * Builds the complete professional layout for an employee sheet.
 */
function buildEmployeeSheetLayout(sheet, empId, empName, meta) {
  meta = meta || {};
  sheet.setTabColor("#2E7D32");

  // 1. Title Banner (Row 1)
  sheet.getRange("A1:H1").merge()
    .setValue("Shah Surveyors ERP - Employee Attendance & Expense Record")
    .setBackground("#1B5E20")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setFontSize(13)
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle");
  sheet.setRowHeight(1, 36);

  // 2. Employee Profile Info Card (Rows 3-6, Cols A-D)
  var infoLabels = [
    ["Employee ID:", empId || "EMP001", "Joining Date:", meta.joiningDate || "01-01-2025"],
    ["Employee Name:", empName || "Employee", "Project / Site:", meta.siteName || meta.projectSite || "Main Site"],
    ["Designation:", meta.designation || meta.role || "Surveyor", "Phone:", meta.phone || ""],
    ["Status:", meta.status || "ACTIVE", "Department:", meta.department || "SURVEY"]
  ];

  for (var r = 0; r < infoLabels.length; r++) {
    var rowIdx = 3 + r;
    sheet.getRange(rowIdx, 1).setValue(infoLabels[r][0]).setFontWeight("bold").setBackground("#E8F5E9").setFontColor("#1B5E20");
    sheet.getRange(rowIdx, 2).setValue(infoLabels[r][1]).setFontWeight("bold").setHorizontalAlignment("left");
    sheet.getRange(rowIdx, 3).setValue(infoLabels[r][2]).setFontWeight("bold").setBackground("#E8F5E9").setFontColor("#1B5E20");
    sheet.getRange(rowIdx, 4).setValue(infoLabels[r][3]).setHorizontalAlignment("left");
    sheet.setRowHeight(rowIdx, 22);
  }

  // 3. Monthly Attendance KPI Summary Card (Rows 3-7, Cols F-G)
  var kpiConfig = [
    ["Total Present:", "=COUNTIF(C11:C60, \"P\")", "#E8F5E9", "#1B5E20"],
    ["Total Absent:", "=COUNTIF(C11:C60, \"A\")", "#FFEBEE", "#C62828"],
    ["Total Half Day:", "=COUNTIF(C11:C60, \"HF\")", "#FFF8E1", "#F57F17"],
    ["Total Working Days:", "=COUNTIF(C11:C60, \"P\") + (COUNTIF(C11:C60, \"HF\")*0.5)", "#E0F2F1", "#004D40"],
    ["Attendance %:", "=IF(COUNTA(A11:A60)>0, ROUND(((COUNTIF(C11:C60, \"P\") + (COUNTIF(C11:C60, \"HF\")*0.5))/COUNTA(A11:A60))*100, 1) & \"%\", \"0%\")", "#EDE7F6", "#311B92"]
  ];

  for (var k = 0; k < kpiConfig.length; k++) {
    var kpiRow = 3 + k;
    sheet.getRange(kpiRow, 6).setValue(kpiConfig[k][0]).setFontWeight("bold").setBackground(kpiConfig[k][2]).setFontColor(kpiConfig[k][3]);
    sheet.getRange(kpiRow, 7).setFormula(kpiConfig[k][1]).setFontWeight("bold").setHorizontalAlignment("center").setBackground("#FAFAFA");
    sheet.setRowHeight(kpiRow, 22);
  }

  // 4. Attendance Section Header (Row 9)
  sheet.getRange("A9:H9").merge()
    .setValue("📅 ATTENDANCE LOG (PRESENT: P | ABSENT: A | HALF DAY: HF)")
    .setBackground("#2E7D32")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setFontSize(11)
    .setHorizontalAlignment("left");
  sheet.setRowHeight(9, 26);

  // 5. Attendance Table Column Headers (Row 10)
  var attHeaders = ["Date", "Day", "Status (P/A/HF)", "Check In", "Check Out", "Working Hours", "Site / Project", "Remarks"];
  var headerRange = sheet.getRange(10, 1, 1, attHeaders.length);
  headerRange.setValues([attHeaders])
    .setBackground("#388E3C")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle");
  sheet.setRowHeight(10, 24);

  // 6. Pre-set Border Grid for Attendance Rows 11-60
  var attDataGrid = sheet.getRange(11, 1, 50, 8);
  attDataGrid.setBorder(true, true, true, true, true, true, "#E0E0E0", SpreadsheetApp.BorderStyle.SOLID);
  sheet.getRange(11, 3, 50, 1).setHorizontalAlignment("center").setFontWeight("bold"); // Status Col Center
  sheet.getRange(11, 1, 50, 1).setHorizontalAlignment("center"); // Date Col Center

  // 7. Conditional Formatting Rules for Status Column (C11:C60)
  var statusRange = sheet.getRange("C11:C60");
  var rulePresent = SpreadsheetApp.newConditionalFormatRule()
    .whenTextEqualTo("P")
    .setBackground("#C8E6C9")
    .setFontColor("#1B5E20")
    .setBold(true)
    .setRanges([statusRange])
    .build();

  var ruleAbsent = SpreadsheetApp.newConditionalFormatRule()
    .whenTextEqualTo("A")
    .setBackground("#FFCDD2")
    .setFontColor("#B71C1C")
    .setBold(true)
    .setRanges([statusRange])
    .build();

  var ruleHalfDay = SpreadsheetApp.newConditionalFormatRule()
    .whenTextEqualTo("HF")
    .setBackground("#FFE082")
    .setFontColor("#E65100")
    .setBold(true)
    .setRanges([statusRange])
    .build();

  sheet.setConditionalFormatRules([rulePresent, ruleAbsent, ruleHalfDay]);

  // 8. Expense Section Header (Row 62)
  sheet.getRange("A62:H62").merge()
    .setValue("💰 EXPENSE CLAIMS & REIMBURSEMENTS")
    .setBackground("#1565C0")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setFontSize(11)
    .setHorizontalAlignment("left");
  sheet.setRowHeight(62, 26);

  // 9. Expense Table Column Headers (Row 63)
  var expHeaders = ["Date", "Expense ID", "Category", "Description / Remarks", "Amount (₹)", "Payment Mode", "Status", "Receipt Link"];
  sheet.getRange(63, 1, 1, expHeaders.length)
    .setValues([expHeaders])
    .setBackground("#1976D2")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle");
  sheet.setRowHeight(63, 24);

  // Set Column Widths for Optimal View
  sheet.setColumnWidth(1, 110); // Date
  sheet.setColumnWidth(2, 110); // Day / Expense ID
  sheet.setColumnWidth(3, 130); // Status / Category
  sheet.setColumnWidth(4, 120); // Check In / Remarks
  sheet.setColumnWidth(5, 120); // Check Out / Amount
  sheet.setColumnWidth(6, 120); // Working Hours / Mode
  sheet.setColumnWidth(7, 160); // Site / Status
  sheet.setColumnWidth(8, 200); // Remarks / Receipt Link
}

function updateEmployeeInfoCard(sheet, empId, empName, meta) {
  if (empId) sheet.getRange("B3").setValue(empId);
  if (meta.joiningDate) sheet.getRange("D3").setValue(meta.joiningDate);
  if (empName) sheet.getRange("B4").setValue(empName);
  if (meta.siteName || meta.projectSite) sheet.getRange("D4").setValue(meta.siteName || meta.projectSite);
  if (meta.designation || meta.role) sheet.getRange("B5").setValue(meta.designation || meta.role);
  if (meta.phone) sheet.getRange("D5").setValue(meta.phone);
  if (meta.status) sheet.getRange("B6").setValue(meta.status);
  if (meta.department) sheet.getRange("D6").setValue(meta.department);
}

// =========================================================================
// 5. MASTER SHEETS CREATOR & CACHING
// =========================================================================

function getOrCreateMasterSheet(ss, sheetName, headers, headerColor) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.getRange(1, 1, 1, headers.length)
      .setValues([headers])
      .setBackground(headerColor || "#1B5E20")
      .setFontColor("#FFFFFF")
      .setFontWeight("bold")
      .setHorizontalAlignment("center");
    sheet.setRowHeight(1, 28);
    sheet.setFrozenRows(1);
  }
  return sheet;
}

// =========================================================================
// 6. ACTION IMPLEMENTATIONS
// =========================================================================

/**
 * Handles CREATE_EMPLOYEE / SYNC_EMPLOYEE
 */
function handleCreateEmployee(ss, data) {
  var empId = data.EmployeeID || data.employeeId || data.empId || "EMP001";
  var empName = data.EmployeeName || data.staffName || data.name || "Employee";
  getOrCreateEmployeeSheet(ss, empId, empName, data);
}

/**
 * Handles ATTENDANCE_PUNCH / ATTENDANCE_SYNC / PUNCH_IN / PUNCH_OUT
 * Upserts to Master_Attendance AND to the dedicated EMPID_Employee_Name tab.
 */
function handleAttendancePunch(ss, data) {
  var empId = data.EmployeeID || data.employeeId || data.empId || "EMP001";
  var empName = data.EmployeeName || data.staffName || data.name || "Employee";
  var dateStr = formatDate(data.date || new Date());
  var dayName = data.day || getDayName(dateStr);
  var time = data.time || data.punchInTime || data.punchOutTime || formatTime(new Date());
  var punchType = (data.punchType || data.type || data.action || "PUNCH_IN").toString().toUpperCase();
  var site = data.siteName || data.workArea || data.site || "Main Site";
  var remarks = data.remarks || "Mobile Punch";
  var mapsUrl = data.googleMapsUrl || data.mapsUrl || "";
  var lat = data.lat || data.Latitude || "";
  var lng = data.lng || data.Longitude || "";
  var coords = (lat && lng) ? (lat + ", " + lng) : "";

  // Normalize Status to P, A, HF
  var rawStatus = (data.status || "PRESENT").toString().toUpperCase();
  var status = "P";
  if (rawStatus.indexOf("HALF") !== -1 || rawStatus === "HF") {
    status = "HF";
  } else if (rawStatus.indexOf("ABSENT") !== -1 || rawStatus === "A") {
    status = "A";
  }

  var checkIn = data.checkIn || (punchType.indexOf("IN") !== -1 ? time : "09:00 AM");
  var checkOut = data.checkOut || (punchType.indexOf("OUT") !== -1 ? time : "PENDING");
  var workingHours = data.workingHours || (punchType.indexOf("OUT") !== -1 ? "8h 30m" : "In Progress");

  // 1. UPDATE MASTER ATTENDANCE SHEET
  var masterAtt = getOrCreateMasterSheet(ss, "Attendance", [
    "Timestamp", "Employee ID", "Employee Name", "Date", "Day", "Status (P/A/HF)",
    "Punch Type", "Check In", "Check Out", "Working Hours", "Site / Area", "GPS Coordinates", "Google Maps URL"
  ], "#1B5E20");

  var masterData = masterAtt.getDataRange().getValues();
  var foundMasterRow = -1;

  for (var r = 1; r < masterData.length; r++) {
    var rEmpId = (masterData[r][1] || "").toString().trim();
    var rDate = formatDate(masterData[r][3]);
    if (rEmpId === empId.toString().trim() && rDate === dateStr) {
      foundMasterRow = r + 1; // 1-indexed
      break;
    }
  }

  var masterRowData = [
    new Date(), empId, empName, dateStr, dayName, status,
    punchType, checkIn, checkOut, workingHours, site, coords, mapsUrl
  ];

  if (foundMasterRow > 0) {
    masterAtt.getRange(foundMasterRow, 1, 1, masterRowData.length).setValues([masterRowData]);
  } else {
    masterAtt.appendRow(masterRowData);
  }

  // 2. UPDATE PER-EMPLOYEE DEDICATED TAB
  var empSheet = getOrCreateEmployeeSheet(ss, empId, empName, data);
  var empValues = empSheet.getRange("A11:H60").getValues();
  var foundEmpRow = -1;
  var firstEmptyRow = -1;

  for (var i = 0; i < empValues.length; i++) {
    var cellDate = formatDate(empValues[i][0]);
    if (cellDate === dateStr) {
      foundEmpRow = 11 + i;
      break;
    }
    if (firstEmptyRow === -1 && (!empValues[i][0] || empValues[i][0].toString().trim() === "")) {
      firstEmptyRow = 11 + i;
    }
  }

  var empRowData = [dateStr, dayName, status, checkIn, checkOut, workingHours, site, remarks];

  if (foundEmpRow > 0) {
    empSheet.getRange(foundEmpRow, 1, 1, empRowData.length).setValues([empRowData]);
  } else if (firstEmptyRow > 0) {
    empSheet.getRange(firstEmptyRow, 1, 1, empRowData.length).setValues([empRowData]);
  } else {
    empSheet.insertRowBefore(61);
    empSheet.getRange(61, 1, 1, empRowData.length).setValues([empRowData]);
  }
}

/**
 * Handles ATTENDANCE_DELETE
 */
function handleAttendanceDelete(ss, data) {
  var empId = data.EmployeeID || data.employeeId || data.empId || "";
  var empName = data.EmployeeName || data.staffName || data.name || "";
  var dateStr = formatDate(data.date || "");

  if (!dateStr) return;

  // 1. Delete from Master
  var masterAtt = ss.getSheetByName("Attendance") || ss.getSheetByName("Master_Attendance");
  if (masterAtt) {
    var values = masterAtt.getDataRange().getValues();
    for (var i = values.length - 1; i >= 1; i--) {
      var rEmp = (values[i][1] || "").toString().trim();
      var rDate = formatDate(values[i][3]);
      if ((!empId || rEmp === empId) && rDate === dateStr) {
        masterAtt.deleteRow(i + 1);
      }
    }
  }

  // 2. Clear from Employee Tab
  var empSheet = getOrCreateEmployeeSheet(ss, empId, empName);
  if (empSheet) {
    var empValues = empSheet.getRange("A11:H60").getValues();
    for (var k = 0; k < empValues.length; k++) {
      if (formatDate(empValues[k][0]) === dateStr) {
        empSheet.getRange(11 + k, 1, 1, 8).clearContent();
      }
    }
  }
}

/**
 * Handles EXPENSE_SYNC / EXPENSE_ADD
 * Upserts into Master_Expenses AND Employee Tab.
 */
function handleExpenseSync(ss, data) {
  var expenseId = data.expenseId || data.id || ("EXP_" + new Date().getTime());
  var empId = data.EmployeeID || data.employeeId || data.empId || "EMP001";
  var empName = data.EmployeeName || data.staffName || data.name || "Employee";
  var dateStr = formatDate(data.date || new Date());
  var category = data.category || "General";
  var desc = data.description || data.remarks || data.title || "Expense";
  var amount = parseFloat(data.amount) || 0.0;
  var paymentMode = data.paymentMode || "UPI / Cash";
  var status = (data.status || "PENDING").toString().toUpperCase();
  var receiptUrl = data.receiptUrl || "";

  // 1. UPDATE MASTER EXPENSES SHEET
  var masterExp = getOrCreateMasterSheet(ss, "Expenses", [
    "Timestamp", "Expense ID", "Employee ID", "Employee Name", "Date",
    "Category", "Description / Remarks", "Amount (₹)", "Payment Mode", "Status", "Receipt Link"
  ], "#1565C0");

  var masterData = masterExp.getDataRange().getValues();
  var foundMasterRow = -1;

  for (var r = 1; r < masterData.length; r++) {
    var rExpId = (masterData[r][1] || "").toString().trim();
    if (rExpId === expenseId.toString().trim()) {
      foundMasterRow = r + 1;
      break;
    }
  }

  var masterRowData = [
    new Date(), expenseId, empId, empName, dateStr,
    category, desc, amount, paymentMode, status, receiptUrl
  ];

  if (foundMasterRow > 0) {
    masterExp.getRange(foundMasterRow, 1, 1, masterRowData.length).setValues([masterRowData]);
  } else {
    masterExp.appendRow(masterRowData);
  }

  // 2. UPDATE PER-EMPLOYEE DEDICATED TAB
  var empSheet = getOrCreateEmployeeSheet(ss, empId, empName, data);
  var lastRow = Math.max(64, empSheet.getLastRow());
  var expValues = lastRow >= 64 ? empSheet.getRange(64, 1, lastRow - 63, 8).getValues() : [];
  var foundEmpRow = -1;

  for (var i = 0; i < expValues.length; i++) {
    var cellExpId = (expValues[i][1] || "").toString().trim();
    if (cellExpId === expenseId.toString().trim()) {
      foundEmpRow = 64 + i;
      break;
    }
  }

  var empExpRow = [dateStr, expenseId, category, desc, amount, paymentMode, status, receiptUrl];

  if (foundEmpRow > 0) {
    empSheet.getRange(foundEmpRow, 1, 1, empExpRow.length).setValues([empExpRow]);
  } else {
    var nextRow = Math.max(64, empSheet.getLastRow() + 1);
    empSheet.getRange(nextRow, 1, 1, empExpRow.length).setValues([empExpRow]);
    empSheet.getRange(nextRow, 5).setNumberFormat("₹#,##0.00");
  }
}

/**
 * Handles EXPENSE_DELETE
 */
function handleExpenseDelete(ss, data) {
  var expenseId = (data.expenseId || data.id || "").toString().trim();
  var empId = data.EmployeeID || data.employeeId || data.empId || "";
  var empName = data.EmployeeName || data.staffName || data.name || "";

  if (!expenseId) return;

  // 1. Master sheet
  var masterExp = ss.getSheetByName("Expenses") || ss.getSheetByName("Master_Expenses");
  if (masterExp) {
    var values = masterExp.getDataRange().getValues();
    for (var i = values.length - 1; i >= 1; i--) {
      if ((values[i][1] || "").toString().trim() === expenseId) {
        masterExp.deleteRow(i + 1);
      }
    }
  }

  // 2. Employee Tab
  var empSheet = getOrCreateEmployeeSheet(ss, empId, empName);
  if (empSheet) {
    var lastRow = empSheet.getLastRow();
    if (lastRow >= 64) {
      var expValues = empSheet.getRange(64, 1, lastRow - 63, 8).getValues();
      for (var k = expValues.length - 1; k >= 0; k--) {
        if ((expValues[k][1] || "").toString().trim() === expenseId) {
          empSheet.deleteRow(64 + k);
        }
      }
    }
  }
}

/**
 * Handles LEAVE_SYNC
 */
function handleLeaveSync(ss, data) {
  var masterLve = getOrCreateMasterSheet(ss, "Leaves", [
    "Timestamp", "Employee ID", "Employee Name", "Start Date", "End Date",
    "Total Days", "Leave Type", "Reason", "Status"
  ], "#6A1B9A");

  masterLve.appendRow([
    new Date(),
    data.EmployeeID || data.employeeId || data.empId || "EMP001",
    data.EmployeeName || data.staffName || data.name || "Employee",
    data.startDate || "",
    data.endDate || "",
    data.totalDays || 1,
    data.leaveType || "CASUAL",
    data.reason || "",
    data.status || "PENDING"
  ]);
}

/**
 * Handles ADVANCE_SALARY_SYNC
 */
function handleAdvanceSalarySync(ss, data) {
  var masterAdv = getOrCreateMasterSheet(ss, "Advances", [
    "Timestamp", "Employee ID", "Employee Name", "Requested Month",
    "Requested Amount", "Approved Amount", "Installments", "Reason", "Status"
  ], "#E65100");

  masterAdv.appendRow([
    new Date(),
    data.EmployeeID || data.employeeId || data.empId || "EMP001",
    data.EmployeeName || data.staffName || data.name || "Employee",
    data.requestedMonth || "",
    parseFloat(data.requestedAmount) || 0.0,
    parseFloat(data.approvedAmount) || 0.0,
    parseInt(data.installments) || 1,
    data.reason || "",
    data.status || "PENDING"
  ]);
}

/**
 * Handles SYNC_PAYROLL
 */
function handlePayrollSync(ss, data) {
  var masterPay = getOrCreateMasterSheet(ss, "Payroll", [
    "Timestamp", "Month", "Employee ID", "Employee Name", "Department", "Role",
    "Base Salary", "Daily Rate", "Total Days", "Working Days", "Present Days",
    "Half Days", "Leave Days", "Absent Days", "Gross Salary", "Advance Cut", "Absent Cut", "Net Salary", "Status"
  ], "#00695C");

  var empId = data.EmployeeID || data.employeeId || data.empId || "EMP001";
  var month = data.month || data.salaryMonth || "";
  var values = masterPay.getDataRange().getValues();
  var foundRow = -1;

  for (var r = 1; r < values.length; r++) {
    if ((values[r][1] || "").toString() === month && (values[r][2] || "").toString() === empId) {
      foundRow = r + 1;
      break;
    }
  }

  var row = [
    new Date(), month, empId,
    data.EmployeeName || data.staffName || data.name || "Employee",
    data.department || data.dept || "SURVEY",
    data.role || "STAFF",
    parseFloat(data.baseMonthlySalary) || 0,
    parseFloat(data.dailyRate) || 0,
    parseInt(data.totalDaysInMonth) || 30,
    parseInt(data.workingDaysInMonth) || 26,
    parseInt(data.presentDays) || 0,
    parseInt(data.halfDays) || 0,
    parseInt(data.approvedLeaveDays) || 0,
    parseInt(data.absentDays) || 0,
    parseFloat(data.grossSalaryEarned) || 0,
    parseFloat(data.advanceDeduction) || 0,
    parseFloat(data.absenceDeduction) || 0,
    parseFloat(data.netSalary) || 0,
    data.status || "CALCULATED"
  ];

  if (foundRow > 0) {
    masterPay.getRange(foundRow, 1, 1, row.length).setValues([row]);
  } else {
    masterPay.appendRow(row);
  }
}

/**
 * Handles DSR_SYNC
 */
function handleDsrSync(ss, data) {
  var masterDsr = getOrCreateMasterSheet(ss, "DSR", [
    "Timestamp", "Date", "Employee ID", "Employee Name",
    "Chainage", "Points", "Area", "Instrument", "Remarks"
  ], "#37474F");

  masterDsr.appendRow([
    new Date(),
    formatDate(data.date || new Date()),
    data.EmployeeID || data.employeeId || data.empId || "EMP001",
    data.EmployeeName || data.staffName || data.name || "Employee",
    data.chainage || "",
    data.points || "",
    data.area || "",
    data.instrument || "",
    data.remarks || ""
  ]);
}

// =========================================================================
// 7. HELPER FORMATTING FUNCTIONS
// =========================================================================

function formatDate(val) {
  if (!val) return Utilities.formatDate(new Date(), "Asia/Kolkata", "dd-MM-yyyy");
  if (val instanceof Date) {
    return Utilities.formatDate(val, "Asia/Kolkata", "dd-MM-yyyy");
  }
  var str = val.toString().trim();
  // If YYYY-MM-DD
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) {
    var parts = str.split("-");
    return parts[2] + "-" + parts[1] + "-" + parts[0];
  }
  // If DD-MM-YYYY or DD/MM/YYYY
  if (/^\d{2}[-\/]\d{2}[-\/]\d{4}$/.test(str)) {
    return str.replace(/\//g, "-");
  }
  return str;
}

function formatTime(val) {
  if (!val) return "09:00 AM";
  if (val instanceof Date) {
    return Utilities.formatDate(val, "Asia/Kolkata", "hh:mm a");
  }
  return val.toString();
}

function getDayName(dateStr) {
  try {
    var parts = dateStr.split("-");
    var d = new Date(parseInt(parts[2]), parseInt(parts[1]) - 1, parseInt(parts[0]));
    var days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
    return days[d.getDay()];
  } catch (e) {
    return "Weekday";
  }
}
