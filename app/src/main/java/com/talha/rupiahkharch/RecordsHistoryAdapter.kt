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
        // Points to the NEW Pill Design XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val expense = expenses[position]
            holder.tvRecordTitle.text = expense.title

            // Logic for Amount Color and Prefix (+/-)
            val isIncome = expense.category.lowercase() in listOf("income", "salary", "gaji")
            holder.tvRecordAmount.text = if (isIncome) "+$${expense.amount}" else "-$${expense.amount}"
            holder.tvRecordAmount.setTextColor(Color.parseColor(if (isIncome) "#4CAF50" else "#FF5252"))

            // Set specific colors for the Pill Indicator and Icon Background
            val (mainColor, lightBg) = when (expense.category.lowercase()) {
                "income", "salary",  -> "#4CAF50" to "#E8F5E9"
                "food" -> "#FFB300" to "#FFF8E1"
                "rent" -> "#FF0000" to "#ebcacd"
                "transport",  -> "#00B0FF" to "#E1F5FE"
                "bill"-> "#f08b97" to "#FFF5F7"
                 "fuel" -> "#f7aa5a" to "#fffdfe"
                "health" -> "#009688" to "#E0F2F1"
                "grocery" -> "#FFB74D" to "#FFF3E0"
                "shopping" -> "#9575CD" to "#E1BEE7"
                "other" -> "#2E7D32" to "#E8F5E9"
                else -> "#00B0FF" to "#E1F5FE"
            }

            holder.viewIndicator.setBackgroundColor(Color.parseColor(mainColor))
            holder.iconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor(lightBg))
            holder.ivRecordIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(mainColor))

            // Set Icon based on category
            val iconRes = when (expense.category.lowercase()) {
                "food" -> R.drawable.fastfood
                "rent"-> R.drawable.home_24
                "transport" -> R.drawable.car
                "bill" -> R.drawable.bill
                 "fuel" -> R.drawable.fuel
                "health" -> R.drawable.health
                "grocery" -> R.drawable.shopping_cart
                "shopping" -> R.drawable.shopping
                "salary", "income" -> R.drawable.salary_24 // replace with your actual salary icon name
                else -> R.drawable.ic_check
            }
            holder.ivRecordIcon.setImageResource(iconRes)
        }

    override fun getItemCount() = expenses.size

    fun updateData(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }
}