package com.talha.rupiahkharch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

class AccountDetailActivity : AppCompatActivity() {

    private lateinit var adapter: ExpenseAdapter
    private var recentTransactionsList: MutableList<Expense> = mutableListOf()
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var tvAccountName: TextView

    // --- 1. THE RESULT LAUNCHER ---
    // This triggers loadAccountTheme() immediately when you return from Editing
    private val editAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadAccountTheme()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_detail)

        // Initialize Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Initialize RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvRecentRecords)
        adapter = ExpenseAdapter(recentTransactionsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        // Initialize Views
        tvAccountName = findViewById(R.id.tvAccountDetailName)

        // --- 2. LOAD SAVED THEME (Name & Color) ---
        // This replaces the old database observer to prevent overwriting Income records
        loadAccountTheme()

        // Get Income passed from MainActivity
        val myIncomeBudget = intent.getDoubleExtra("TOTAL_INCOME", 0.0)

        setupLineChart()

        // --- 3. OBSERVE ALL TRANSACTIONS (Math & List Only) ---
        viewModel.allExpenses.observe(this) { allRecords: List<Expense> ->
            val onlyExpenses = allRecords.filter {
                it.category.lowercase() != "income" && it.category.lowercase() != "salary"
            }

            recentTransactionsList.clear()
            recentTransactionsList.addAll(allRecords)
            adapter.notifyDataSetChanged()

            val totalSpent = onlyExpenses.sumOf { it.amount }
            val remainingBalance = myIncomeBudget - totalSpent

            val tvMainBalance = findViewById<TextView>(R.id.tvMainBalance)
            tvMainBalance.text = "PKR ${String.format("%,.2f", remainingBalance)}"

            val tvPercentage = findViewById<TextView>(R.id.tvPercentage)
            if (myIncomeBudget > 0) {
                val percentUsed = (totalSpent / myIncomeBudget) * 100
                tvPercentage.text = "${String.format("%.1f", percentUsed)}%"
                tvPercentage.setTextColor(if (percentUsed > 100) Color.parseColor("#E74C3C") else Color.parseColor("#2ECC71"))
            } else {
                tvPercentage.text = "0%"
            }

            val tvDateRange = findViewById<TextView>(R.id.tvDateRange)
            if (allRecords.isNotEmpty()) {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                val start = sdf.format(Date(allRecords.minOf { it.date }))
                val end = sdf.format(Date(allRecords.maxOf { it.date }))
                tvDateRange.text = if (start == end) "Today, $start" else "$start - $end"
            } else {
                tvDateRange.text = "No records"
            }

            updateChartWithRealData(onlyExpenses)
        }

        // --- 4. EDIT BUTTON LOGIC ---
        val btnEditToolbar = findViewById<ImageView>(R.id.btnEdit)
        btnEditToolbar.setOnClickListener {
            val intent = Intent(this, EditAccountActivity::class.java)

            val currentName = tvAccountName.text.toString()
            val currentBalanceText = findViewById<TextView>(R.id.tvMainBalance).text.toString()
            val numericBalance = currentBalanceText.replace("PKR ", "").replace(",", "").toDoubleOrNull() ?: 0.0

            // Passing data to Edit Screen
            intent.putExtra("ACCOUNT_NAME", currentName)
            intent.putExtra("ACCOUNT_AMOUNT", numericBalance)

            editAccountLauncher.launch(intent)
        }
    }

    private fun loadAccountTheme() {
        val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
        val name = sharedPref.getString("ACCOUNT_NAME", "Your Account")
        val colorHex = sharedPref.getString("ACCOUNT_COLOR", "#00B0FF")

        tvAccountName.text = name

        try {
            val colorInt = Color.parseColor(colorHex)

            // Apply colors to Toolbar
            findViewById<Toolbar>(R.id.toolbar)?.setBackgroundColor(colorInt)

            // Apply colors to Wallet Card
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardWallet)
                ?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorInt))

            // Match the Status Bar
            window.statusBarColor = colorInt
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupLineChart() {
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            axisLeft.setGridDashedLine(android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f))
            animateX(1000)
        }
    }

    private fun updateChartWithRealData(expenses: List<Expense>) {
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        val entries = ArrayList<Entry>()
        val sortedExpenses = expenses.sortedBy { it.date }

        sortedExpenses.forEachIndexed { index, expense ->
            entries.add(Entry(index.toFloat(), expense.amount.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Spending History").apply {
            color = Color.parseColor("#00B0FF")
            setDrawFilled(true)
            fillColor = Color.parseColor("#E1F5FE")
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
            setDrawCircles(false)
        }

        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }
}