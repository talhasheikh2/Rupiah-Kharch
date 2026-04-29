package com.talha.rupiahkharch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    // UPDATED: Now observes expenses based on the current User ID
    val allExpenses: LiveData<List<Expense>>

    init {
        val dao = ExpenseDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(dao)

        // Grab current User ID from Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // UPDATED: Pass the userId to the repository function
        allExpenses = repository.getAllExpenses(userId).asLiveData()
    }

    /**
     * Standard delete used by SyncManager to clear local data
     * before restoring from Cloud.
     */
    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    /**
     * Specialized function for Logout that ensures the database is
     * wiped before the user is redirected to the Login screen.
     */
    fun logoutClearData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Updated Insert function to handle Cloud Sync.
     */
    fun insert(expense: Expense, isFromCloud: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        // Ensure the expense has the current userId before inserting
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val updatedExpense = if (expense.userId.isEmpty()) {
            expense.copy(userId = currentUserId)
        } else {
            expense
        }
        repository.insert(updatedExpense, isFromCloud)
    }

    fun update(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(expense)
    }

    fun delete(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(expense)
    }

    fun getExpenseById(id: Int): LiveData<Expense> {
        return repository.getExpenseById(id)
    }
}