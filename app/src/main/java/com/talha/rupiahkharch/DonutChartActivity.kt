package com.talha.rupiahkharch

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.ChipGroup
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel

class DonutChartActivity : AppCompatActivity() {

    private lateinit var donutChart: PieChart
    private lateinit var legendContainer: ChipGroup
    private val viewModel: ExpenseViewModel by viewModels()

    // Categorization color map (Exact matches to MainActivity)
    private val categoryColors = mapOf(
        "food"      to Color.parseColor("#00C9FF"),
        "rent"      to Color.parseColor("#0F3A7D"),
        "shopping"  to Color.parseColor("#FACB0C"),
        "health"    to Color.parseColor("#ED1E79"),
        "income"    to Color.parseColor("#99E549"),
        "transport" to Color.parseColor("#3F007F"),
        "bill"      to Color.parseColor("#C1272D"),
        "fuel"      to Color.parseColor("#6A0F49"),
        "savings"   to Color.parseColor("#009688"),
        "other"     to Color.parseColor("#424242")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donut_chart)

        donutChart = findViewById(R.id.donutChart)
        legendContainer = findViewById(R.id.legendContainer)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        setupChartConfig()

        viewModel.allExpenses.observe(this) { allRecords ->
            if (!allRecords.isNullOrEmpty()) {
                val totalsMap = allRecords.groupBy { it.category.lowercase() }
                    .mapValues { it.value.sumOf { exp -> exp.amount }.toFloat() }

                updateChartData(totalsMap)
                setupCustomLegend(totalsMap.keys)
            } else {
                donutChart.clear()
                legendContainer.removeAllViews()
            }
        }
    }

    private fun setupChartConfig() {
        donutChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 45f
            transparentCircleRadius = 0f
            setDrawEntryLabels(false)
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1200)
        }
    }

    private fun updateChartData(categoryTotals: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for ((name, total) in categoryTotals) {
            if (total > 0) {
                entries.add(PieEntry(total, ""))
                colors.add(categoryColors[name.lowercase()] ?: Color.LTGRAY)
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            setDrawValues(false)
        }

        donutChart.data = PieData(dataSet)
        donutChart.invalidate()
    }

    private fun setupCustomLegend(activeCategories: Set<String>) {
        legendContainer.removeAllViews()
        for (category in activeCategories) {
            val color = categoryColors[category] ?: Color.GRAY
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_custom_legend, legendContainer, false)

            itemView.findViewById<View>(R.id.vColorBox).setBackgroundColor(color)
            itemView.findViewById<TextView>(R.id.tvCategoryName).text = category.replaceFirstChar { it.uppercase() }

            legendContainer.addView(itemView)
        }
    }
}