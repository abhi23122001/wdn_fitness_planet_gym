package com.shahsurveyors.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.shahsurveyors.myapplication.models.*
import com.shahsurveyors.myapplication.utils.PayrollCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalaryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // ========================================================
    // SALARY PROFILES (HISTORICAL & EFFECTIVE DATED)
    // ========================================================

    suspend fun saveSalaryProfile(profile: SalaryProfileModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("salaryProfiles")
            val currentUid = auth.currentUser?.uid ?: ""

            // 1. Find existing active profiles for this employee
            val existingActiveDocs = collection
                .whereEqualTo("employeeUid", profile.employeeUid)
                .whereEqualTo("active", true)
                .get()
                .await()

            val batch = firestore.batch()

            // Close existing active periods
            for (doc in existingActiveDocs.documents) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "active" to false,
                        "effectiveTo" to profile.effectiveFrom
                    )
                )
            }

            // 2. Create new profile document
            val newDocRef = if (profile.id.isNotBlank()) {
                collection.document(profile.id)
            } else {
                collection.document()
            }

            val data = hashMapOf(
                "id" to newDocRef.id,
                "uid" to profile.employeeUid,
                "employeeUid" to profile.employeeUid,
                "employeeName" to profile.employeeName,
                "employeeId" to profile.employeeId,
                "department" to profile.department,
                "payType" to profile.payType,
                "monthlySalary" to profile.monthlySalary,
                "dailyRate" to profile.dailyRate,
                "overtimeRatePerHour" to profile.overtimeRatePerHour,
                "effectiveFrom" to profile.effectiveFrom,
                "effectiveTo" to profile.effectiveTo,
                "note" to profile.note,
                "setByUid" to currentUid,
                "setByName" to profile.setByName,
                "setAt" to System.currentTimeMillis(),
                "active" to true
            )

            batch.set(newDocRef, data)
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSalaryProfilesForEmployee(employeeUid: String): List<SalaryProfileModel> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("salaryProfiles")
                    .whereEqualTo("employeeUid", employeeUid)
                    .get()
                    .await()
                snapshot.toObjects(SalaryProfileModel::class.java).sortedByDescending { it.effectiveFrom }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getAllSalaryProfiles(): List<SalaryProfileModel> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("salaryProfiles")
                .get()
                .await()
            snapshot.toObjects(SalaryProfileModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ========================================================
    // ADVANCE SALARY REQUESTS
    // ========================================================

    suspend fun submitAdvanceSalaryRequest(request: AdvanceSalaryRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUid = auth.currentUser?.uid ?: request.employeeUid
            val docRef = if (request.id.isNotBlank()) {
                firestore.collection("advanceSalaryRequests").document(request.id)
            } else {
                firestore.collection("advanceSalaryRequests").document()
            }

            val data = hashMapOf(
                "id" to docRef.id,
                "uid" to currentUid,
                "userUid" to currentUid,
                "employeeUid" to currentUid,
                "employeeName" to request.employeeName,
                "employeeId" to request.employeeId,
                "department" to request.department,
                "requestedAmount" to request.requestedAmount,
                "approvedAmount" to 0.0,
                "installments" to request.installments,
                "requestedMonth" to request.requestedMonth,
                "reason" to request.reason,
                "status" to "PENDING",
                "appliedAt" to System.currentTimeMillis()
            )

            docRef.set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAdvanceRequestsForEmployee(employeeUid: String): List<AdvanceSalaryRequest> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("advanceSalaryRequests")
                    .whereEqualTo("employeeUid", employeeUid)
                    .get()
                    .await()
                snapshot.toObjects(AdvanceSalaryRequest::class.java).sortedByDescending { it.appliedAt }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun getAllAdvanceRequests(): List<AdvanceSalaryRequest> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("advanceSalaryRequests")
                .get()
                .await()
            snapshot.toObjects(AdvanceSalaryRequest::class.java).sortedByDescending { it.appliedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun decideAdvanceRequest(
        requestId: String,
        status: String,
        approvedAmount: Double,
        installments: Int,
        adminUid: String,
        adminName: String,
        note: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "approvedAmount" to if (status == "APPROVED") approvedAmount else 0.0,
                "installments" to if (status == "APPROVED") installments else 1,
                "decidedAt" to System.currentTimeMillis(),
                "decidedByUid" to adminUid,
                "decidedByName" to adminName,
                "note" to note
            )

            firestore.collection("advanceSalaryRequests")
                .document(requestId)
                .update(updates)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ========================================================
    // PAYROLL RECORDS
    // ========================================================

    suspend fun savePayrollRecord(record: PayrollRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("payrollRecords")
                .document(record.id)
                .set(record, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPayrollRecordsForMonth(yearMonth: String): List<PayrollRecord> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("payrollRecords")
                .whereEqualTo("salaryMonth", yearMonth)
                .get()
                .await()
            snapshot.toObjects(PayrollRecord::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPayrollRecordForEmployee(employeeUid: String, yearMonth: String): PayrollRecord? =
        withContext(Dispatchers.IO) {
            try {
                val doc = firestore.collection("payrollRecords")
                    .document("${employeeUid}_${yearMonth}")
                    .get()
                    .await()

                if (doc.exists()) {
                    doc.toObject(PayrollRecord::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

    // ========================================================
    // 360° EMPLOYEE MONTHLY SUMMARY & DETAILS
    // ========================================================

    suspend fun getEmployee360Report(employeeUid: String, yearMonth: String): Employee360Report =
        withContext(Dispatchers.IO) {
            try {
                // 1. User document
                val userDoc = firestore.collection("users").document(employeeUid).get().await()
                val name = userDoc.getString("name") ?: "Staff User"
                val empId = userDoc.getString("employeeId") ?: userDoc.getString("id") ?: employeeUid.take(6).uppercase()
                val dept = userDoc.getString("department") ?: userDoc.getString("dept") ?: "SURVEY"
                val role = userDoc.getString("role") ?: "STAFF"
                val phone = userDoc.getString("phone") ?: ""
                val email = userDoc.getString("email") ?: ""
                val photoUrl = userDoc.getString("photoUrl") ?: userDoc.getString("dpUrl") ?: ""
                val active = userDoc.getBoolean("active") ?: true
                val monthlySalaryFallback = (userDoc.get("monthlySalary") ?: userDoc.get("salary"))?.toString()?.toDoubleOrNull() ?: 15000.0

                // 2. Attendance punches for this user in this month (in-memory safe filtering)
                val attSnapshot = firestore.collection("attendance").get().await()
                val userAttDocs = attSnapshot.documents.filter { doc ->
                    val docUid = doc.getString("employeeUid") ?: doc.getString("uid") ?: doc.getString("userUid") ?: ""
                    val docName = doc.getString("staffName") ?: doc.getString("name") ?: doc.getString("EmployeeName") ?: ""
                    val docDate = doc.getString("date") ?: ""
                    (docUid == employeeUid || docName.equals(name, ignoreCase = true)) && docDate.startsWith(yearMonth)
                }

                // Group by date to calculate Full Days vs Half Days
                val punchesByDate = userAttDocs.groupBy { it.getString("date") ?: "" }

                var presentCount = 0
                var halfDayCount = 0
                val dailyLogs = mutableListOf<DailyPunchLog>()

                for ((dateKey, punches) in punchesByDate) {
                    if (dateKey.isBlank()) continue

                    val inPunch = punches.find {
                        val t = it.getString("type") ?: it.getString("action") ?: it.getString("punchType") ?: ""
                        t.contains("IN", ignoreCase = true)
                    }
                    val outPunch = punches.find {
                        val t = it.getString("type") ?: it.getString("action") ?: it.getString("punchType") ?: ""
                        t.contains("OUT", ignoreCase = true)
                    }

                    val inTime = inPunch?.getString("time") ?: inPunch?.getString("punchInTime") ?: ""
                    val outTime = outPunch?.getString("time") ?: outPunch?.getString("punchOutTime") ?: ""
                    val lat = inPunch?.getDouble("lat") ?: inPunch?.getDouble("Latitude") ?: inPunch?.getDouble("punchInLat") ?: 0.0
                    val lng = inPunch?.getDouble("lng") ?: inPunch?.getDouble("Longitude") ?: inPunch?.getDouble("punchInLng") ?: 0.0
                    val outLat = outPunch?.getDouble("lat") ?: 0.0
                    val outLng = outPunch?.getDouble("lng") ?: 0.0
                    val workArea = inPunch?.getString("workArea") ?: inPunch?.getString("siteName") ?: outPunch?.getString("workArea") ?: "Main Office / Site"

                    val isFullDay = inPunch != null && outPunch != null
                    val statusStr = if (isFullDay) "PRESENT" else "HALF_DAY"

                    if (isFullDay) {
                        presentCount++
                    } else {
                        halfDayCount++
                    }

                    val mapsUrl = if (lat != 0.0 && lng != 0.0) "https://www.google.com/maps?q=$lat,$lng" else (inPunch?.getString("googleMapsUrl") ?: inPunch?.getString("mapsUrl") ?: "")

                    dailyLogs.add(
                        DailyPunchLog(
                            id = dateKey,
                            date = dateKey,
                            punchInTime = inTime,
                            punchOutTime = outTime,
                            punchInLat = lat,
                            punchInLng = lng,
                            punchOutLat = outLat,
                            punchOutLng = outLng,
                            workArea = workArea,
                            dayStatus = statusStr,
                            googleMapsUrl = mapsUrl,
                            totalHours = if (isFullDay) "8.5 hrs" else "Single Punch"
                        )
                    )
                }

                dailyLogs.sortByDescending { it.date }

                // 3. Approved Leaves in this month (Query both leaveRequests and leaves)
                val leavesSnapshot = firestore.collection("leaveRequests").get().await()
                val legacyLeavesSnapshot = firestore.collection("leaves").get().await()
                val allLeavesList = (leavesSnapshot.toObjects(LeaveRequest::class.java) + legacyLeavesSnapshot.toObjects(LeaveRequest::class.java))
                    .distinctBy { it.id.ifBlank { "${it.employeeUid}_${it.startDate}" } }

                val userLeaves = allLeavesList.filter { it.employeeUid == employeeUid }
                val approvedLeaves = userLeaves.filter {
                    it.status.equals("APPROVED", ignoreCase = true) && (it.startDate.startsWith(yearMonth) || it.endDate.startsWith(yearMonth))
                }
                val leaveDaysCount = approvedLeaves.sumOf { it.totalDays }

                // 4. Advances
                val advances = getAdvanceRequestsForEmployee(employeeUid)
                val approvedAdvances = advances.filter { it.status.equals("APPROVED", ignoreCase = true) }
                val totalAdvApproved = approvedAdvances.sumOf { it.approvedAmount }
                val advMonthlyDeduction = PayrollCalculator.calculateAdvanceDeductionForMonth(approvedAdvances, yearMonth)

                // 5. Expenses
                val expSnapshot = firestore.collection("expenses")
                    .whereEqualTo("employeeUid", employeeUid)
                    .get()
                    .await()
                var totalClaimed = 0.0
                var totalApproved = 0.0
                for (doc in expSnapshot.documents) {
                    val amt = doc.getDouble("amount") ?: 0.0
                    val st = doc.getString("status") ?: "PENDING"
                    totalClaimed += amt
                    if (st.equals("APPROVED", ignoreCase = true)) {
                        totalApproved += amt
                    }
                }

                // 6. Calculate Payroll
                val salaryProfiles = getSalaryProfilesForEmployee(employeeUid)
                val payroll = PayrollCalculator.calculateMonthlyPayroll(
                    employeeUid = employeeUid,
                    employeeName = name,
                    employeeId = empId,
                    department = dept,
                    role = role,
                    yearMonth = yearMonth,
                    salaryProfiles = salaryProfiles,
                    fallbackMonthlySalary = monthlySalaryFallback,
                    presentDays = presentCount,
                    halfDays = halfDayCount,
                    approvedLeaveDays = leaveDaysCount,
                    approvedAdvances = approvedAdvances
                )

                Employee360Report(
                    employeeUid = employeeUid,
                    name = name,
                    employeeId = empId,
                    department = dept,
                    role = role,
                    phone = phone,
                    email = email,
                    photoUrl = photoUrl,
                    active = active,
                    month = yearMonth,
                    presentDaysCount = presentCount,
                    halfDaysCount = halfDayCount,
                    absentDaysCount = payroll.absentDays,
                    approvedLeaveDaysCount = leaveDaysCount,
                    totalWorkingDays = payroll.workingDaysInMonth,
                    dailyPunchLogs = dailyLogs,
                    leaveRequests = userLeaves,
                    totalAdvanceApproved = totalAdvApproved,
                    advanceMonthlyDeduction = advMonthlyDeduction,
                    advanceRemainingBalance = maxOf(0.0, totalAdvApproved - advMonthlyDeduction),
                    advanceRequests = advances,
                    totalExpensesClaimed = totalClaimed,
                    totalExpensesApproved = totalApproved,
                    payroll = payroll
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Employee360Report(
                    employeeUid = employeeUid,
                    month = yearMonth
                )
            }
        }
}
