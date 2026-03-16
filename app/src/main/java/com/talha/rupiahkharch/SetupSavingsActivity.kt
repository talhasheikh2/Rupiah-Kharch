package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.talha.rupiahkharch.CategoryAdapter
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.GoalCategory
import com.talha.rupiahkharch.model.SavingsGoal
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SetupSavingsActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var etTargetAmount: EditText
    private lateinit var etDeductionAmount: EditText
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var tvPrediction: TextView
    private lateinit var btnStartGoal: MaterialButton

    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategoryName: String = "Car"
    private var selectedIconRes: Int = R.drawable.cars

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_savings)

        initViews()
        setupCategoryRecyclerView()
        setupInputListeners()

        btnStartGoal.setOnClickListener {
            saveGoalToDatabase()
        }
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        etDeductionAmount = findViewById(R.id.etDeductionAmount)
        toggleGroup = findViewById(R.id.toggleGroup)
        tvPrediction = findViewById(R.id.tvPrediction)
        btnStartGoal = findViewById(R.id.btnStartGoal)

        toggleGroup.check(R.id.btnDaily)
    }

    private fun setupInputListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateFuturePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etTargetAmount.addTextChangedListener(textWatcher)
        etDeductionAmount.addTextChangedListener(textWatcher)

        toggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) updateFuturePreview()
        }
    }

    private fun setupCategoryRecyclerView() {
        val categoryList = listOf(
            GoalCategory(1, "Car", R.drawable.cars),
            GoalCategory(2, "House", R.drawable.house),
            GoalCategory(3, "Emergency Fund", R.drawable.emergency),
            GoalCategory(4, "Travel", R.drawable.travelling),
            GoalCategory(5, "School Fees", R.drawable.education),
            GoalCategory(6, "Business", R.drawable.business),
            GoalCategory(7, "Motor Bike", R.drawable.motorbike)
        )

        categoryAdapter = CategoryAdapter(categoryList) { selectedGoal ->
            selectedCategoryName = selectedGoal.name
            selectedIconRes = selectedGoal.iconRes
            updateFuturePreview()
        }

        rvCategories.apply {
            layoutManager = LinearLayoutManager(this@SetupSavingsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun updateFuturePreview() {
        val targetAmount = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
        val deductionAmount = etDeductionAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (targetAmount > 0 && deductionAmount > 0) {
            val totalUnits = (targetAmount / deductionAmount).toInt()
            val calendar = Calendar.getInstance()

            when (toggleGroup.checkedButtonId) {
                R.id.btnDaily -> calendar.add(Calendar.DAY_OF_YEAR, totalUnits)
                R.id.btnWeekly -> calendar.add(Calendar.WEEK_OF_YEAR, totalUnits)
                R.id.btnMonthly -> calendar.add(Calendar.MONTH, totalUnits)
            }

            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val year = calendar.get(Calendar.YEAR)

            tvPrediction.text = "Future Preview / At this rate, you will reach your $selectedCategoryName goal by $month $year."
        } else {
            tvPrediction.text = "Enter amounts to see your prediction."
        }
    }

    private fun saveGoalToDatabase() {
        val targetAmountValue = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
        val deductionAmountValue = etDeductionAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (targetAmountValue <= 0) {
            etTargetAmount.error = "Please enter a valid target"
            return
        }

        val frequency = when (toggleGroup.checkedButtonId) {
            R.id.btnDaily -> "Daily"
            R.id.btnWeekly -> "Weekly"
            R.id.btnMonthly -> "Monthly"
            else -> "Daily"
        }

        val currentTime = System.currentTimeMillis()

        // 1. Prepare the Goal
        val newGoal = SavingsGoal(
            id = 0,
            title = selectedCategoryName,
            targetAmount = targetAmountValue,
            iconRes = selectedIconRes,
            colorHex = "#009688",
            deductionAmount = deductionAmountValue,
            frequency = frequency,
            startDate = currentTime,
            savedAmount = deductionAmountValue, // Record the first deduction
            lastDeductionDate = currentTime    // Set this to now so worker waits for the NEXT interval
        )

        // 2. Prepare the Expense record for the Main Screen
        val firstDeductionRecord = com.talha.rupiahkharch.model.Expense(
            id = 0,
            title = "Savings For : $selectedCategoryName",
            amount = deductionAmountValue,
            date = currentTime,
            category = "savings", // Lowercase to match your adapter!
            color = "#009688"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)

                // SAVE BOTH to the database
                db.goalDao().insertGoal(newGoal)
                db.expenseDao().insert(firstDeductionRecord) // THIS WAS MISSING!

                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Goal Started & First Deduction Added!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SetupSavingsActivity, SavingsActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}