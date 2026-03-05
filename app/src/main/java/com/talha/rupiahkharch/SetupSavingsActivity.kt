package com.talha.rupiahkharch

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.GoalCategory
import com.talha.rupiahkharch.model.SavingsGoal
import kotlinx.coroutines.launch
import java.util.*

class SetupSavingsActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var rvCategories: RecyclerView
    private lateinit var etTargetAmount: EditText
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnDaily: MaterialButton
    private lateinit var btnWeekly: MaterialButton
    private lateinit var btnMonthly: MaterialButton
    private lateinit var tvDeductionValue: TextView
    private lateinit var deductionSlider: Slider
    private lateinit var tvPrediction: TextView
    private lateinit var btnStartGoal: MaterialButton

    private lateinit var categoryAdapter: CategoryAdapter

    // Selection Tracking
    private var selectedCategoryName: String = "Car"
    private var selectedIconRes: Int = R.drawable.car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_savings) // Ensure this matches your XML file name

        // Initialize all views manually
        initViews()

        setupCategoryRecyclerView()
        setupSliderLogic()

        btnStartGoal.setOnClickListener {
            saveGoalToDatabase()
        }
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        toggleGroup = findViewById(R.id.toggleGroup)
        btnDaily = findViewById(R.id.btnDaily)
        btnWeekly = findViewById(R.id.btnWeekly)
        btnMonthly = findViewById(R.id.btnMonthly)
        tvDeductionValue = findViewById(R.id.tvDeductionValue)
        deductionSlider = findViewById(R.id.deductionSlider)
        tvPrediction = findViewById(R.id.tvPrediction)
        btnStartGoal = findViewById(R.id.btnStartGoal)
    }

    private fun setupCategoryRecyclerView() {
        val categoryList = listOf(
            GoalCategory(1, "Car", R.drawable.cars),
            GoalCategory(2, "House", R.drawable.home),
            GoalCategory(3, "Emergency Fund", R.drawable.emergencey),
            GoalCategory(4, "Travel", R.drawable.travelling),
            GoalCategory(5, "School Fees", R.drawable.educations),
            GoalCategory(6, "Business", R.drawable.bussinnes) ,
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

    private fun setupSliderLogic() {
        deductionSlider.addOnChangeListener { _, value, _ ->
            tvDeductionValue.text = "Rs. ${value.toInt()}"
            updateFuturePreview()
        }
    }

    private fun updateFuturePreview() {
        val targetAmount = etTargetAmount.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
        val deductionAmount = deductionSlider.value.toDouble()

        if (targetAmount > 0 && deductionAmount > 0) {
            val totalUnits = (targetAmount / deductionAmount).toInt()
            val calendar = Calendar.getInstance()

            // Check checkedButtonId from the toggle group
            when (toggleGroup.checkedButtonId) {
                R.id.btnDaily -> calendar.add(Calendar.DAY_OF_YEAR, totalUnits)
                R.id.btnWeekly -> calendar.add(Calendar.WEEK_OF_YEAR, totalUnits)
                R.id.btnMonthly -> calendar.add(Calendar.MONTH, totalUnits)
            }

            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val year = calendar.get(Calendar.YEAR)

            tvPrediction.text = "Future Preview / At this rate, you will reach your $selectedCategoryName goal by $month $year."
        }
    }

    private fun saveGoalToDatabase() {
        val targetAmount = etTargetAmount.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
        val deductionAmount = deductionSlider.value.toDouble()

        val frequency = when (toggleGroup.checkedButtonId) {
            R.id.btnDaily -> "Daily"
            R.id.btnWeekly -> "Weekly"
            else -> "Monthly"
        }

        if (targetAmount <= 0) {
            etTargetAmount.error = "Please enter a valid amount"
            return
        }

        val newGoal = SavingsGoal(
            title = selectedCategoryName,
            savedAmount = 0.0,
            targetAmount = targetAmount,
            iconRes = selectedIconRes,
            colorHex = "#009688",
            deductionAmount = deductionAmount,
            frequency = frequency
        )

        lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(this@SetupSavingsActivity)
            db.goalDao().insertGoal(newGoal)
            finish()
        }
    }
}