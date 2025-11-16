package com.davidgrath.expensetracker.entities.ui

data class SellerLocationUi(
    val id: Long,
    val sellerId: Long,
    val location: String,
    val isVirtual: Boolean,
    val longitude: Double?,
    val latitude: Double?,
    val address: String?,
)