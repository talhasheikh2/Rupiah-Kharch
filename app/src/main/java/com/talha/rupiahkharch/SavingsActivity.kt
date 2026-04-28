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

    // NEW: Add the ViewModel reference
    private lateinit var savingsViewModel: SavingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        // Initialize views
        rvSavingsGoals = findViewById(R.id.rvSavingsGoals)
        btnBackToAccount = findViewById(R.id.backToAccount)
        backHome = findViewById(R.id.backHome)

        // Initialize ViewModel
        // This is the "Bridge" to your Repository and Firebase
        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // 1. Setup Navigation
        btnBackToAccount.setOnClickListener {
            val intent = Intent(this, AccountDetailActivity::class.java)
            startActivity(intent)
        }

        backHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 2. Initialize Adapter
        adapter = SavingsGoalAdapter(
            goals = emptyList(),
            onItemClick = { clickedGoal ->
                showDeleteConfirmation(clickedGoal)
            },
            onPauseClick = { clickedGoal ->
                toggleGoalPause(clickedGoal)
            }
        )
        rvSavingsGoals.adapter = adapter

        // 3. Observe the ViewModel instead of the DB directly
        // This ensures the UI updates whenever the local or cloud data changes
        savingsViewModel.allGoals.observe(this) { goals ->
            if (goals != null) {
                adapter.updateList(goals)
            }
        }
    }

    // UPDATED: Toggle Pause status using ViewModel (Triggers Cloud Sync)
    private fun toggleGoalPause(goal: SavingsGoal) {
        goal.isPaused = !goal.isPaused

        // This call goes: ViewModel -> Repository -> (Room + Firebase)
        savingsViewModel.update(goal)

        val statusMessage = if (goal.isPaused) "Goal Paused" else "Goal Resumed"
        Toast.makeText(this@SavingsActivity, statusMessage, Toast.LENGTH_SHORT).show()
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

    // UPDATED: Perform Deletion using ViewModel (Triggers Cloud Deletion)
    private fun performDeletion(goal: SavingsGoal) {
        // We pass the ID to the ViewModel
        savingsViewModel.delete(goal.id)

        Toast.makeText(this@SavingsActivity, "Goal Deleted from Phone & Cloud", Toast.LENGTH_SHORT).show()
    }
}