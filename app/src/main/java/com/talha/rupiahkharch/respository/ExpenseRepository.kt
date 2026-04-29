package com.talha.rupiahkharch.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * UPDATED: Returns a flow of expenses specifically for the logged-in user.
     * If no user is found, it returns an empty flow.
     */
    fun getAllExpenses(userId: String): Flow<List<Expense>> {
        return if (userId.isNotEmpty()) {
            expenseDao.getAllExpenses(userId)
        } else {
            emptyFlow()
        }
    }

    suspend fun deleteAll() {
        expenseDao.deleteAll()
        try {
            expenseDao.resetIdCounter()
        } catch (e: Exception) {
            Log.e("Repository", "Could not reset ID counter: ${e.message}")
        }
    }

    fun getExpenseById(id: Int): LiveData<Expense> {
        return expenseDao.getExpenseById(id)
    }

    /**
     * Logic for inserting expenses.
     */
    suspend fun insert(expense: Expense, isFromCloud: Boolean = false) {
        // Ensure the expense has the current userId before saving locally
        val userId = auth.currentUser?.uid ?: ""
        val expenseWithUser = if (expense.userId.isEmpty()) expense.copy(userId = userId) else expense

        // 1. Always save to local Room database first
        val newIdLong = expenseDao.insert(expenseWithUser)

        // 2. Only push to Cloud if this is a manual entry
        if (!isFromCloud && userId.isNotEmpty()) {
            val updatedExpense = expenseWithUser.copy(id = newIdLong.toInt())
            try {
                firestore.collection("users").document(userId)
                    .collection("expenses")
                    .document(newIdLong.toString())
                    .set(updatedExpense)
                    .await()
            } catch (e: Exception) {
                Log.e("Repository", "Cloud upload failed: ${e.message}")
            }
        }
    }

    suspend fun update(expense: Expense) {
        expenseDao.update(expense)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("expenses")
                    .document(expense.id.toString())
                    .set(expense)
                    .await()
            } catch (e: Exception) {
                Log.e("Repository", "Cloud update failed: ${e.message}")
            }
        }
    }

    suspend fun delete(expense: Expense) {
        expenseDao.delete(expense)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("expenses")
                    .document(expense.id.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("Repository", "Cloud deletion failed: ${e.message}")
            }
        }
    }
}