package com.talha.rupiahkharch.worker

import android.content.Context
import android.util.Log // Added for Logcat
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

            // LOG: Check if worker started
            Log.d("SAVINGS_WORKER", "Worker started. Checking ${allGoals.size} goals.")

            allGoals.forEach { goal ->
                if (goal.isDeductionDue()) {
                    val currentTime = System.currentTimeMillis()

                    // 1. Update the Savings Goal progress
                    goal.savedAmount += goal.deductionAmount
                    goal.lastDeductionDate = currentTime
                    goalDao.updateGoal(goal)

                    // 2. Add to Main Screen Records
                    // FIXED: Category changed to "savings" (lowercase) to match your adapter filter
                    val savingsExpense = Expense(
                        id = 0,
                        title = "Auto-Save: ${goal.title}",
                        amount = goal.deductionAmount,
                        date = currentTime,
                        category = "savings",
                        color = "#009688"
                    )

                    expenseDao.insert(savingsExpense)

                    // LOG: Confirm insertion
                    Log.d("SAVINGS_WORKER", "SUCCESS: Added Rs. ${goal.deductionAmount} for ${goal.title}")

                    // 3. Trigger Notification
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