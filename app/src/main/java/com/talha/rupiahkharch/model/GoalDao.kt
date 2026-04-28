package com.talha.rupiahkharch.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long // Return Long instead of Unit
    // Used by the SavingsActivity to show live updates on the UI
    @Query("SELECT * FROM savings_goals")
    fun getAllGoals(): LiveData<List<SavingsGoal>>

    /**
     * NEW: Used by SavingsWorker.
     * Background workers need a direct List, not LiveData.
     */
    @Query("SELECT * FROM savings_goals")
    fun getAllGoalsSync(): List<SavingsGoal>

    /**
     * NEW: Allows the worker to update the savedAmount
     * and lastDeductionDate for an existing goal.
     */
    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Int)
}