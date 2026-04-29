package com.talha.rupiahkharch.model

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    /**
     * UPDATED: Now filters by userId to ensure User A
     * cannot see User B's goals.
     */
    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    fun getAllGoals(userId: String): LiveData<List<SavingsGoal>>

    /**
     * UPDATED: Used by SavingsWorker.
     * Filters by userId to process deductions only for the active user.
     */
    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    fun getAllGoalsSync(userId: String): List<SavingsGoal>

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Int)

    /**
     * NEW: Clears all goals.
     * Call this in your ViewModel's logout function.
     */
    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()

    /**
     * NEW: Resets the Auto-Increment ID counter.
     */
    @Query("DELETE FROM sqlite_sequence WHERE name='savings_goals'")
    suspend fun resetIdCounter()
}