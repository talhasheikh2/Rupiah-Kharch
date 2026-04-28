package com.talha.rupiahkharch.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import kotlinx.coroutines.tasks.await

class SyncManager(private val context: Context, private val viewModel: ExpenseViewModel) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun downloadUserData() {
        val userId = auth.currentUser?.uid ?: return

        // 1. Download Profile Details
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
        }

        // 2. Download Expenses & Sync to Room Database
        val expenseSnapshots = firestore.collection("users").document(userId)
            .collection("expenses").get().await()

        val cloudExpenses = expenseSnapshots.documents.mapNotNull { doc ->
            // Convert Firestore doc back to your Expense Model
            Expense(
                title = doc.getString("title") ?: "",
                amount = doc.getDouble("amount") ?: 0.0,
                date = doc.getLong("date") ?: System.currentTimeMillis(),
                category = doc.getString("category") ?: "other"
            )
        }

        // Save them to your local Room DB so they show up in the list
        cloudExpenses.forEach { viewModel.insert(it) }
    }
}