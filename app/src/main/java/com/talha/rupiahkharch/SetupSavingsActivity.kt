package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.talha.rupiahkharch.CategoryAdapter
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.GoalCategory
import com.talha.rupiahkharch.model.SavingsGoal
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.talha.rupiahkharch.viewmodel.SavingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SetupSavingsActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var etTargetAmount: EditText
    private lateinit var etDeductionAmount: EditText
    private lateinit var etMinBalanceSafety: EditText
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var tvPrediction: TextView
    private lateinit var btnStartGoal: MaterialButton

    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategoryName: String = "Car"
    private var selectedIconRes: Int = R.drawable.cars

    // NEW: ViewModels for Cloud Sync
    private lateinit var savingsViewModel: SavingsViewModel
    private lateinit var expenseViewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_savings)

        initViews()
        setupCategoryRecyclerView()
        setupInputListeners()

        // Initialize ViewModels
        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)
        expenseViewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

        btnStartGoal.setOnClickListener {
            saveGoalToDatabase()
        }
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        etDeductionAmount = findViewById(R.id.etDeductionAmount)
        etMinBalanceSafety = findViewById(R.id.etMinBalanceSafety)
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

// ... inside SetupSavingsActivity.kt ...

// ... (keep imports and class variables as they are)

    private fun saveGoalToDatabase() {
        val targetAmountValue = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
        val deductionAmountValue = etDeductionAmount.text.toString().toDoubleOrNull() ?: 0.0
        val minSafetyValue = etMinBalanceSafety.text.toString().toDoubleOrNull() ?: 10000.0

        // Get current User ID
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (targetAmountValue <= 0) {
            etTargetAmount.error = "Please enter a valid target"
            return
        }

        if (deductionAmountValue <= 0) {
            etDeductionAmount.error = "Please enter a deduction amount"
            return
        }

        val frequency = when (toggleGroup.checkedButtonId) {
            R.id.btnDaily -> "Daily"
            R.id.btnWeekly -> "Weekly"
            R.id.btnMonthly -> "Monthly"
            else -> "Daily"
        }

        val currentTime = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)

                // FIXED: Pass currentUserId to these functions to match the updated Dao
                val income = db.expenseDao().getTotalIncomeSync(currentUserId) ?: 0.0
                val expense = db.expenseDao().getTotalExpenseSync(currentUserId) ?: 0.0
                val currentBalance = income - expense

                val initialSavedAmount = if (currentBalance >= minSafetyValue) deductionAmountValue else 0.0

                // 1. Prepare the Goal object
                val newGoal = SavingsGoal(
                    id = 0,
                    userId = currentUserId,
                    title = selectedCategoryName,
                    targetAmount = targetAmountValue,
                    iconRes = selectedIconRes,
                    colorHex = "#009688",
                    deductionAmount = deductionAmountValue,
                    frequency = frequency,
                    startDate = currentTime,
                    savedAmount = initialSavedAmount,
                    lastDeductionDate = if (initialSavedAmount > 0) currentTime else 0L,
                    minBalanceSafety = minSafetyValue,
                    isPaused = false
                )

                // 2. Save via ViewModel
                savingsViewModel.insert(newGoal)

                // 3. Record expense if deduction happened
                if (initialSavedAmount > 0) {
                    val firstDeductionRecord = com.talha.rupiahkharch.model.Expense(
                        id = 0,
                        userId = currentUserId, // FIXED: Added userId here too
                        title = "Savings: $selectedCategoryName",
                        amount = deductionAmountValue,
                        date = currentTime,
                        category = "savings",
                        color = "#009688"
                    )
                    expenseViewModel.insert(firstDeductionRecord)
                }

                withContext(Dispatchers.Main) {
                    val msg = if (initialSavedAmount > 0)
                        "Goal Started & First Deduction Synced!"
                    else
                        "Goal Started! Deduction skipped (Balance low)."

                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
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