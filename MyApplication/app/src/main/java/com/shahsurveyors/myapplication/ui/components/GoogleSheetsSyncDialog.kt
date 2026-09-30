package com.shahsurveyors.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahsurveyors.myapplication.data.SyncResult
import com.shahsurveyors.myapplication.ui.theme.*

@Composable
fun GoogleSheetsSyncDialog(
    isSyncing: Boolean,
    statusText: String,
    progressPercent: Float,
    syncResult: SyncResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSyncing) {
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = ShahGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Sheets Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ShahBlack
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (isSyncing) {
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        color = ShahBlack,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = ShahGreen,
                        trackColor = ShahLightGrey
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = ShahMediumGrey,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )
                } else if (syncResult != null) {
                    if (syncResult.isSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync Completed Successfully!",
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ShahLightGrey.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                SyncStatLine("📍 Attendance Logs Synced:", "${syncResult.attendanceCount}")
                                SyncStatLine("💳 Payroll Records Synced:", "${syncResult.payrollCount}")
                                SyncStatLine("💰 Expense Claims Synced:", "${syncResult.expenseCount}")
                                SyncStatLine("🏖️ Leave Requests Synced:", "${syncResult.leaveCount}")
                                SyncStatLine("💵 Salary Advances Synced:", "${syncResult.advanceCount}")
                                SyncStatLine("📋 DSR Reports Synced:", "${syncResult.dsrCount}")
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = ShahMediumGrey.copy(alpha = 0.3f)
                                )
                                SyncStatLine("📊 Total Rows Transmitted:", "${syncResult.totalRecords}", isBold = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "All Master Sheets & individual employee dedicated tabs have been created and updated in your Google Spreadsheet.",
                            fontSize = 11.sp,
                            color = ShahMediumGrey
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync Notice",
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = syncResult.message,
                            fontSize = 12.sp,
                            color = ShahBlack
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isSyncing) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ShahGreen)
                ) {
                    Text("OK / DONE", color = ShahWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun SyncStatLine(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isBold) ShahBlack else ShahMediumGrey,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = if (isBold) ShahGreen else ShahBlack,
            fontWeight = FontWeight.Bold
        )
    }
}
