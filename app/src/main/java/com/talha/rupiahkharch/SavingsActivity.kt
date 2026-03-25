package com.talha.rupiahkharch

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.adapter.SavingsGoalAdapter
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.SavingsGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavingsActivity : AppCompatActivity() {

    private lateinit var rvSavingsGoals: RecyclerView
    private lateinit var adapter: SavingsGoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        rvSavingsGoals = findViewById(R.id.rvSavingsGoals)
        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // 1. UPDATED: Initialize Adapter with TWO listeners (Delete and Pause)
        adapter = SavingsGoalAdapter(
            goals = emptyList(),
            onItemClick = { clickedGoal ->
                // Tapping the card still shows delete confirmation
                showDeleteConfirmation(clickedGoal)
            },
            onPauseClick = { clickedGoal ->
                // Tapping the pause icon toggles the status
                toggleGoalPause(clickedGoal)
            }
        )
        rvSavingsGoals.adapter = adapter

        val db = ExpenseDatabase.getDatabase(this)

        db.goalDao().getAllGoals().observe(this) { goals ->
            if (goals != null) {
                adapter.updateList(goals)
            }
        }
    }

    // 2. NEW: Function to toggle the Pause/Resume status in the DB
    private fun toggleGoalPause(goal: SavingsGoal) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = ExpenseDatabase.getDatabase(applicationContext)

            // Flip the boolean value
            goal.isPaused = !goal.isPaused

            // Save to database
            db.goalDao().updateGoal(goal)

            withContext(Dispatchers.Main) {
                val statusMessage = if (goal.isPaused) "Goal Paused" else "Goal Resumed"
                Toast.makeText(this@SavingsActivity, statusMessage, Toast.LENGTH_SHORT).show()
                // No need to refresh manually, LiveData will handle it!
            }
        }
    }

    private fun showDeleteConfirmation(goal: SavingsGoal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete the '${goal.title}' goal?")
            .setPositiveButton("Delete") { _, _ ->
                performDeletion(goal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeletion(goal: SavingsGoal) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = ExpenseDatabase.getDatabase(applicationContext)
            db.goalDao().deleteGoal(goal.id)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SavingsActivity, "Goal Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}