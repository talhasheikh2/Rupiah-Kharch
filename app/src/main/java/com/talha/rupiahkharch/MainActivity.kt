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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

    // UI References
    private lateinit var btnProfile: ImageView
    private lateinit var tvEmptyState: TextView
    private lateinit var balanceCard: com.google.android.material.card.MaterialCardView

    // Animation variables
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var shakeRunnable: Runnable

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications disabled. You won't see savings alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- PERSISTENCE LOGIC START ---
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("dark_mode", false)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // --- PERSISTENCE LOGIC END ---

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

        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnProfile = findViewById(R.id.btnProfile)
        balanceCard = findViewById(R.id.balanceCard)

        // Setup Shake Animation Loop
        val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
        shakeRunnable = Runnable {
            balanceCard.startAnimation(shakeAnim)
            handler.postDelayed(shakeRunnable, 4000)
        }

        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        btnSavings.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#009688"))

        adapter = ExpenseAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // --- UPDATED LISTENERS ---
        adapter.setOnItemClickListener {
            Toast.makeText(this, "Hold record to Edit or Swipe left to Delete ⬅️", Toast.LENGTH_SHORT).show()
        }

        // 1. Long Click to Edit Functionality
        adapter.setOnItemLongClickListener { expense ->
            showEditDialog(expense)
        }

        // Setup Swipe-to-Delete with Confirmation Dialog
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expenseToDelete = adapter.getExpenseAt(position)

                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Delete Record")
                builder.setMessage("Are you sure you want to delete '${expenseToDelete.title}'?")

                builder.setPositiveButton("Delete") { _, _ ->
                    viewModel.delete(expenseToDelete)
                    Toast.makeText(this@MainActivity, "${expenseToDelete.title} deleted", Toast.LENGTH_SHORT).show()
                }

                builder.setNegativeButton("Cancel") { dialog, _ ->
                    adapter.notifyItemChanged(position)
                    dialog.dismiss()
                }

                builder.setCancelable(false)
                builder.show()
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
                R.id.nav_about -> startActivity(Intent(this, DonutChartActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_help -> startActivity(Intent(this, HelpActivity::class.java))

                // --- ADD THIS LOGOUT CASE ---
                R.id.nav_logout -> {
                    showLogoutConfirmation()
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    // --- NEW EDIT DIALOG FUNCTION ---
    private fun showEditDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgType)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val categories = arrayOf("Food", "Rent", "Shopping", "Transport", "Health","Bill","Fuel", "Other")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        // 1. Pre-fill data
        etTitle.setText(expense.title)
        etAmount.setText(expense.amount.toString())
        btnSave.text = "Update"

        if (expense.category.lowercase() == "income" || expense.category.lowercase() == "salary") {
            rgType.check(R.id.rbIncome)
            spCategory.visibility = View.GONE
        } else {
            rgType.check(R.id.rbExpense)
            spCategory.visibility = View.VISIBLE
            val spinnerPosition = categories.indexOfFirst { it.lowercase() == expense.category.lowercase() }
            if (spinnerPosition != -1) spCategory.setSelection(spinnerPosition)
        }

        rgType.setOnCheckedChangeListener { _, checkedId ->
            spCategory.visibility = if (checkedId == R.id.rbExpense) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amountVal = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val category = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else spCategory.selectedItem.toString().lowercase()

            if (title.isNotEmpty() && amountVal > 0) {
                // 2. Update existing object
                expense.title = title
                expense.amount = amountVal
                expense.category = category
                // Date remains same as original creation unless you want to update it to System.currentTimeMillis()

                viewModel.update(expense) // Ensure your ViewModel has an update function
                dialog.dismiss()
                Toast.makeText(this, "Record Updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter name and amount", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
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

        if (filteredList.isEmpty()) {
            if (tvEmptyState.visibility == View.GONE) {
                tvEmptyState.visibility = View.VISIBLE
                val slideAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade)
                tvEmptyState.startAnimation(slideAnim)
            }
            tvEmptyState.text = if (isIncomeView) "No Income records added yet" else "No Expense records added yet"
        } else {
            tvEmptyState.visibility = View.GONE
            tvEmptyState.clearAnimation()
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
        loadProfileImage()
        handler.postDelayed(shakeRunnable, 4000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(shakeRunnable)
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

    private fun loadProfileImage() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val savedImageData = sharedPref.getString("user_image", null)

        if (savedImageData != null) {
            // Check if it's a Base64 string (from our new Cloud Sync logic)
            if (savedImageData.length > 200) {
                try {
                    val imageBytes = android.util.Base64.decode(savedImageData, android.util.Base64.DEFAULT)
                    btnProfile.load(imageBytes) {
                        crossfade(true)
                        placeholder(R.drawable.ic_account)
                        error(R.drawable.ic_account)
                        transformations(CircleCropTransformation())
                        listener(onSuccess = { _, _ -> btnProfile.imageTintList = null })
                    }
                } catch (e: Exception) {
                    // Fallback to loading as URI if decoding fails
                    btnProfile.load(savedImageData) {
                        crossfade(true)
                        placeholder(R.drawable.ic_account)
                        error(R.drawable.ic_account)
                        transformations(CircleCropTransformation())
                        listener(onSuccess = { _, _ -> btnProfile.imageTintList = null })
                    }
                }
            } else {
                // It's a standard local URI
                btnProfile.load(Uri.parse(savedImageData)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_account)
                    error(R.drawable.ic_account)
                    transformations(CircleCropTransformation())
                    listener(onSuccess = { _, _ -> btnProfile.imageTintList = null })
                }
            }
        } else {
            // No image found: show default icon
            btnProfile.setImageResource(R.drawable.ic_account)
            btnProfile.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? Your data is saved safely in the cloud.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // 1. Sign out of Firebase FIRST
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        // 2. Clear all Local Preferences
        getSharedPreferences("UserProfile", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("AccountPrefs", Context.MODE_PRIVATE).edit().clear().apply()

        // 3. Clear the Database and navigate ONLY when finished
        // This replaces the Handler/Timer logic
        viewModel.logoutClearData {
            // This block runs ONLY after the database is 100% empty
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}