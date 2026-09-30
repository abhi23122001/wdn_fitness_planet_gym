package com.shahsurveyors.myapplication.ui.ops

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.shahsurveyors.myapplication.data.NotificationRepository
import com.shahsurveyors.myapplication.models.AppNotification
import com.shahsurveyors.myapplication.network.RetrofitClient
import com.shahsurveyors.myapplication.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseItem(
    val id: String = "",
    val employeeUid: String = "",
    val employeeName: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val remarks: String = "",
    val receiptUrl: String = "",
    val status: String = "PENDING",
    val submittedAt: Long = System.currentTimeMillis(),
    val dateStr: String = ""
)

class ExpenseViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    val expensesList = mutableStateListOf<ExpenseItem>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    init {
        loadMyExpenses()
    }

    fun loadMyExpenses() {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("expenses")
                    .whereEqualTo("employeeUid", currentUid)
                    .get()
                    .await()

                val items = snapshot.documents.mapNotNull { doc ->
                    val amount = doc.getDouble("amount") ?: (doc.get("amount") as? Long)?.toDouble() ?: 0.0
                    val category = doc.getString("category") ?: "Other"
                    val remarks = doc.getString("remarks") ?: ""
                    val status = doc.getString("status") ?: "PENDING"
                    val ts = doc.getLong("submittedAt") ?: System.currentTimeMillis()
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                    val dateStr = sdf.format(Date(ts))
                    ExpenseItem(
                        id = doc.id,
                        employeeUid = currentUid,
                        employeeName = doc.getString("employeeName") ?: "",
                        amount = amount,
                        category = category,
                        remarks = remarks,
                        receiptUrl = doc.getString("receiptUrl") ?: "",
                        status = status,
                        submittedAt = ts,
                        dateStr = dateStr
                    )
                }.sortedByDescending { it.submittedAt }

                withContext(Dispatchers.Main) {
                    expensesList.clear()
                    expensesList.addAll(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submitClaim(
        amount: String,
        category: String,
        remarks: String,
        receiptBitmap: Bitmap?
    ) {
        if (amount.isBlank()) {
            statusMessage = "Please enter amount"
            return
        }

        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            statusMessage = "Please enter a valid amount"
            return
        }

        if (remarks.isBlank()) {
            statusMessage = "Please enter remarks"
            return
        }

        viewModelScope.launch {
            isLoading = true

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    statusMessage = "Please login first"
                    return@launch
                }

                val uid = currentUser.uid

                // 1. PROCESS RECEIPT
                statusMessage = "Processing receipt..."
                var receiptUrl = ""

                if (receiptBitmap != null) {
                    val compressedBytes = withContext(Dispatchers.Default) {
                        BitmapUtils.compressBitmap(
                            receiptBitmap,
                            maxDimension = 1024,
                            targetSizeKb = 500,
                            quality = 80
                        )
                    }

                    // FIREBASE STORAGE
                    statusMessage = "Uploading receipt..."
                    val fileName = "receipt_${UUID.randomUUID()}.jpg"
                    val storageReference = storage.reference
                        .child("expense_receipts")
                        .child(uid)
                        .child(fileName)

                    storageReference.putBytes(compressedBytes).await()
                    receiptUrl = storageReference.downloadUrl.await().toString()
                }

                // 2. FIRESTORE DOCUMENT
                statusMessage = "Saving expense..."
                val expenseDoc = firestore.collection("expenses").document()
                val empName = currentUser.displayName?.ifBlank { null } ?: "Staff User"
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                val dateStr = sdf.format(Date(currentTime))

                val expenseData = hashMapOf(
                    "id" to expenseDoc.id,
                    "uid" to uid,
                    "userUid" to uid,
                    "employeeUid" to uid,
                    "employeeId" to uid,
                    "employeeName" to empName,
                    "employeeEmail" to (currentUser.email ?: ""),
                    "amount" to amountValue,
                    "category" to category,
                    "remarks" to remarks,
                    "receiptUrl" to receiptUrl,
                    "status" to "PENDING",
                    "approvedBy" to "",
                    "approvedAt" to null,
                    "submittedAt" to currentTime,
                    "createdAt" to currentTime,
                    "updatedAt" to currentTime
                )

                expenseDoc.set(expenseData).await()

                // Add to local list immediately
                expensesList.add(
                    0,
                    ExpenseItem(
                        id = expenseDoc.id,
                        employeeUid = uid,
                        employeeName = empName,
                        amount = amountValue,
                        category = category,
                        remarks = remarks,
                        receiptUrl = receiptUrl,
                        status = "PENDING",
                        submittedAt = currentTime,
                        dateStr = dateStr
                    )
                )

                // 3. SEND ALERT NOTIFICATION TO ADMIN
                try {
                    notificationRepository.sendNotification(
                        AppNotification(
                            title = "💰 New Expense Claim: ₹ ${amountValue.toInt()}",
                            message = "$empName submitted a $category claim for '$remarks'",
                            type = "EXPENSE"
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. INSTANT GOOGLE SHEETS SYNC (NON-BLOCKING)
                withContext(Dispatchers.IO) {
                    try {
                        val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date(currentTime))
                        val empId = "EMP${uid.take(4).uppercase()}"
                        val payload = mapOf<String, Any>(
                            "action" to "EXPENSE_SYNC",
                            "expenseId" to expenseDoc.id,
                            "id" to expenseDoc.id,
                            "date" to formattedDate,
                            "staffName" to empName,
                            "EmployeeName" to empName,
                            "name" to empName,
                            "EmployeeID" to empId,
                            "empId" to empId,
                            "category" to category,
                            "title" to remarks,
                            "description" to remarks,
                            "remarks" to remarks,
                            "amount" to amountValue,
                            "paymentMode" to "UPI / Cash",
                            "status" to "PENDING",
                            "receiptUrl" to receiptUrl
                        )
                        RetrofitClient.api.handleAction(payload)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                statusMessage = "Expense submitted successfully"

            } catch (e: Exception) {
                e.printStackTrace()
                statusMessage = "Submission failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearStatus() {
        statusMessage = null
    }
}