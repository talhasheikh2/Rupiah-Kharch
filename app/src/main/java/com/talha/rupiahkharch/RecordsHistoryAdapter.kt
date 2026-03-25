package com.talha.rupiahkharch

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.model.Expense

class RecordsHistoryAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<RecordsHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewIndicator: View = view.findViewById(R.id.viewIndicator)
        val iconContainer: View = view.findViewById(R.id.iconContainer)
        val ivRecordIcon: ImageView = view.findViewById(R.id.ivRecordIcon)
        val tvRecordTitle: TextView = view.findViewById(R.id.tvRecordTitle)
        val tvRecordAmount: TextView = view.findViewById(R.id.tvRecordAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]
        holder.tvRecordTitle.text = expense.title

        // Use trim() to avoid issues with extra spaces
        val category = expense.category.lowercase().trim()
        val title = expense.title.lowercase().trim()

        // Logic for Amount Color and Prefix (+/-)
        val isIncome = category in listOf("income", "salary")
        holder.tvRecordAmount.text = if (isIncome) "+Rs. ${expense.amount}" else "-Rs. ${expense.amount}"
        holder.tvRecordAmount.setTextColor(Color.parseColor(if (isIncome) "#4CAF50" else "#FF5252"))

        // 1. DETERMINE COLORS (Indicator bar and Icon Background)
        val (mainColor, lightBg) = when (category) {
            "income", "salary" -> "#4CAF50" to "#E8F5E9"
            "food" -> "#FFB300" to "#FFF8E1"
            "rent" -> "#FF0000" to "#ebcacd"
            "transport" -> "#00B0FF" to "#E1F5FE"
            "bill" -> "#f08b97" to "#FFF5F7"
            "fuel" -> "#f7aa5a" to "#fffdfe"
            "health" -> "#009688" to "#E0F2F1"
            "grocery" -> "#FFB74D" to "#FFF3E0"
            "shopping" -> "#9575CD" to "#E1BEE7"
            "savings" -> "#009688" to "#E0F2F1" // Teal theme for savings
            else -> "#00B0FF" to "#E1F5FE"
        }

        // 2. DETERMINE ICON (Including Savings Logic)
        val iconRes = when {
            category == "food" -> R.drawable.fastfood
            category == "rent" -> R.drawable.home_24
            category == "transport" -> R.drawable.car
            category == "bill" -> R.drawable.bill
            category == "fuel" -> R.drawable.fuel
            category == "health" -> R.drawable.health
            category == "grocery" -> R.drawable.shopping_cart
            category == "shopping" -> R.drawable.shopping
            category in listOf("salary", "income") -> R.drawable.salary_24

            // SPECIAL SAVINGS LOGIC: Check title for specific goals
            category == "savings" || title.contains("savings") -> {
                when {
                    title.contains("car") -> R.drawable.cars
                    title.contains("house") -> R.drawable.house
                    title.contains("emergency") -> R.drawable.emergency
                    title.contains("travel") -> R.drawable.travelling
                    title.contains("school") || title.contains("education") -> R.drawable.education
                    title.contains("business") -> R.drawable.business
                    title.contains("bike") || title.contains("motor") -> R.drawable.motorbike
                    else -> R.drawable.ic_check // Default fallback
                }
            }
            else -> R.drawable.ic_check
        }

        // 3. APPLY STYLES
        holder.viewIndicator.setBackgroundColor(Color.parseColor(mainColor))
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor(lightBg))

        // FIX: If it's a savings icon, remove the tint so it shows original colors/details.
        // If it's a regular category, apply the mainColor tint.
        if (category == "savings" || title.contains("savings")) {
            holder.ivRecordIcon.imageTintList = null
        } else {
            holder.ivRecordIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(mainColor))
        }

        holder.ivRecordIcon.setImageResource(iconRes)
    }

    override fun getItemCount() = expenses.size

    fun updateData(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }
}