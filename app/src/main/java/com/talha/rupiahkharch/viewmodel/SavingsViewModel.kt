package com.talha.rupiahkharch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.SavingsGoal
import com.talha.rupiahkharch.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository

    // We use a MediatorLiveData or a simple SwitchMap to ensure we observe
    // goals for the correct user even if the Auth state changes.
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val allGoals: LiveData<List<SavingsGoal>>

    init {
        val dao = ExpenseDatabase.getDatabase(application).goalDao()
        repository = GoalRepository(dao)

        // Fetch only goals belonging to the logged-in user
        allGoals = repository.getAllGoals(userId)
    }

    /**
     * Updated insert to handle manual entry vs cloud sync.
     * @param isFromCloud set to true in SyncManager to prevent re-uploading.
     */
    fun insert(goal: SavingsGoal, isFromCloud: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(goal, isFromCloud)
    }

    fun update(goal: SavingsGoal) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(goal)
    }

    fun delete(goalId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(goalId)
    }

    /**
     * Clears all goals from the local database on logout.
     */
    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }
}