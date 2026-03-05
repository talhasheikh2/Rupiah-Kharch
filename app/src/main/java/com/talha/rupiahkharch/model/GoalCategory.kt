package com.talha.rupiahkharch.model

data class GoalCategory(
    val id: Int,
    val name: String,
    val iconRes: Int,
    var isSelected: Boolean = false
)