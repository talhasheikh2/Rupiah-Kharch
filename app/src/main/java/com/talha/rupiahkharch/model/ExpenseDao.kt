package com.talha.rupiahkharch.model

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)
    @Update // Add this
    suspend fun update(expense: Expense)
    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expense_table ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expense_table ORDER BY date DESC LIMIT 10")
    fun getRecentExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expense_table WHERE id = :id")
    fun getExpenseById(id: Int): LiveData<Expense>
}