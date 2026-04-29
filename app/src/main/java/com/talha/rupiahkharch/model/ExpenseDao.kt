package com.talha.rupiahkharch.model

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expense_table")
    suspend fun deleteAll()

    @Query("DELETE FROM sqlite_sequence WHERE name='expense_table'")
    suspend fun resetIdCounter()

    @Transaction
    suspend fun clearTableAndResetIds() {
        deleteAll()
        resetIdCounter()
    }

    // --- UPDATED QUERIES: FILTER BY USERID ---

    @Query("SELECT * FROM expense_table WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpenses(userId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expense_table WHERE userId = :userId ORDER BY date DESC LIMIT 10")
    fun getRecentExpenses(userId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expense_table WHERE id = :id")
    fun getExpenseById(id: Int): LiveData<Expense>

    // Important for the SavingsWorker: Calculate only for the active user
    @Query("SELECT SUM(amount) FROM expense_table WHERE userId = :userId AND category IN ('income', 'salary', 'Income', 'Salary')")
    fun getTotalIncomeSync(userId: String): Double?

    @Query("SELECT SUM(amount) FROM expense_table WHERE userId = :userId AND category NOT IN ('income', 'salary', 'Income', 'Salary')")
    fun getTotalExpenseSync(userId: String): Double?
}