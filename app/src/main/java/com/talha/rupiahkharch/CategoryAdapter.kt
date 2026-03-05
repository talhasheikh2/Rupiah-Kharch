package com.talha.rupiahkharch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.talha.rupiahkharch.model.GoalCategory

class CategoryAdapter(
    private val categories: List<GoalCategory>,
    private val onCategorySelected: (GoalCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Default to the first item (e.g., Car) as selected
    private var selectedPosition = 0

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.categoryCard)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val label: TextView = view.findViewById(R.id.tvLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.label.text = category.name
        holder.icon.setImageResource(category.iconRes)

        // Logic: Apply Teal border and subtle lift if selected
        if (position == selectedPosition) {
            holder.card.strokeWidth = 6 // Thicker border for better visibility
            holder.card.strokeColor = Color.parseColor("#009688") // Vibrant Teal
            holder.card.cardElevation = 6f
            holder.card.alpha = 1.0f
        } else {
            holder.card.strokeWidth = 0
            holder.card.cardElevation = 0f
            holder.card.alpha = 0.7f // Dim unselected items slightly for focus
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition

                // Refresh the old and new items to move the border
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onCategorySelected(category)
            }
        }
    }

    override fun getItemCount() = categories.size

    // Helper function to get the currently selected category object
    fun getSelectedCategory(): GoalCategory = categories[selectedPosition]
}