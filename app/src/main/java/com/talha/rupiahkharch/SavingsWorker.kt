package com.talha.rupiahkharch.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.talha.rupiahkharch.NotificationHelper
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDatabase
import java.util.*

class SavingsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ExpenseDatabase.getDatabase(applicationContext)
            val goalDao = db.goalDao()
            val expenseDao = db.expenseDao()

            val allGoals = goalDao.getAllGoalsSync()
            Log.d("SAVINGS_WORKER", "Worker started. Checking ${allGoals.size} goals.")

            // 1. CALCULATE CURRENT BALANCE ONCE
            val income = expenseDao.getTotalIncomeSync() ?: 0.0
            val expenses = expenseDao.getTotalExpenseSync() ?: 0.0
            val currentBalance = income - expenses

            Log.d("SAVINGS_WORKER", "Current Balance: Rs. $currentBalance")

            allGoals.forEach { goal ->
                // 2. CHECK: IS MANUALLY PAUSED?
                if (goal.isPaused) {
                    Log.d("SAVINGS_WORKER", "Skipping '${goal.title}': Goal is paused by user.")
                    return@forEach
                }

                // 3. CHECK: SAFETY NET (Does user have enough money?)
                if (currentBalance < goal.minBalanceSafety) {
                    Log.d("SAVINGS_WORKER", "Safety Net Triggered for '${goal.title}'!")

                    // Update UI flag (This stays true so the card shows the red alert)
                    if (!goal.wasSkippedLowBalance) {
                        goal.wasSkippedLowBalance = true
                        goalDao.updateGoal(goal)
                    }

                    // --- NEW: NOTIFICATION THROTTLING (Max ~3 times a week) ---
                    val currentTime = System.currentTimeMillis()
                    val fortyEightHours = 48 * 60 * 60 * 1000L // 48 hours in milliseconds

                    if (currentTime - goal.lastNotificationDate > fortyEightHours) {
                        NotificationHelper.showNotification(
                            applicationContext,
                            "Savings Paused",
                            "We skipped your '${goal.title}' saving today to keep your balance above Rs. ${goal.minBalanceSafety}."
                        )

                        // Update the notification timestamp
                        goal.lastNotificationDate = currentTime
                        goalDao.updateGoal(goal)
                    }

                    return@forEach
                } else {
                    // Reset flag if balance is now healthy
                    if (goal.wasSkippedLowBalance) {
                        goal.wasSkippedLowBalance = false
                        goalDao.updateGoal(goal)
                    }
                }

                // 4. CHECK: IS IT TIME TO DEDUCT?
                if (goal.isDeductionDue()) {
                    val currentTime = System.currentTimeMillis()

                    // Update Goal Progress
                    goal.savedAmount += goal.deductionAmount
                    goal.lastDeductionDate = currentTime

                    // IMPORTANT: Reset notification timer & UI flag on success
                    goal.wasSkippedLowBalance = false
                    goal.lastNotificationDate = 0L

                    goalDao.updateGoal(goal)

                    // Add Record to Main Screen
                    val savingsExpense = Expense(
                        id = 0,
                        title = "Savings Plan: ${goal.title}",
                        amount = goal.deductionAmount,
                        date = currentTime,
                        category = "savings",
                        color = "#009688"
                    )
                    expenseDao.insert(savingsExpense)

                    Log.d("SAVINGS_WORKER", "SUCCESS: Added Rs. ${goal.deductionAmount} for ${goal.title}")

                    NotificationHelper.showNotification(
                        applicationContext,
                        "Savings Alert!",
                        "Rs. ${goal.deductionAmount} added to ${goal.title}."
                    )
                } else {
                    Log.d("SAVINGS_WORKER", "Goal '${goal.title}' is not due yet.")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SAVINGS_WORKER", "CRITICAL ERROR: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}