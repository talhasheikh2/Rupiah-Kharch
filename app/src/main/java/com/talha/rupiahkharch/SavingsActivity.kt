package com.talha.rupiahkharch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.adapter.SavingsGoalAdapter
import com.talha.rupiahkharch.model.ExpenseDatabase

class SavingsActivity : AppCompatActivity() {

    private lateinit var rvSavingsGoals: RecyclerView
    private lateinit var adapter: SavingsGoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        // 1. Initialize UI components
        rvSavingsGoals = findViewById(R.id.rvSavingsGoals)
        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // 2. Initialize Adapter with an empty list first
        adapter = SavingsGoalAdapter(emptyList())
        rvSavingsGoals.adapter = adapter

        // 3. Setup Database & Observation
        val db = ExpenseDatabase.getDatabase(this)

        // Using the modern lambda syntax (removing 'Observer {}')
        db.goalDao().getAllGoals().observe(this) { goals ->
            if (goals != null) {
                // 4. Use the new updateList function instead of creating a new adapter
                adapter.updateList(goals)
            }
        }
    }
}