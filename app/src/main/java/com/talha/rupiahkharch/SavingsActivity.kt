package com.talha.rupiahkharch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.adapter.SavingsGoalAdapter
import com.talha.rupiahkharch.model.SavingsGoal

class SavingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        val rvSavingsGoals = findViewById<RecyclerView>(R.id.rvSavingsGoals)

        // Mock data matching your image
        // Updated mock data matching your new model structure
        val goals = listOf(
            SavingsGoal(id = 1, title = "Travel and Vacation", savedAmount = 13500.0, targetAmount = 25000.0, iconRes = R.drawable.travel, colorHex = "#5C6BC0", deductionAmount = 500.0, frequency = "Weekly"),
            SavingsGoal(id = 2, title = "Emergency Fund", savedAmount = 8000.0, targetAmount = 10000.0, iconRes = R.drawable.alarm, colorHex = "#FF7043", deductionAmount = 200.0, frequency = "Daily"),
            SavingsGoal(id = 3, title = "Home Purchase", savedAmount = 175000.0, targetAmount = 250000.0, iconRes = R.drawable.home2, colorHex = "#66BB6A", deductionAmount = 1000.0, frequency = "Monthly"),
            SavingsGoal(id = 4, title = "Buying a Car", savedAmount = 36500.0, targetAmount = 75000.0, iconRes = R.drawable.car2, colorHex = "#29B6F6", deductionAmount = 500.0, frequency = "Weekly"),
            SavingsGoal(id = 5, title = "Education", savedAmount = 25000.0, targetAmount = 40000.0, iconRes = R.drawable.school, colorHex = "#FFA726", deductionAmount = 300.0, frequency = "Weekly"),
            SavingsGoal(id = 6, title = "Business", savedAmount = 80000.0, targetAmount = 100000.0, iconRes = R.drawable.fire, colorHex = "#AB47BC", deductionAmount = 1000.0, frequency = "Monthly")
        )

        // Set the LayoutManager to 2 columns
        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // Connect the Adapter (Make sure to pass the data)
        val adapter = SavingsGoalAdapter(goals)
        rvSavingsGoals.adapter = adapter
    }
}