package com.talha.rupiahkharch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.SavingsGoal
import com.talha.rupiahkharch.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository
    val allGoals: LiveData<List<SavingsGoal>>

    init {
        // We use the same Database class, but get the goalDao
        val dao = ExpenseDatabase.getDatabase(application).goalDao()
        repository = GoalRepository(dao)
        allGoals = repository.allGoals
    }

    fun insert(goal: SavingsGoal) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(goal)
    }

    fun update(goal: SavingsGoal) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(goal)
    }

    fun delete(goalId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(goalId)
    }
}