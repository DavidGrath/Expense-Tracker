package com.davidgrath.expensetracker.entities.ui

data class CategoryWithStatsUi(
    val id: Long,
    val profileId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String,
    val icon: String,
    val transactionCount: Int,
    val itemCount: Int, //TODO Account for reductions
    val iconId: Int
)