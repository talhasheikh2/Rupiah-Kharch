package com.talha.rupiahkharch

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.talha.rupiahkharch.worker.SavingsWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter
    private var isIncomeView = true

    // NEW: Profile ImageView reference
    private lateinit var btnProfile: ImageView

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
        val btnReports = findViewById<MaterialButton>(R.id.btnReports)
        val btnSavings = findViewById<MaterialButton>(R.id.btnSavings)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)

        // NEW: Initialize Profile Button
        btnProfile = findViewById(R.id.btnProfile)

        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        btnSavings.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#009688"))

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

            override fun onChildDraw(c: android.graphics.Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, active: Boolean) {
                val itemView = vh.itemView
                val paint = android.graphics.Paint()
                if (dX < 0) {
                    paint.color = Color.parseColor("#80FF6B6B")
                    val background = android.graphics.RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    c.drawRect(background, paint)
                    val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                    icon?.let {
                        val margin = (itemView.height - it.intrinsicHeight) / 2
                        it.setBounds(itemView.right - margin - it.intrinsicWidth, itemView.top + margin, itemView.right - margin, itemView.top + margin + it.intrinsicHeight)
                        it.setTint(Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, active)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        viewModel.allExpenses.observe(this) { allRecords ->
            updateDashboardAndList(allRecords, tvCardTitle, tvTotalAmount, cardBackground)
        }

        btnViewIncome.setOnClickListener {
            isIncomeView = true
            viewModel.allExpenses.value?.let { updateDashboardAndList(it, tvCardTitle, tvTotalAmount, cardBackground) }
        }

        btnViewExpense.setOnClickListener {
            isIncomeView = false
            viewModel.allExpenses.value?.let { updateDashboardAndList(it, tvCardTitle, tvTotalAmount, cardBackground) }
        }

        // NEW: Open Settings/Profile when clicking top-right image
        btnProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val btnAccountDetails = findViewById<MaterialButton>(R.id.btnAccountDetails)
        btnAccountDetails.setOnClickListener {
            val allRecords = viewModel.allExpenses.value ?: emptyList()
            val totalIncome = allRecords.filter { it.category.lowercase() in listOf("income", "salary") }.sumOf { it.amount }
            val intent = Intent(this, AccountDetailActivity::class.java)
            intent.putExtra("TOTAL_INCOME", totalIncome)
            startActivity(intent)
        }

        btnAdd.setOnClickListener { showAddDialog() }
        btnReports.setOnClickListener { startActivity(Intent(this, RecordsActivity::class.java)) }
        btnSavings.setOnClickListener {
            startActivity(Intent(this, SetupSavingsActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_account -> startActivity(Intent(this, AccountDetailActivity::class.java))
                R.id.nav_goals -> startActivity(Intent(this, SavingsActivity::class.java))
                R.id.nav_about -> Toast.makeText(this, "About Clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_help -> startActivity(Intent(this,HelpActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupAutoSavingsWorker() {
        val workManager = WorkManager.getInstance(this)
        val periodicRequest = PeriodicWorkRequestBuilder<SavingsWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork("SavingsAutomation", ExistingPeriodicWorkPolicy.KEEP, periodicRequest)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateDashboardAndList(allRecords: List<Expense>, title: TextView, amount: TextView, bg: RelativeLayout) {
        val filteredList = if (isIncomeView) {
            allRecords.filter { it.category.lowercase() in listOf("income", "salary") }
        } else {
            allRecords.filter { it.category.lowercase() !in listOf("income", "salary") }
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
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        rgType.setOnCheckedChangeListener { _, checkedId -> spCategory.visibility = if (checkedId == R.id.rbExpense) View.VISIBLE else View.GONE }
        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amountVal = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val category = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else spCategory.selectedItem.toString().lowercase()
            if (title.isNotEmpty() && amountVal > 0) {
                viewModel.insert(Expense(title = title, amount = amountVal, date = System.currentTimeMillis(), category = category))
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
        loadProfileImage() // NEW: Load the image whenever returning to main
    }

    private fun loadToolbarTheme() {
        val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
        val colorHex = sharedPref.getString("ACCOUNT_COLOR", "#00B0FF")
        try {
            val colorInt = Color.parseColor(colorHex)
            findViewById<RelativeLayout>(R.id.customToolbar)?.setBackgroundColor(colorInt)
            window.statusBarColor = colorInt
        } catch (e: Exception) {
            findViewById<RelativeLayout>(R.id.customToolbar)?.setBackgroundColor(Color.parseColor("#00B0FF"))
        }
    }

    // NEW: Function to load the saved profile picture from EditProfileActivity
    private fun loadProfileImage() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val imageUriString = sharedPref.getString("user_image", null)

        if (imageUriString != null) {
            btnProfile.load(Uri.parse(imageUriString)) {
                crossfade(true)
                placeholder(R.drawable.ic_account)
                error(R.drawable.ic_account)
                transformations(CircleCropTransformation())
                // Ensure tint is removed so the actual photo isn't blue/white
                listener(onSuccess = { _, _ -> btnProfile.imageTintList = null })
            }
        } else {
            btnProfile.setImageResource(R.drawable.ic_account)
            // Restore the white tint for the default icon
            btnProfile.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }
}