package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.talha.rupiahkharch.model.Expense
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel

class EditAccountActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var etName: EditText
    private lateinit var etNumber: EditText
    private lateinit var spColor: Spinner

    private var currentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        // 1. Initialize UI
        etName = findViewById(R.id.etAccountName)
        etNumber = findViewById(R.id.etAccountNumber)
        spColor = findViewById(R.id.spColor)

        val btnSave = findViewById<ImageView>(R.id.btnSave)
        val btnDelete = findViewById<ImageView>(R.id.btnDelete)
        val btnCancel = findViewById<ImageView>(R.id.btnCancel)

        // 2. ViewModel
        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        // 3. Setup UI components
        setupColorSpinner()
        loadDataFromStorage() // UPDATED: Loading from both Intent and Prefs

        // 4. Listeners
        btnCancel.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnSave.setOnClickListener {
            updateAccount()
        }

        spColor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedColorHex = parent?.getItemAtPosition(position).toString()
                updateUIColors(selectedColorHex)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupColorSpinner() {
        val colorList = listOf(
            "#E74C3C", "#FF1744", "#FF5252", "#F06292",
            "#AD1457", "#6A1B9A", "#AB47BC", "#BA68C8", "#00695C"
        )
        val colorAdapter = ColorSpinnerAdapter(this, colorList)
        spColor.adapter = colorAdapter
    }

    private fun loadDataFromStorage() {
        // Load the persistent data from SharedPreferences
        val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
        val savedName = sharedPref.getString("ACCOUNT_NAME", "Edit Account Name")
        val savedColor = sharedPref.getString("ACCOUNT_COLOR", "#00B0FF")

        // Load ID and Amount from Intent (since these come from the transaction logic)
        currentId = intent.getIntExtra("ACCOUNT_ID", -1)
        val amount = intent.getDoubleExtra("ACCOUNT_AMOUNT", 0.0)

        etName.setText(savedName)
        etNumber.setText(amount.toString())

        // Logic to set spinner to saved color if needed can go here
    }

    private fun updateAccount() {
        val name = etName.text.toString().trim()
        val selectedColor = spColor.selectedItem.toString()

        if (name.isNotEmpty()) {
            // 1. SAVE TO PREFERENCES (Separated from Database records)
            val sharedPref = getSharedPreferences("AccountPrefs", MODE_PRIVATE)
            sharedPref.edit().apply {
                putString("ACCOUNT_NAME", name)
                putString("ACCOUNT_COLOR", selectedColor)
                apply()
            }

            // 2. PREPARE RESULT
            val resultIntent = Intent()
            resultIntent.putExtra("UPDATED_NAME", name)
            resultIntent.putExtra("UPDATED_COLOR", selectedColor)
            setResult(RESULT_OK, resultIntent)

            // NO viewModel.update(updatedExpense) here!
            // This prevents the Income/Salary list item from changing its name.

            finish()
        } else {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        if (currentId == -1) return

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Delete") { _, _ ->
                // This only deletes the specific transaction record from the list
                val expenseToDelete = Expense(id = currentId, title = "", amount = 0.0, date = 0, category = "")
                viewModel.delete(expenseToDelete)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUIColors(colorHex: String) {
        try {
            val colorInt = android.graphics.Color.parseColor(colorHex)
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.editToolbar)?.setBackgroundColor(colorInt)
            window.statusBarColor = colorInt
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}