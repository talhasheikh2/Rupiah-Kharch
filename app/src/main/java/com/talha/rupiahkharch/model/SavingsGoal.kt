package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val iconRes: Int,
    val colorHex: String,
    val deductionAmount: Double,
    val frequency: String, // Matches your Activity setup
    val startDate: Long = System.currentTimeMillis(),
    var savedAmount: Double = 0.0,
    var lastDeductionDate: Long = 0L
) {

    /**
     * LOGIC: Checks if it's time to deduct money.
     * Fixed to handle the very first deduction (lastDeductionDate == 0).
     */
    fun isDeductionDue(): Boolean {
        val currentTime = System.currentTimeMillis()

        // THE FIX: If this goal has NEVER had a deduction, it's due NOW.
        if (lastDeductionDate == 0L) {
            return true
        }

        val diffInMillis = currentTime - lastDeductionDate
        return when (frequency.lowercase()) {
            "daily" -> diffInMillis >= TimeUnit.DAYS.toMillis(1)
            "weekly" -> diffInMillis >= TimeUnit.DAYS.toMillis(7)
            "monthly" -> {
                // ... your monthly logic ...
                true // just for testing you can return true
            }
            else -> false
        }
    }

    fun getRemaining(): Double {
        val remaining = targetAmount - savedAmount
        return if (remaining > 0) remaining else 0.0
    }

    fun getProgress(): Int {
        if (targetAmount <= 0) return 0
        val progress = ((savedAmount / targetAmount) * 100).toInt()
        return progress.coerceIn(0, 100)
    }
}