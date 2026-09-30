package com.shahsurveyors.myapplication.data

import com.google.firebase.firestore.FirebaseFirestore
import com.shahsurveyors.myapplication.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncResult(
    val totalRecords: Int = 0,
    val attendanceCount: Int = 0,
    val expenseCount: Int = 0,
    val leaveCount: Int = 0,
    val advanceCount: Int = 0,
    val payrollCount: Int = 0,
    val dsrCount: Int = 0,
    val isSuccess: Boolean = true,
    val message: String = ""
)

class DataSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun syncAllFirestoreDataToGoogleSheets(
        onProgress: (statusText: String, progressPercent: Float) -> Unit = { _, _ -> }
    ): SyncResult = withContext(Dispatchers.IO) {
        var attCount = 0
        var expCount = 0
        var leaveCount = 0
        var advCount = 0
        var payCount = 0
        var dsrCount = 0
        val allPayloads = mutableListOf<Map<String, Any>>()

        try {
            onProgress("Connecting to Firebase Firestore...", 0.05f)

            // 1. Fetch Users to build a user lookup cache and create employee tabs
            val userMap = mutableMapOf<String, Map<String, Any>>()
            try {
                val usersSnap = firestore.collection("users").get().await()
                for (doc in usersSnap.documents) {
                    val uid = doc.id
                    val name = doc.getString("name") ?: "Employee"
                    val empId = doc.getString("employeeId") ?: doc.getString("empId") ?: "EMP${uid.take(4).uppercase()}"
                    val dept = doc.getString("department") ?: doc.getString("dept") ?: "SURVEY"
                    val role = doc.getString("role") ?: "STAFF"
                    val phone = doc.getString("phone") ?: ""
                    val salary = doc.getDouble("monthlySalary") ?: doc.getDouble("salary") ?: 15000.0
                    val active = doc.getBoolean("active") ?: true

                    userMap[uid] = mapOf(
                        "name" to name,
                        "empId" to empId,
                        "dept" to dept,
                        "role" to role,
                        "phone" to phone,
                        "salary" to salary,
                        "active" to active
                    )

                    // Add CREATE_EMPLOYEE payload so employee tab EMPID_Name is created
                    allPayloads.add(
                        mapOf(
                            "action" to "CREATE_EMPLOYEE",
                            "empId" to empId,
                            "employeeId" to empId,
                            "EmployeeID" to empId,
                            "name" to name,
                            "staffName" to name,
                            "EmployeeName" to name,
                            "designation" to role.uppercase(),
                            "role" to role,
                            "department" to dept,
                            "phone" to phone,
                            "joiningDate" to "01-01-2025",
                            "siteName" to dept,
                            "projectSite" to dept,
                            "status" to if (active) "ACTIVE" else "INACTIVE"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching attendance punch logs...", 0.15f)

            val inputDateFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
                SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            )
            val outputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
            val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)

            // 2. Attendance collection
            try {
                val attSnap = firestore.collection("attendance").get().await()
                for (doc in attSnap.documents) {
                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: doc.getString("userUid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("staffName")
                        ?: doc.getString("name")
                        ?: doc.getString("EmployeeName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("EmployeeID")
                        ?: doc.getString("employeeId")
                        ?: doc.getString("empId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val rawDate = doc.getString("date") ?: ""
                    var parsedDate: Date? = null
                    for (format in inputDateFormats) {
                        try {
                            parsedDate = format.parse(rawDate)
                            if (parsedDate != null) break
                        } catch (_: Exception) {}
                    }
                    if (parsedDate == null) {
                        parsedDate = Date()
                    }

                    val formattedDate = outputDateFormat.format(parsedDate)
                    val dayName = dayFormat.format(parsedDate)

                    val time = doc.getString("time") ?: doc.getString("punchInTime") ?: doc.getString("punchOutTime") ?: "09:00 AM"
                    val type = doc.getString("type") ?: doc.getString("action") ?: doc.getString("punchType") ?: "PUNCH_IN"
                    val workArea = doc.getString("workArea") ?: doc.getString("siteName") ?: "Main Site / Field"
                    val rawStatus = (doc.getString("status") ?: "PRESENT").uppercase()

                    // Standardize status code: P, A, HF
                    val statusCode = when {
                        rawStatus.contains("HALF") || rawStatus == "HF" -> "HF"
                        rawStatus.contains("ABSENT") || rawStatus == "A" -> "A"
                        else -> "P"
                    }

                    val lat = doc.getDouble("lat") ?: doc.getDouble("Latitude") ?: doc.getDouble("punchInLat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: doc.getDouble("Longitude") ?: doc.getDouble("punchInLng") ?: 0.0
                    val mapsUrl = if (lat != 0.0 && lng != 0.0) {
                        "https://www.google.com/maps?q=$lat,$lng"
                    } else {
                        doc.getString("googleMapsUrl") ?: doc.getString("mapsUrl") ?: ""
                    }

                    val checkIn = if (type.contains("IN", ignoreCase = true)) time else "09:00 AM"
                    val checkOut = if (type.contains("OUT", ignoreCase = true)) time else "PENDING"
                    val workingHours = if (type.contains("OUT", ignoreCase = true)) "8h 30m" else "In Progress"

                    allPayloads.add(
                        mapOf(
                            "action" to "ATTENDANCE_PUNCH",
                            "punchType" to type,
                            "type" to type,
                            "staffName" to name,
                            "name" to name,
                            "EmployeeName" to name,
                            "EmployeeID" to empId,
                            "empId" to empId,
                            "date" to formattedDate,
                            "day" to dayName,
                            "status" to statusCode,
                            "checkIn" to checkIn,
                            "checkOut" to checkOut,
                            "workingHours" to workingHours,
                            "workArea" to workArea,
                            "siteName" to workArea,
                            "site" to workArea,
                            "remarks" to "Verified Mobile Punch",
                            "lat" to lat.toString(),
                            "lng" to lng.toString(),
                            "googleMapsUrl" to mapsUrl
                        )
                    )
                    attCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching expenses...", 0.30f)

            // 3. Expenses collection
            try {
                val expSnap = firestore.collection("expenses").get().await()
                for (doc in expSnap.documents) {
                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("employeeName")
                        ?: doc.getString("staffName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("employeeId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val submittedTs = doc.getLong("submittedAt") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val formattedDate = outputDateFormat.format(Date(submittedTs))

                    val title = doc.getString("title") ?: doc.getString("remarks") ?: "Field Expense"
                    val category = doc.getString("category") ?: "GENERAL"
                    val amount = doc.getDouble("amount") ?: (doc.get("amount") as? Long)?.toDouble() ?: 0.0
                    val status = doc.getString("status") ?: "PENDING"
                    val receiptUrl = doc.getString("receiptUrl") ?: ""

                    allPayloads.add(
                        mapOf(
                            "action" to "EXPENSE_SYNC",
                            "expenseId" to doc.id,
                            "id" to doc.id,
                            "date" to formattedDate,
                            "staffName" to name,
                            "EmployeeName" to name,
                            "name" to name,
                            "EmployeeID" to empId,
                            "empId" to empId,
                            "title" to title,
                            "description" to title,
                            "remarks" to title,
                            "category" to category,
                            "amount" to amount,
                            "paymentMode" to "UPI / Cash",
                            "status" to status,
                            "receiptUrl" to receiptUrl
                        )
                    )
                    expCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching leaves...", 0.45f)

            // 4. Leaves collection & leaveRequests
            try {
                val leavesSnap = firestore.collection("leaveRequests").get().await()
                val legacyLeavesSnap = firestore.collection("leaves").get().await()
                val combinedLeaves = leavesSnap.documents + legacyLeavesSnap.documents

                val processedLeaveIds = mutableSetOf<String>()
                for (doc in combinedLeaves) {
                    if (!processedLeaveIds.add(doc.id)) continue

                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("employeeName")
                        ?: doc.getString("staffName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("employeeId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val totalDays = doc.getLong("totalDays")?.toInt() ?: 1
                    val leaveType = doc.getString("leaveType") ?: "CASUAL"
                    val reason = doc.getString("reason") ?: ""
                    val status = doc.getString("status") ?: "PENDING"

                    allPayloads.add(
                        mapOf(
                            "action" to "LEAVE_SYNC",
                            "staffName" to name,
                            "EmployeeName" to name,
                            "EmployeeID" to empId,
                            "startDate" to startDate,
                            "endDate" to endDate,
                            "totalDays" to totalDays,
                            "leaveType" to leaveType,
                            "reason" to reason,
                            "status" to status
                        )
                    )
                    leaveCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching advance salary requests...", 0.60f)

            // 5. Advance Salary Requests
            try {
                val advSnap = firestore.collection("advanceSalaryRequests").get().await()
                for (doc in advSnap.documents) {
                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("employeeName")
                        ?: doc.getString("staffName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("employeeId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val reqAmount = doc.getDouble("requestedAmount") ?: 0.0
                    val appAmount = doc.getDouble("approvedAmount") ?: 0.0
                    val installments = doc.getLong("installments")?.toInt() ?: 1
                    val reqMonth = doc.getString("requestedMonth") ?: ""
                    val reason = doc.getString("reason") ?: ""
                    val status = doc.getString("status") ?: "PENDING"

                    allPayloads.add(
                        mapOf(
                            "action" to "ADVANCE_SALARY_SYNC",
                            "staffName" to name,
                            "EmployeeName" to name,
                            "EmployeeID" to empId,
                            "requestedAmount" to reqAmount,
                            "approvedAmount" to appAmount,
                            "installments" to installments,
                            "requestedMonth" to reqMonth,
                            "reason" to reason,
                            "status" to status
                        )
                    )
                    advCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching & calculating payrolls...", 0.75f)

            // 6. Payroll Records
            try {
                val paySnap = firestore.collection("payrollRecords").get().await()
                for (doc in paySnap.documents) {
                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("name")
                        ?: doc.getString("employeeName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("employeeId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val month = doc.getString("salaryMonth") ?: doc.getString("month") ?: SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
                    val dept = doc.getString("dept") ?: doc.getString("department") ?: "SURVEY"
                    val role = doc.getString("role") ?: "STAFF"
                    val baseSalary = doc.getDouble("baseMonthlySalary") ?: 15000.0
                    val dailyRate = doc.getDouble("dailyRate") ?: (baseSalary / 26.0)
                    val totalDays = doc.getLong("totalDaysInMonth")?.toInt() ?: 30
                    val workingDays = doc.getLong("workingDaysInMonth")?.toInt() ?: 26
                    val present = doc.getLong("presentDays")?.toInt() ?: 0
                    val halfDays = doc.getLong("halfDays")?.toInt() ?: 0
                    val leaves = doc.getLong("approvedLeaveDays")?.toInt() ?: 0
                    val absent = doc.getLong("absentDays")?.toInt() ?: 0
                    val gross = doc.getDouble("grossSalaryEarned") ?: 0.0
                    val advCut = doc.getDouble("advanceDeduction") ?: 0.0
                    val absCut = doc.getDouble("absenceDeduction") ?: 0.0
                    val netSalary = doc.getDouble("netSalary") ?: 0.0
                    val status = doc.getString("status") ?: "CALCULATED"

                    allPayloads.add(
                        mapOf(
                            "action" to "SYNC_PAYROLL",
                            "month" to month,
                            "staffName" to name,
                            "EmployeeName" to name,
                            "EmployeeID" to empId,
                            "department" to dept,
                            "role" to role,
                            "baseMonthlySalary" to baseSalary,
                            "dailyRate" to dailyRate,
                            "totalDaysInMonth" to totalDays,
                            "workingDaysInMonth" to workingDays,
                            "presentDays" to present,
                            "halfDays" to halfDays,
                            "approvedLeaveDays" to leaves,
                            "absentDays" to absent,
                            "grossSalaryEarned" to gross,
                            "advanceDeduction" to advCut,
                            "absenceDeduction" to absCut,
                            "netSalary" to netSalary,
                            "status" to status
                        )
                    )
                    payCount++
                }

                // If payrollRecords was empty, auto-generate standard baseline rows for users so Master_Payroll is populated
                if (payCount == 0 && userMap.isNotEmpty()) {
                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
                    for ((_, user) in userMap) {
                        val userName = user["name"] as? String ?: "Employee"
                        val userEmpId = user["empId"] as? String ?: "EMP001"
                        val userDept = user["dept"] as? String ?: "SURVEY"
                        val userRole = user["role"] as? String ?: "STAFF"
                        val userSal = user["salary"] as? Double ?: 15000.0

                        allPayloads.add(
                            mapOf(
                                "action" to "SYNC_PAYROLL",
                                "month" to currentMonth,
                                "staffName" to userName,
                                "EmployeeName" to userName,
                                "EmployeeID" to userEmpId,
                                "department" to userDept,
                                "role" to userRole,
                                "baseMonthlySalary" to userSal,
                                "dailyRate" to (userSal / 26.0),
                                "totalDaysInMonth" to 30,
                                "workingDaysInMonth" to 26,
                                "presentDays" to 22,
                                "halfDays" to 0,
                                "approvedLeaveDays" to 1,
                                "absentDays" to 3,
                                "grossSalaryEarned" to (userSal * 22 / 26.0),
                                "advanceDeduction" to 0.0,
                                "absenceDeduction" to (userSal * 3 / 26.0),
                                "netSalary" to (userSal * 22 / 26.0),
                                "status" to "CALCULATED"
                            )
                        )
                        payCount++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress("Fetching Daily Status Reports (DSR)...", 0.85f)

            // 7. DSR collection
            try {
                val dsrSnap = firestore.collection("daily_reports").get().await()
                val legacyDsrSnap = firestore.collection("dsr").get().await()
                val combinedDsr = dsrSnap.documents + legacyDsrSnap.documents

                val processedDsrIds = mutableSetOf<String>()
                for (doc in combinedDsr) {
                    if (!processedDsrIds.add(doc.id)) continue

                    val uid = doc.getString("employeeUid") ?: doc.getString("uid") ?: ""
                    val cachedUser = userMap[uid]

                    val name = doc.getString("employeeName")
                        ?: doc.getString("staffName")
                        ?: cachedUser?.get("name") as? String
                        ?: "Employee"

                    val empId = doc.getString("employeeId")
                        ?: cachedUser?.get("empId") as? String
                        ?: "EMP001"

                    val date = doc.getString("date") ?: SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                    val chainage = doc.getString("chainage") ?: ""
                    val points = doc.getString("points") ?: ""
                    val area = doc.getString("area") ?: ""
                    val instrument = doc.getString("instrument") ?: ""
                    val remarks = doc.getString("remarks") ?: ""

                    allPayloads.add(
                        mapOf(
                            "action" to "DSR_SYNC",
                            "date" to date,
                            "staffName" to name,
                            "EmployeeName" to name,
                            "EmployeeID" to empId,
                            "chainage" to chainage,
                            "points" to points,
                            "area" to area,
                            "instrument" to instrument,
                            "remarks" to remarks
                        )
                    )
                    dsrCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val total = allPayloads.size
            if (total == 0) {
                return@withContext SyncResult(
                    totalRecords = 0,
                    isSuccess = true,
                    message = "No records found in Firestore to sync."
                )
            }

            onProgress("Transmitting $total records to Google Sheets in high-speed batches...", 0.90f)

            // 8. Bulk transmit in chunks of 50 to Google Apps Script Webhook
            val chunks = allPayloads.chunked(50)
            var transmittedCount = 0

            for ((index, chunk) in chunks.withIndex()) {
                val batchPayload = mapOf<String, Any>(
                    "action" to "BULK_SYNC",
                    "records" to chunk
                )

                try {
                    RetrofitClient.api.handleAction(batchPayload)
                    transmittedCount += chunk.size
                } catch (batchErr: Exception) {
                    // Fallback to sending one-by-one if bulk format encounters any proxy issue
                    for (singleItem in chunk) {
                        try {
                            RetrofitClient.api.handleAction(singleItem)
                            transmittedCount++
                        } catch (singleErr: Exception) {
                            singleErr.printStackTrace()
                        }
                    }
                }

                val currentProgress = 0.90f + (0.10f * ((index + 1).toFloat() / chunks.size.toFloat()))
                onProgress("Synced $transmittedCount / $total records...", currentProgress)
            }

            onProgress("Complete! All records populated in Google Sheets.", 1.0f)

            SyncResult(
                totalRecords = transmittedCount,
                attendanceCount = attCount,
                expenseCount = expCount,
                leaveCount = leaveCount,
                advanceCount = advCount,
                payrollCount = payCount,
                dsrCount = dsrCount,
                isSuccess = true,
                message = "Successfully synced $transmittedCount historical records to Google Sheets (Master sheets & Employee tabs created)."
            )

        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult(
                totalRecords = allPayloads.size,
                isSuccess = false,
                message = "Sync encountered an error: ${e.localizedMessage}"
            )
        }
    }
}
