package com.talha.rupiahkharch

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel

class RecordsActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: RecordsHistoryAdapter
    private lateinit var rvRecords: RecyclerView
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records) // Ensure this matches your XML filename

        rvRecords = findViewById(R.id.rvRecords)
        etSearch = findViewById(R.id.etSearch)

        // Initialize with the NEW Adapter
        adapter = RecordsHistoryAdapter(emptyList())
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = adapter
        // 3. Observe Data from ViewModel
        viewModel.allExpenses.observe(this) { allRecords ->
            adapter.updateData(allRecords)

            // 4. Setup Search/Filter Logic
            setupSearch(allRecords)
        }
    }

    private fun setupSearch(fullList: List<com.talha.rupiahkharch.model.Expense>) {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = fullList.filter {
                    it.title.lowercase().contains(query) || it.category.lowercase().contains(query)
                }
                adapter.updateData(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}