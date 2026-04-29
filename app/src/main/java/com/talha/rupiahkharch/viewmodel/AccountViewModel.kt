package com.talha.rupiahkharch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.repository.ExpenseRepository

class AccountViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // FIXED: Get current User ID and call the repository function
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // This now observes only the expenses belonging to the logged-in user
    val recentExpenses: LiveData<List<Expense>> = repository.getAllExpenses(userId).asLiveData()
}