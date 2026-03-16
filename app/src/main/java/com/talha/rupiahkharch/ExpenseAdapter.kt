package com.talha.rupiahkharch

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.model.Expense
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(private var expenseList: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var onItemClickListener: (() -> Unit)? = null

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvExpenseTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvExpenseDate)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
    }

    fun getExpenseAt(position: Int): Expense = expenseList[position]

    fun setOnItemClickListener(listener: () -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentExpense = expenseList[position]
        val category = currentExpense.category.lowercase().trim()

        holder.tvTitle.text = currentExpense.title
        holder.itemView.setOnClickListener { onItemClickListener?.invoke() }

        // 1. Amount Styling
        if (category == "salary" || category == "income") {
            holder.tvAmount.text = "+${currentExpense.amount}"
            holder.tvAmount.setTextColor(Color.parseColor("#2ECC71"))
        } else {
            holder.tvAmount.text = "-${currentExpense.amount}"
            holder.tvAmount.setTextColor(Color.parseColor("#E74C3C"))
        }

        // 2. Map the Icon Resource (Added SAVINGS)
        val iconRes = when (category) {
            "shopping"  -> R.drawable.shopping
            "transport" -> R.drawable.car
            "bill"      -> R.drawable.bill
            "fuel"      -> R.drawable.fuel
            "health"    -> R.drawable.health
            "salary", "income" -> R.drawable.salary_24
            "rent"      -> R.drawable.home_24
            "food"      -> R.drawable.fastfood
            "grocery"   -> R.drawable.shopping_cart
            "savings"   -> R.drawable.ic_savings
            else        -> android.R.drawable.ic_menu_help
        }
        holder.ivCategoryIcon.setImageResource(iconRes)

        // 3. Color Filters (Added SAVINGS color)
        when (category) {
            "shopping" -> holder.ivCategoryIcon.setColorFilter(Color.parseColor("#FFB238"), PorterDuff.Mode.SRC_IN)
            "transport" -> holder.ivCategoryIcon.setColorFilter(Color.parseColor("#00B0FF"), PorterDuff.Mode.SRC_IN)
            "bill", "fuel" -> holder.ivCategoryIcon.setColorFilter(Color.parseColor("#D81B60"), PorterDuff.Mode.SRC_IN)
            "health" -> holder.ivCategoryIcon.setColorFilter(Color.parseColor("#009688"), PorterDuff.Mode.SRC_IN)
            "savings" -> holder.ivCategoryIcon.setColorFilter(Color.parseColor("#4CAF50"), PorterDuff.Mode.SRC_IN) // Green for savings
            else -> holder.ivCategoryIcon.clearColorFilter()
        }

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = formatter.format(Date(currentExpense.date))
    }

    override fun getItemCount() = expenseList.size

    fun updateData(newList: List<Expense>) {
        expenseList = newList
        notifyDataSetChanged()
    }
}