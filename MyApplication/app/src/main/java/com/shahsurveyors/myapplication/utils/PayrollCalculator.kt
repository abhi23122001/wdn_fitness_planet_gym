package com.shahsurveyors.myapplication.utils

import com.shahsurveyors.myapplication.models.AdvanceSalaryRequest
import com.shahsurveyors.myapplication.models.PayrollRecord
import com.shahsurveyors.myapplication.models.SalaryProfileModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object PayrollCalculator {

    /**
     * Standard working days in an Indian ERP operational month.
     */
    const val STANDARD_WORKING_DAYS = 26

    /**
     * Calculates full PayrollRecord for a given employee and month.
     */
    fun calculateMonthlyPayroll(
        employeeUid: String,
        employeeName: String,
        employeeId: String,
        department: String,
        role: String,
        yearMonth: String, // "YYYY-MM"
        salaryProfiles: List<SalaryProfileModel>,
        fallbackMonthlySalary: Double = 15000.0,
        presentDays: Int = 0,
        halfDays: Int = 0,
        approvedLeaveDays: Int = 0,
        overtimeHours: Double = 0.0,
        approvedAdvances: List<AdvanceSalaryRequest> = emptyList(),
        otherDeductions: Double = 0.0
    ): PayrollRecord {

        val profile = findApplicableSalaryProfile(salaryProfiles, yearMonth)

        val totalDaysInMonth = getDaysInMonth(yearMonth)
        val workingDaysInMonth = STANDARD_WORKING_DAYS

        // Base rates
        val monthlySalary = profile?.monthlySalary ?: if (fallbackMonthlySalary > 0) fallbackMonthlySalary else 15000.0
        val dailyRate = if (profile != null && profile.dailyRate > 0) {
            profile.dailyRate
        } else if (monthlySalary > 0) {
            (monthlySalary / workingDaysInMonth * 100.0).roundToInt() / 100.0
        } else {
            0.0
        }

        val overtimeRate = if (profile != null && profile.overtimeRatePerHour > 0) {
            profile.overtimeRatePerHour
        } else if (dailyRate > 0) {
            ((dailyRate / 8.0) * 1.5 * 100.0).roundToInt() / 100.0
        } else {
            0.0
        }

        // Calculate Effective Period Proration
        val (effectiveBaseSalary, effectivePeriodText) = if (profile != null) {
            calculateEffectiveBaseSalary(profile, yearMonth, totalDaysInMonth)
        } else {
            Pair(monthlySalary, "Standard Rate")
        }

        // Attendance & Absence (Smart Calculation for Current vs Past Months)
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
        val isCurrentMonth = (yearMonth == currentYearMonth)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // For current ongoing month, only count passed working days to prevent unfair future absence deductions
        val elapsedDaysInMonth = if (isCurrentMonth) minOf(totalDaysInMonth, currentDay) else totalDaysInMonth
        val elapsedWorkingDays = if (isCurrentMonth) {
            maxOf(1, (elapsedDaysInMonth * (STANDARD_WORKING_DAYS.toDouble() / totalDaysInMonth)).roundToInt())
        } else {
            workingDaysInMonth
        }

        val effectivePresent = presentDays + (halfDays * 0.5)
        val nonAbsenceDays = effectivePresent + approvedLeaveDays
        val absentDays = if (isCurrentMonth) {
            maxOf(0, (elapsedWorkingDays - nonAbsenceDays).roundToInt())
        } else {
            maxOf(0, (workingDaysInMonth - nonAbsenceDays).roundToInt())
        }

        val absenceDeduction = ((absentDays * dailyRate) * 100.0).roundToInt() / 100.0

        // Overtime Earnings
        val overtimePay = ((overtimeHours * overtimeRate) * 100.0).roundToInt() / 100.0

        // Gross Salary Earned (Based on days worked + leaves)
        val earnedDays = if (nonAbsenceDays > 0) nonAbsenceDays else if (isCurrentMonth) maxOf(1.0, elapsedWorkingDays.toDouble() - absentDays) else 0.0
        val grossSalaryEarned = maxOf(0.0, ((earnedDays * dailyRate + overtimePay) * 100.0).roundToInt() / 100.0)

        // Advance Salary Deductions calculation
        val advanceDeduction = calculateAdvanceDeductionForMonth(
            approvedAdvances = approvedAdvances,
            targetYearMonth = yearMonth
        )

        val totalDeductions = ((absenceDeduction + advanceDeduction + otherDeductions) * 100.0).roundToInt() / 100.0
        val netSalary = maxOf(
            0.0,
            ((effectiveBaseSalary + overtimePay - totalDeductions) * 100.0).roundToInt() / 100.0
        )

        val monthName = getMonthName(yearMonth)
        val year = getYear(yearMonth)

        return PayrollRecord(
            id = "${employeeUid}_${yearMonth}",
            employeeUid = employeeUid,
            name = employeeName,
            employeeId = employeeId,
            dept = department,
            role = role,
            salaryMonth = yearMonth,
            year = year,
            monthName = monthName,
            baseMonthlySalary = monthlySalary,
            dailyRate = dailyRate,
            overtimeRatePerHour = overtimeRate,
            effectiveSalaryPeriod = effectivePeriodText,
            totalDaysInMonth = totalDaysInMonth,
            workingDaysInMonth = workingDaysInMonth,
            presentDays = presentDays,
            halfDays = halfDays,
            effectivePresentDays = effectivePresent,
            approvedLeaveDays = approvedLeaveDays,
            absentDays = absentDays,
            overtimeHours = overtimeHours,
            grossSalaryEarned = grossSalaryEarned,
            absenceDeduction = absenceDeduction,
            overtimePay = overtimePay,
            advanceDeduction = advanceDeduction,
            otherDeductions = otherDeductions,
            totalDeductions = totalDeductions,
            netSalary = netSalary,
            status = "CALCULATED"
        )
    }

    /**
     * Safe Month length calculation using Calendar.
     */
    fun getDaysInMonth(yearMonth: String): Int {
        return try {
            val parts = yearMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            30
        }
    }

    /**
     * Safe Month name formatter using SimpleDateFormat.
     */
    fun getMonthName(yearMonth: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
            val date = sdf.parse(yearMonth)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(date)
            } else {
                yearMonth
            }
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun getYear(yearMonth: String): Int {
        return try {
            yearMonth.split("-")[0].toInt()
        } catch (e: Exception) {
            Calendar.getInstance().get(Calendar.YEAR)
        }
    }

    /**
     * Finds the salary profile applicable for the selected YearMonth (YYYY-MM).
     */
    fun findApplicableSalaryProfile(
        profiles: List<SalaryProfileModel>,
        yearMonth: String
    ): SalaryProfileModel? {
        if (profiles.isEmpty()) return null

        val monthStart = "${yearMonth}-01"
        val monthEnd = "${yearMonth}-31"

        // 1. Find profile explicitly matching effective period
        val matchingProfile = profiles
            .filter { profile ->
                val fromOk = profile.effectiveFrom.isBlank() || profile.effectiveFrom <= monthEnd
                val toOk = profile.effectiveTo.isNullOrBlank() || profile.effectiveTo >= monthStart
                fromOk && toOk
            }
            .maxByOrNull { it.effectiveFrom }

        if (matchingProfile != null) {
            return matchingProfile
        }

        // 2. Fallback to active profile if starting date is <= selected month
        return profiles.find { it.active && (it.effectiveFrom.isBlank() || it.effectiveFrom <= monthEnd) }
            ?: profiles.maxByOrNull { it.effectiveFrom }
    }

    /**
     * Calculate proration if salary started mid-month.
     */
    private fun calculateEffectiveBaseSalary(
        profile: SalaryProfileModel,
        yearMonth: String,
        totalDaysInMonth: Int
    ): Pair<Double, String> {
        val baseMonthly = profile.monthlySalary
        if (profile.effectiveFrom.isBlank()) {
            return Pair(baseMonthly, "Full Month")
        }

        val effectiveMonth = if (profile.effectiveFrom.length >= 7) {
            profile.effectiveFrom.substring(0, 7)
        } else {
            ""
        }

        if (effectiveMonth == yearMonth && profile.effectiveFrom.length >= 10) {
            val startDay = profile.effectiveFrom.substring(8, 10).toIntOrNull() ?: 1
            if (startDay > 1) {
                val activeDays = totalDaysInMonth - startDay + 1
                val proratedSalary = ((baseMonthly * activeDays / totalDaysInMonth) * 100.0).roundToInt() / 100.0
                return Pair(proratedSalary, "From ${profile.effectiveFrom} ($activeDays/$totalDaysInMonth days)")
            }
        }

        return Pair(baseMonthly, "From ${profile.effectiveFrom}")
    }

    /**
     * Calculates the total advance installment deduction for the selected target month.
     */
    fun calculateAdvanceDeductionForMonth(
        approvedAdvances: List<AdvanceSalaryRequest>,
        targetYearMonth: String // YYYY-MM
    ): Double {
        var totalDeduction = 0.0

        val targetParts = targetYearMonth.split("-")
        if (targetParts.size < 2) return 0.0
        val targetYear = targetParts[0].toIntOrNull() ?: return 0.0
        val targetMonth = targetParts[1].toIntOrNull() ?: return 0.0

        for (advance in approvedAdvances) {
            if (advance.status != "APPROVED") continue
            if (advance.approvedAmount <= 0 || advance.installments <= 0) continue

            val reqParts = advance.requestedMonth.split("-")
            val reqYear = if (reqParts.size >= 2) reqParts[0].toIntOrNull() ?: targetYear else targetYear
            val reqMonth = if (reqParts.size >= 2) reqParts[1].toIntOrNull() ?: targetMonth else targetMonth

            val monthlyEmi = (advance.approvedAmount / advance.installments * 100.0).roundToInt() / 100.0

            val monthsDiff = (targetYear - reqYear) * 12 + (targetMonth - reqMonth)

            if (monthsDiff in 0 until advance.installments) {
                totalDeduction += monthlyEmi
            }
        }

        return (totalDeduction * 100.0).roundToInt() / 100.0
    }
}
