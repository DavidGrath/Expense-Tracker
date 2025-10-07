package com.davidgrath.expensetracker.entities.ui

data class AccountUi(
    val id: Long,
    val profileId: Long,
    val currencyCode: String,
    val currencyDisplayName: String,
    val name: String,
)