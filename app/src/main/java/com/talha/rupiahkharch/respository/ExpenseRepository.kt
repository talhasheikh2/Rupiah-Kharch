package com.talha.rupiahkharch.repository

import androidx.lifecycle.LiveData
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDao
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpenseById(id: Int): LiveData<Expense> {
        return expenseDao.getExpenseById(id)
    }
    suspend fun insert(expense: Expense) {
        expenseDao.insert(expense)
    }
    suspend fun update(expense: Expense) {
        expenseDao.update(expense)
    }
    suspend fun delete(expense: Expense) {
        expenseDao.delete(expense)
    }
}