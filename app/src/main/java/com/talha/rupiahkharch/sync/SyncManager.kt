package com.talha.rupiahkharch.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.SavingsGoal
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.talha.rupiahkharch.viewmodel.SavingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncManager(
    private val context: Context,
    private val viewModel: ExpenseViewModel,
    private val savingsViewModel: SavingsViewModel
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun downloadUserData() {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("SyncManager", "CRITICAL: No user ID found during sync!")
            return
        }

        try {
            Log.d("SyncManager", "Starting sync for User: $userId")

            // --- 1. RESTORE PROFILE DATA ---
            // This pulls the name, email, and photo from the 'users' collection
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val sharedPref = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                sharedPref.edit().apply {
                    putString("user_name", userDoc.getString("name"))
                    putString("user_email", userDoc.getString("email"))
                    putString("user_phone", userDoc.getString("phone"))
                    putString("user_image", userDoc.getString("profileImageBase64"))
                    apply()
                }
                Log.d("SyncManager", "Profile Data Restored successfully.")
            } else {
                Log.e("SyncManager", "No user profile document found in Firestore.")
            }

            // --- 2. DOWNLOAD EXPENSES ---
            val expenseSnapshots = firestore.collection("users").document(userId)
                .collection("expenses").get().await()

            Log.d("SyncManager", "Firebase Found: ${expenseSnapshots.size()} expenses")

            val cloudExpenses = expenseSnapshots.documents.mapNotNull { doc ->
                try {
                    Expense(
                        id = doc.getLong("id")?.toInt() ?: doc.id.filter { it.isDigit() }.toIntOrNull() ?: 0,
                        userId = userId,
                        title = doc.getString("title") ?: "Restored Data",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                        category = doc.getString("category") ?: "other",
                        color = doc.getString("color") ?: "#000000"
                    )
                } catch (e: Exception) {
                    Log.e("SyncManager", "Error parsing expense ${doc.id}: ${e.message}")
                    null
                }
            }

            // --- 3. DOWNLOAD GOALS ---
            val goalSnapshots = firestore.collection("users").document(userId)
                .collection("goals").get().await()

            Log.d("SyncManager", "Firebase Found: ${goalSnapshots.size()} goals")

            val cloudGoals = goalSnapshots.documents.mapNotNull { doc ->
                try {
                    SavingsGoal(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        userId = userId,
                        title = doc.getString("title") ?: "Goal",
                        targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                        iconRes = doc.getLong("iconRes")?.toInt() ?: 0,
                        colorHex = doc.getString("colorHex") ?: "#009688",
                        deductionAmount = doc.getDouble("deductionAmount") ?: 0.0,
                        frequency = doc.getString("frequency") ?: "Daily",
                        startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                        savedAmount = doc.getDouble("savedAmount") ?: 0.0,
                        lastDeductionDate = doc.getLong("lastDeductionDate") ?: 0L,
                        minBalanceSafety = doc.getDouble("minBalanceSafety") ?: 10000.0,
                        isPaused = doc.getBoolean("isPaused") ?: false,
                        wasSkippedLowBalance = doc.getBoolean("wasSkippedLowBalance") ?: false,
                        lastNotificationDate = doc.getLong("lastNotificationDate") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("SyncManager", "Error parsing goal ${doc.id}: ${e.message}")
                    null
                }
            }

            // --- 4. SAVE TO LOCAL DATABASE ---
            withContext(Dispatchers.IO) {
                viewModel.deleteAll()
                savingsViewModel.deleteAll()

                cloudExpenses.forEach {
                    viewModel.insert(it, isFromCloud = true)
                }
                cloudGoals.forEach {
                    savingsViewModel.insert(it, isFromCloud = true)
                }

                Log.d("SyncManager", "Sync Finished. ${cloudExpenses.size} expenses and ${cloudGoals.size} goals written to Room.")
            }

        } catch (e: Exception) {
            Log.e("SyncManager", "FATAL SYNC ERROR: ${e.message}")
            e.printStackTrace()
        }
    }
}