package com.talha.rupiahkharch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.talha.rupiahkharch.repository.ExpenseRepository

class AccountViewModel(private val repository: ExpenseRepository) : ViewModel() {
    val recentExpenses = repository.allExpenses.asLiveData()
}