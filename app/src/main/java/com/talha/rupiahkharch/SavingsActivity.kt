package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
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
    private lateinit var btnBackToAccount: ImageButton // Add this

    private lateinit var backHome: ImageView // Add this



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        // Initialize views
        rvSavingsGoals = findViewById(R.id.rvSavingsGoals)
        btnBackToAccount = findViewById(R.id.backToAccount) // Initialize the button
        backHome = findViewById(R.id.backHome) // Initialize the button


        rvSavingsGoals.layoutManager = GridLayoutManager(this, 2)

        // 1. Setup the Navigation to EditAccountActivity
        btnBackToAccount.setOnClickListener {
            val intent = Intent(this, AccountDetailActivity::class.java)
            startActivity(intent)
        }

        backHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 2. Initialize Adapter with TWO listeners (Delete and Pause)
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

        val db = ExpenseDatabase.getDatabase(this)

        db.goalDao().getAllGoals().observe(this) { goals ->
            if (goals != null) {
                adapter.updateList(goals)
            }
        }
    }

    // Toggle the Pause/Resume status in the DB
    private fun toggleGoalPause(goal: SavingsGoal) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = ExpenseDatabase.getDatabase(applicationContext)

            goal.isPaused = !goal.isPaused
            db.goalDao().updateGoal(goal)

            withContext(Dispatchers.Main) {
                val statusMessage = if (goal.isPaused) "Goal Paused" else "Goal Resumed"
                Toast.makeText(this@SavingsActivity, statusMessage, Toast.LENGTH_SHORT).show()
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