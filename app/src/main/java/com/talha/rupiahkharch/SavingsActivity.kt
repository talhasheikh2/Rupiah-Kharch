package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.adapter.SavingsGoalAdapter
import com.talha.rupiahkharch.model.SavingsGoal
import com.talha.rupiahkharch.viewmodel.SavingsViewModel

class SavingsActivity : AppCompatActivity() {

    private lateinit var rvSavingsGoals: RecyclerView
    private lateinit var adapter: SavingsGoalAdapter
    private lateinit var btnBackToAccount: ImageButton
    private lateinit var backHome: ImageView
    private lateinit var savingsViewModel: SavingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        rvSavingsGoals = findViewById(R.id.rvSavingsGoals)
        btnBackToAccount = findViewById(R.id.backToAccount)
        backHome = findViewById(R.id.backHome)

        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // 1. Navigation
        btnBackToAccount.setOnClickListener {
            startActivity(Intent(this, AccountDetailActivity::class.java))
        }

        backHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // 2. Initialize Adapter
        adapter = SavingsGoalAdapter(
            goals = emptyList(),
            onItemClick = { clickedGoal -> showDeleteConfirmation(clickedGoal) },
            onPauseClick = { clickedGoal -> toggleGoalPause(clickedGoal) }
        )
        rvSavingsGoals.adapter = adapter

        // 3. UPDATED OBSERVER
        // In the ViewModel, allGoals is now correctly filtered by the current
        // logged-in user's ID. This ensures User A never sees User B's goals.
        savingsViewModel.allGoals.observe(this) { goals ->
            if (goals != null) {
                adapter.updateList(goals)
            }
        }
    }

    private fun toggleGoalPause(goal: SavingsGoal) {
        goal.isPaused = !goal.isPaused
        savingsViewModel.update(goal)
        val statusMessage = if (goal.isPaused) "Goal Paused" else "Goal Resumed"
        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(goal: SavingsGoal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete '${goal.title}'?")
            .setPositiveButton("Delete") { _, _ -> performDeletion(goal) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeletion(goal: SavingsGoal) {
        // Pass the ID to the ViewModel for both local and cloud deletion
        savingsViewModel.delete(goal.id)
        Toast.makeText(this, "Goal Deleted", Toast.LENGTH_SHORT).show()
    }
}