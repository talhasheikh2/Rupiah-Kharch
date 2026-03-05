package com.talha.rupiahkharch.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GoalDao {
    // Adding OnConflictStrategy replaces the goal if it already exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals")
    fun getAllGoals(): LiveData<List<SavingsGoal>>

    // Optional: Add this to delete a goal when it's completed
    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Int)
}