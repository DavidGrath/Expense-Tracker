package com.davidgrath.expensetracker.entities.ui

import androidx.annotation.DrawableRes
import com.davidgrath.expensetracker.Utils

data class CategoryUi(
    val id: Long,
    val stringId: String?,
    val name: String,
    @DrawableRes val iconId: Int
)
