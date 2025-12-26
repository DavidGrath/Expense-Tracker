package com.davidgrath.expensetracker.entities.ui

import androidx.annotation.DrawableRes

data class AddTransactionCategory(
    val id: Long,
    val stringId: String?,
    val name: String,
    @DrawableRes val iconId: Int
)