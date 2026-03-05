package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val color: String = "#00B0FF" // Add this field with a default color
)