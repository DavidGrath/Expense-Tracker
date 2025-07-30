package com.davidgrath.expensetracker.entities.db

data class CategoryDb(
    val id: Long,
    val profileID: Long,
    val stringID: String?,
    val isCustom: Boolean,
    val name: String?
)
