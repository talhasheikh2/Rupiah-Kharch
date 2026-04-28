package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var title: String,
    var amount: Double,
    var date: Long,
    var category: String,
    var color: String = "#00B0FF"
)