package com.talha.rupiahkharch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.button.MaterialButton
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.talha.rupiahkharch.worker.SavingsWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter
    private var isIncomeView = true

    // Permission launcher for Android 13+ notifications
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications disabled. You won't see savings alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. SCHEDULING & PERMISSIONS
        askNotificationPermission()
        setupAutoSavingsWorker()

        // Initialize UI Elements
        val cardBackground = findViewById<RelativeLayout>(R.id.cardBackground)
        val tvCardTitle = findViewById<TextView>(R.id.tvCardTitle)
        val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
        val btnViewIncome = findViewById<Button>(R.id.btnViewIncome)
        val btnViewExpense = findViewById<Button>(R.id.btnViewExpense)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val recyclerView = findViewById<RecyclerView>(R.id.rvExpenses)
        val btnReports = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnReports)
        val btnSavings = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavings)

        btnSavings.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#009688"))

        // Initialize Adapter
        adapter = ExpenseAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener {
            Toast.makeText(this, "Swipe left to delete this record ⬅️", Toast.LENGTH_SHORT).show()
        }

        // Setup Swipe-to-Delete
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

                if (dX < 0) {
                    paint.color = Color.parseColor("#80FF6B6B")
                    val background = android.graphics.RectF(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)

                    val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        // Observe Database
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
            val totalIncome = allRecords.filter {
                it.category.lowercase() == "income" || it.category.lowercase() == "salary"
            }.sumOf { it.amount }

            val intent = Intent(this, AccountDetailActivity::class.java)
            intent.putExtra("TOTAL_INCOME", totalIncome)
            startActivity(intent)
        }

        btnAdd.setOnClickListener { showAddDialog() }

        btnReports.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }

        btnSavings.setOnClickListener {
            startActivity(Intent(this, SetupSavingsActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /**
     * Scheduling the background worker to handle Rs. 500 deductions
     */
    private fun setupAutoSavingsWorker() {
        val workManager = WorkManager.getInstance(this)

        // Keep ONLY the background loop (15-minute minimum)
        val periodicRequest = PeriodicWorkRequestBuilder<SavingsWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "SavingsAutomation",
            ExistingPeriodicWorkPolicy.KEEP, // KEEP ensures we don't reset the timer every time the app opens
            periodicRequest
        )

    }
    /**
     * Request notification permission for Android 13+
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateDashboardAndList(allRecords: List<Expense>, title: TextView, amount: TextView, bg: RelativeLayout) {
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
        loadToolbarTheme()
    }

    private fun loadToolbarTheme() {
        val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
        val colorHex = sharedPref.getString("ACCOUNT_COLOR", "#00B0FF")

        try {
            val colorInt = Color.parseColor(colorHex)
            val customToolbar = findViewById<RelativeLayout>(R.id.customToolbar)
            customToolbar?.setBackgroundColor(colorInt)
            window.statusBarColor = colorInt
        } catch (e: Exception) {
            val defaultColor = Color.parseColor("#00B0FF")
            findViewById<RelativeLayout>(R.id.customToolbar)?.setBackgroundColor(defaultColor)
        }
    }
}