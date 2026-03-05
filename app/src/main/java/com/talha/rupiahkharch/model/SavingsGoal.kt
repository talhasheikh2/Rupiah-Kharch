package com.talha.rupiahkharch.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,          // e.g., "Car" from Category Picker
    val savedAmount: Double,    // Starts at 0.0
    val targetAmount: Double,   // From Numeric Input (e.g., 1,00,000)
    val iconRes: Int,           // Resource ID of the 3D icon
    val colorHex: String,       // Color for the gauge
    val deductionAmount: Double, // From Material Slider (e.g., 500)
    val frequency: String       // From Toggle Group (Daily, Weekly, Monthly)
) {
    fun getProgress(): Int {
        if (targetAmount <= 0) return 0
        return ((savedAmount / targetAmount) * 100).toInt()
    }

    fun getRemaining(): Double {
        return targetAmount - savedAmount
    }
}