package com.talha.rupiahkharch

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.google.android.material.button.MaterialButton
import android.content.Intent
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter
    private var isIncomeView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Elements
        val cardBackground = findViewById<RelativeLayout>(R.id.cardBackground)
        val tvCardTitle = findViewById<TextView>(R.id.tvCardTitle)
        val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
        val btnViewIncome = findViewById<Button>(R.id.btnViewIncome)
        val btnViewExpense = findViewById<Button>(R.id.btnViewExpense)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val recyclerView = findViewById<RecyclerView>(R.id.rvExpenses)
        // Find the button by its ID
        val btnReports = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnReports)


        // Inside onCreate
        val btnSavings = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavings)
        btnSavings.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#009688"))


        // 1. Initialize Adapter FIRST with an empty list
        adapter = ExpenseAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 2. Set the Click Listener for the instruction popup
        adapter.setOnItemClickListener {
            Toast.makeText(this, "Swipe left to delete this record ⬅️", Toast.LENGTH_SHORT).show()
        }

        // 3. Setup Swipe-to-Delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expenseToDelete = adapter.getExpenseAt(position)
                viewModel.delete(expenseToDelete)
                Toast.makeText(this@MainActivity, "${expenseToDelete.title} deleted", Toast.LENGTH_SHORT).show()
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()

                if (dX < 0) { // Swiping to the left
                    // 1. Lighter Red Color (Coral/Salmon)
                    paint.color = Color.parseColor("#80FF6B6B")
                    val background = android.graphics.RectF(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)

                    // 2. Draw the Icon Safely
                    // Make sure you have an icon in drawable named ic_delete
                    val icon = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)

                    icon?.let {
                        // Calculate position
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(Color.WHITE) // Forces the icon to be White
                        it.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        // 4. Observe Database
        viewModel.allExpenses.observe(this) { allRecords ->
            updateDashboardAndList(allRecords, tvCardTitle, tvTotalAmount, cardBackground)
        }

        // Button Listeners
        btnViewIncome.setOnClickListener {
            isIncomeView = true
            viewModel.allExpenses.value?.let { updateDashboardAndList(it, tvCardTitle, tvTotalAmount, cardBackground) }
        }

        btnViewExpense.setOnClickListener {
            isIncomeView = false
            viewModel.allExpenses.value?.let { updateDashboardAndList(it, tvCardTitle, tvTotalAmount, cardBackground) }
        }
        val btnAccountDetails = findViewById<MaterialButton>(R.id.btnAccountDetails)

        btnAccountDetails.setOnClickListener {
            val allRecords = viewModel.allExpenses.value ?: emptyList()

            // 2. Filter and Sum the income
            val totalIncome = allRecords.filter {
                it.category.lowercase() == "income" || it.category.lowercase() == "salary"
            }.sumOf { it.amount }

            // 3. Create Intent and pass the value
            val intent = Intent(this, AccountDetailActivity::class.java)
            intent.putExtra("TOTAL_INCOME", totalIncome) // "TOTAL_INCOME" is the key
            startActivity(intent)
        }
        btnAdd.setOnClickListener { showAddDialog() }
        // Set the click listener
        btnReports.setOnClickListener {
            // Create an Intent to go from MainActivity to RecordsActivity
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }

        btnSavings.setOnClickListener {
            // Create an Intent to go from the current activity to SavingsActivity
            val intent = Intent(this, SetupSavingsActivity::class.java)
            startActivity(intent)

            // Optional: Add a smooth slide transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }

    private fun updateDashboardAndList(allRecords: List<Expense>, title: TextView, amount: TextView, bg: RelativeLayout) {
        // We check for "income" OR "salary" to be safe
        val filteredList = if (isIncomeView) {
            allRecords.filter { it.category.lowercase() == "income" || it.category.lowercase() == "salary" }
        } else {
            allRecords.filter { it.category.lowercase() != "income" && it.category.lowercase() != "salary" }
        }

        title.text = if (isIncomeView) "Total Income" else "Total Expense"
        bg.setBackgroundColor(Color.parseColor(if (isIncomeView) "#102A43" else "#d3ae53"))
        amount.text = "${filteredList.sumOf { it.amount }}"

        adapter.updateData(filteredList)
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // CRITICAL: Initialize the Spinner with data
        val categories = arrayOf("Food", "Rent", "Shopping", "Transport", "Health","Bill","Fuel", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = spinnerAdapter

        rgType.setOnCheckedChangeListener { _, checkedId ->
            spCategory.visibility = if (checkedId == R.id.rbExpense) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            val category = if (rgType.checkedRadioButtonId == R.id.rbIncome) {
                "income"
            } else {
                spCategory.selectedItem.toString().lowercase()
            }

            if (title.isNotEmpty() && amount > 0) {
                val newExpense = Expense(
                    title = title,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    category = category
                )
                viewModel.insert(newExpense)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter name and amount", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
    override fun onResume() {
        super.onResume()
        // Refresh the color every time the user returns to this screen
        loadToolbarTheme()
    }

    private fun loadToolbarTheme() {
        val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
        // Pull only the color. Default is your current blue: #00B0FF
        val colorHex = sharedPref.getString("ACCOUNT_COLOR", "#00B0FF")

        try {
            val colorInt = Color.parseColor(colorHex)

            // Update the RelativeLayout background
            val customToolbar = findViewById<RelativeLayout>(R.id.customToolbar)
            customToolbar?.setBackgroundColor(colorInt)

            // Update the System Status Bar to match for a clean look
            window.statusBarColor = colorInt

        } catch (e: Exception) {
            // Fallback to default if something goes wrong
            val defaultColor = Color.parseColor("#00B0FF")
            findViewById<RelativeLayout>(R.id.customToolbar)?.setBackgroundColor(defaultColor)
        }
    }
}