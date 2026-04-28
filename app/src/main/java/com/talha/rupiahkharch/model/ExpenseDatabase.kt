package com.talha.rupiahkharch.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    // Added User::class to the entities list
    entities = [Expense::class, SavingsGoal::class, User::class],
    version = 11, // Increased version because we changed the database schema
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao

    // Added User DAO
    abstract fun userDao(): UserDao

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
                    .fallbackToDestructiveMigration() // Be careful: this wipes data when version changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}