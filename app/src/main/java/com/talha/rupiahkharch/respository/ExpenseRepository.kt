package com.talha.rupiahkharch.repository

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // Initialize Firebase Cloud Firestore
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpenseById(id: Int): LiveData<Expense> {
        return expenseDao.getExpenseById(id)
    }

    suspend fun insert(expense: Expense) {
        // 1. Save to Room and capture the new ID
        val newIdLong = expenseDao.insert(expense)

        // 2. Create a copy of the expense with the real ID
        val updatedExpense = expense.copy(id = newIdLong.toInt())

        // 3. Push to Cloud
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("expenses")
                    .document(newIdLong.toString()) // Use the ID as the document name
                    .set(updatedExpense)
                    .await()
            } catch (e: Exception) {
                // Log error or handle offline state
            }
        }
    }



    suspend fun update(expense: Expense) {
        expenseDao.update(expense)

        // Update in Cloud
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expense.id.toString())
                .set(expense)
        }
    }

    suspend fun delete(expense: Expense) {
        // 1. Delete from Room
        expenseDao.delete(expense)

        // 2. Delete from Cloud
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expense.id.toString())
                .delete()
        }
    }
}