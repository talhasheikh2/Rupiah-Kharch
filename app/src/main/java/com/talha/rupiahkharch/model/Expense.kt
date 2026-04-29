package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "", // <--- THIS WAS MISSING
    var title: String,
    var amount: Double,
    val date: Long,
    var category: String,
    var color: String = "#000000"
)