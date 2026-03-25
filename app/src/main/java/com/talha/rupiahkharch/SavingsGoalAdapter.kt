package com.talha.rupiahkharch.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.talha.rupiahkharch.R
import com.talha.rupiahkharch.model.SavingsGoal
import java.util.Locale

class SavingsGoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onItemClick: (SavingsGoal) -> Unit,
    private val onPauseClick: (SavingsGoal) -> Unit // NEW: Handle Pause/Resume button
) : RecyclerView.Adapter<SavingsGoalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val savingsProgress: ProgressBar = view.findViewById(R.id.savingsProgress)
        val ivGoalIcon: ImageView = view.findViewById(R.id.ivGoalIcon)
        val tvSavedAmount: TextView = view.findViewById(R.id.tvSavedAmount)
        val tvRemainingAmount: TextView = view.findViewById(R.id.tvRemainingAmount)
        val tvGoalTitle: TextView = view.findViewById(R.id.tvGoalTitle)
        val tvTargetTotal: TextView = view.findViewById(R.id.tvTargetTotal)
        val btnPauseResume: ImageButton = view.findViewById(R.id.btnPauseResume) // NEW
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge) // NEW
    }

    fun updateList(newGoals: List<SavingsGoal>) {
        this.goals = newGoals
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goals[position]

        val currentSaved = goal.savedAmount
        val remaining = goal.getRemaining()
        val progress = goal.getProgress()
        val llShortfallAlert: LinearLayout = holder.itemView.findViewById(R.id.llShortfallAlert)

        holder.tvGoalTitle.text = goal.title
        holder.tvTargetTotal.text = "Target: Rs. ${String.format(Locale.US, "%,.0f", goal.targetAmount)}"
        holder.tvSavedAmount.text = "Saved: Rs. ${String.format(Locale.US, "%,.0f", currentSaved)}"
        holder.tvRemainingAmount.text = "Left: Rs. ${String.format(Locale.US, "%,.0f", remaining)}"
        holder.savingsProgress.progress = progress

        // ICON SETTING
        if (goal.iconRes != 0) {
            holder.ivGoalIcon.setImageResource(goal.iconRes)
        } else {
            holder.ivGoalIcon.setImageResource(R.drawable.cars)
        }


        if (goal.wasSkippedLowBalance && !goal.isPaused) {
            llShortfallAlert.visibility = View.VISIBLE
        } else {
            llShortfallAlert.visibility = View.GONE
        }
        // COLOR LOGIC
        val colorForThisCard = getColorForGoal(goal.id)
        holder.savingsProgress.progressTintList = ColorStateList.valueOf(colorForThisCard)
        holder.tvGoalTitle.setTextColor(colorForThisCard)

        // --- PAUSE / RESUME LOGIC ---
        if (goal.isPaused) {
            // UI for Paused State
            holder.btnPauseResume.setImageResource(android.R.drawable.ic_media_play) // Standard Play Icon
            holder.tvStatusBadge.visibility = View.VISIBLE
            holder.itemView.alpha = 0.6f // Fade the card
        } else {
            // UI for Active State
            holder.btnPauseResume.setImageResource(android.R.drawable.ic_media_pause) // Standard Pause Icon
            holder.tvStatusBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f // Full visibility
        }

        // --- CLICK LISTENERS ---
        holder.btnPauseResume.setOnClickListener {
            onPauseClick(goal)
        }

        holder.itemView.setOnClickListener {
            onItemClick(goal)
        }
    }

    private fun getColorForGoal(goalId: Int): Int {
        val colors = listOf(
            "#FF5722", "#4CAF50", "#2196F3", "#9C27B0",
            "#FFC107", "#00BCD4", "#E91E63", "#3F51B5",
            "#009688", "#795548", "#607D8B"
        )
        val index = if (goalId > 0) goalId % colors.size else 0
        return Color.parseColor(colors[index])
    }

    override fun getItemCount() = goals.size
}