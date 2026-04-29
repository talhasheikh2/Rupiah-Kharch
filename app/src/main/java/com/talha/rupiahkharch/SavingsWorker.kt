package com.talha.rupiahkharch.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.NotificationHelper
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDatabase
import java.util.*

class SavingsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. GET THE CURRENT USER ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.d("SAVINGS_WORKER", "No user logged in. Worker stopping.")
                return Result.success()
            }

            val db = ExpenseDatabase.getDatabase(applicationContext)
            val goalDao = db.goalDao()
            val expenseDao = db.expenseDao()

            // 2. Fetch only THIS user's goals
            val allGoals = goalDao.getAllGoalsSync(userId)
            Log.d("SAVINGS_WORKER", "Worker started for user $userId. Checking ${allGoals.size} goals.")

            // 3. CALCULATE BALANCE (Passing userId to updated Dao functions)
            val income = expenseDao.getTotalIncomeSync(userId) ?: 0.0
            val expenses = expenseDao.getTotalExpenseSync(userId) ?: 0.0
            val currentBalance = income - expenses

            Log.d("SAVINGS_WORKER", "Current Balance for $userId: Rs. $currentBalance")

            allGoals.forEach { goal ->
                if (goal.isPaused) return@forEach

                // Check Safety Net
                if (currentBalance < goal.minBalanceSafety) {
                    if (!goal.wasSkippedLowBalance) {
                        goal.wasSkippedLowBalance = true
                        goalDao.updateGoal(goal)
                    }

                    val currentTime = System.currentTimeMillis()
                    val fortyEightHours = 48 * 60 * 60 * 1000L

                    if (currentTime - goal.lastNotificationDate > fortyEightHours) {
                        NotificationHelper.showNotification(
                            applicationContext,
                            "Savings Paused",
                            "We skipped your '${goal.title}' saving today to keep your balance above Rs. ${goal.minBalanceSafety}."
                        )
                        goal.lastNotificationDate = currentTime
                        goalDao.updateGoal(goal)
                    }
                    return@forEach
                } else {
                    if (goal.wasSkippedLowBalance) {
                        goal.wasSkippedLowBalance = false
                        goalDao.updateGoal(goal)
                    }
                }

                // Check if Deduction is Due
                if (goal.isDeductionDue()) {
                    val currentTime = System.currentTimeMillis()

                    // Update Goal
                    goal.savedAmount += goal.deductionAmount
                    goal.lastDeductionDate = currentTime
                    goal.wasSkippedLowBalance = false
                    goal.lastNotificationDate = 0L
                    goalDao.updateGoal(goal)

                    // 4. Record Expense WITH userId (Fixed the 'userId' symbol error)
                    val savingsExpense = Expense(
                        id = 0,
                        userId = userId, // Attached the ID here
                        title = "Savings Plan: ${goal.title}",
                        amount = goal.deductionAmount,
                        date = currentTime,
                        category = "savings",
                        color = "#009688"
                    )
                    expenseDao.insert(savingsExpense)

                    NotificationHelper.showNotification(
                        applicationContext,
                        "Savings Alert!",
                        "Rs. ${goal.deductionAmount} added to ${goal.title}."
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SAVINGS_WORKER", "CRITICAL ERROR: ${e.message}")
            Result.retry()
        }
    }
}