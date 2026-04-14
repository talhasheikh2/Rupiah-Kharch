package com.talha.rupiahkharch

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils // Required for animation
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

    // Track if it's the first time data is loaded to prevent
    // the animation from playing every time you type in search
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        rvRecords = findViewById(R.id.rvRecords)
        etSearch = findViewById(R.id.etSearch)

        adapter = RecordsHistoryAdapter(emptyList())
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = adapter

        viewModel.allExpenses.observe(this) { allRecords ->
            if (allRecords != null) {
                adapter.updateData(allRecords)

                // TRIGGER ANIMATION ONLY ON FIRST LOAD
                if (isFirstLoad && allRecords.isNotEmpty()) {
                    runLayoutAnimation(rvRecords)
                    isFirstLoad = false
                }

                setupSearch(allRecords)
            }
        }
    }

    // New helper function to force the animation
    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_right)

        recyclerView.layoutAnimation = controller
        recyclerView.adapter?.notifyDataSetChanged()
        recyclerView.scheduleLayoutAnimation()
    }

    private fun setupSearch(fullList: List<com.talha.rupiahkharch.model.Expense>) {
        // Remove existing listeners if any to prevent duplicate triggers
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = fullList.filter {
                    it.title.lowercase().contains(query) || it.category.lowercase().contains(query)
                }
                adapter.updateData(filteredList)

                // Note: We DON'T call scheduleLayoutAnimation here
                // so the list stays stable while the user is typing.
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}