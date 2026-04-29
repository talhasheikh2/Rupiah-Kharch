package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "", // Added to keep goals private to each user
    val title: String,
    val targetAmount: Double,
    val iconRes: Int,
    val colorHex: String,
    val deductionAmount: Double,
    val frequency: String,
    val startDate: Long = System.currentTimeMillis(),
    var savedAmount: Double = 0.0,
    var lastDeductionDate: Long = 0L,
    val minBalanceSafety: Double = 10000.0,
    var isPaused: Boolean = false,
    var wasSkippedLowBalance: Boolean = false,
    var lastNotificationDate: Long = 0L
) {

    fun isDeductionDue(): Boolean {
        val currentTime = System.currentTimeMillis()

        // 1. Check if the goal is already finished
        if (savedAmount >= targetAmount) {
            return false
        }

        // 2. If it has NEVER had a deduction, it's due now.
        if (lastDeductionDate == 0L) {
            return true
        }

        val diffInMillis = currentTime - lastDeductionDate

        // 3. Proper time checks for each frequency
        return when (frequency.lowercase()) {
            "daily" -> diffInMillis >= TimeUnit.DAYS.toMillis(1)
            "weekly" -> diffInMillis >= TimeUnit.DAYS.toMillis(7)
            "monthly" -> diffInMillis >= TimeUnit.DAYS.toMillis(30)
            else -> false
        }
    }

    fun getRemaining(): Double = (targetAmount - savedAmount).coerceAtLeast(0.0)

    fun getProgress(): Int {
        if (targetAmount <= 0) return 0
        return ((savedAmount / targetAmount) * 100).toInt().coerceIn(0, 100)
    }
}