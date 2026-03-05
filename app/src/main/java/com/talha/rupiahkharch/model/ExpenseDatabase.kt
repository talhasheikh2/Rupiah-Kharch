package com.talha.rupiahkharch.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Added SavingsGoal::class and incremented version to 3
@Database(entities = [Expense::class, SavingsGoal::class], version = 3)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    // Added GoalDao to provide access to savings goals
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "rupiah_kharch_db"
                )
                    // If you are still in development, fallbackToDestructiveMigration
                    // will clear the old DB and create version 3 from scratch.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}