package com.talha.rupiahkharch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val allExpenses: LiveData<List<Expense>>

    init {
        val dao = ExpenseDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(dao)
        allExpenses = repository.allExpenses.asLiveData()
    }

    fun insert(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(expense)
    }
    fun update(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(expense)
    }
    fun delete(expense: Expense) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(expense)
    }
    // Change this:
    fun getExpenseById(id: Int): LiveData<Expense> {
        return repository.getExpenseById(id) // Call 'repository', NOT 'expenseDao'
    }
}