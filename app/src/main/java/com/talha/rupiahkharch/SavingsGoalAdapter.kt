package com.talha.rupiahkharch.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.R
import com.talha.rupiahkharch.model.SavingsGoal

class SavingsGoalAdapter(private val goals: List<SavingsGoal>) :
    RecyclerView.Adapter<SavingsGoalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val savingsProgress: ProgressBar = view.findViewById(R.id.savingsProgress)
        val ivGoalIcon: ImageView = view.findViewById(R.id.ivGoalIcon)
        val tvSavedAmount: TextView = view.findViewById(R.id.tvSavedAmount)
        val tvRemainingAmount: TextView = view.findViewById(R.id.tvRemainingAmount)
        val tvGoalTitle: TextView = view.findViewById(R.id.tvGoalTitle)
        val tvTargetTotal: TextView = view.findViewById(R.id.tvTargetTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goals[position]

        // 1. Set Text Data
        holder.tvGoalTitle.text = goal.title
        holder.tvTargetTotal.text = String.format("Rs. %,.0f", goal.targetAmount)
        holder.tvSavedAmount.text = String.format("Rs. %,.0f", goal.savedAmount)
        holder.tvRemainingAmount.text = String.format("Rs. %,.0f", goal.getRemaining())
        // 2. Calculate and Set Progress
        holder.savingsProgress.progress = goal.getProgress()

        // 3. Set Icon
        holder.ivGoalIcon.setImageResource(goal.iconRes)

        // 4. Apply Dynamic Colors
        val color = Color.parseColor(goal.colorHex)
        holder.savingsProgress.progressTintList = ColorStateList.valueOf(color)
        holder.tvGoalTitle.setTextColor(color) // Makes the title match the theme

        // Optional: Tint the icon to match the theme
//        holder.ivGoalIcon.imageTintList = ColorStateList.valueOf(color)
    }

    override fun getItemCount() = goals.size
}