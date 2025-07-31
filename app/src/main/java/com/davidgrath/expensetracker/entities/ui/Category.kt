package com.davidgrath.expensetracker.entities.ui

import android.util.Log
import androidx.annotation.DrawableRes
import com.davidgrath.expensetracker.Utils

data class Category(
    val id: Long,
    val stringId: String,
    val name: String,
    @DrawableRes val iconId: Int
) {
    companion object {
        val TEMP_DEFAULT_CATEGORIES = Utils.CORE_CATEGORIES.mapIndexed { index, s ->
            Category(index.toLong(), s, Utils.CATEGORY_NAMES_DEFAULT[s]!!, Utils.CATEGORY_IDS_DEFAULT[s]!!)
        }
    }
}
