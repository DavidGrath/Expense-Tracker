package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.entities.ui.Transaction
import com.davidgrath.expensetracker.entities.ui.TransactionItem

class Utils {
    companion object {
        val CORE_CATEGORIES = setOf(
            "food",
            "rent",
            "utilities",
            "transportation",
            "healthcare",
            "debt",
            "childcare",
            "household",
            "entertainment",
            "self_care",
            "clothing",
            "education",
            "gifts_and_donations",
            "emergency",
            "savings",
            "fitness",
            "miscellaneous",
        )
        val CATEGORY_NAMES_DEFAULT = mapOf(
            "food" to "Food",
            "rent" to "Housing",
            "utilities" to "Utilities",
            "transportation" to "Transportation",
            "healthcare" to "Healthcare",
            "debt" to "Debt",
            "childcare" to "Childcare",
            "household" to "Household",
            "entertainment" to "Entertainment",
            "self_care" to "Self-care",
            "clothing" to "Clothing",
            "education" to "Education",
            "gifts_and_donations" to "Gifts and donations",
            "emergency" to "Emergency",
            "savings" to "Savings",
            "fitness" to "Fitness",
            "miscellaneous" to "Miscellaneous",
        )
        val CATEGORY_IDS_DEFAULT = mapOf(
            "food" to R.drawable.baseline_restaurant_24,
            "rent" to R.drawable.baseline_home_24,
            "utilities" to R.drawable.baseline_construction_24,
            "transportation" to R.drawable.baseline_directions_car_24,
            "healthcare" to R.drawable.baseline_medical_services_24,
            "debt" to R.drawable.baseline_category_24,
            "childcare" to R.drawable.baseline_category_24,
            "household" to R.drawable.baseline_category_24,
            "entertainment" to R.drawable.baseline_category_24,
            "self_care" to R.drawable.baseline_category_24,
            "clothing" to R.drawable.baseline_category_24,
            "education" to R.drawable.baseline_category_24,
            "gifts_and_donations" to R.drawable.baseline_category_24,
            "emergency" to R.drawable.baseline_category_24,
            "savings" to R.drawable.baseline_category_24,
            "fitness" to R.drawable.baseline_category_24,
            "miscellaneous" to R.drawable.baseline_category_24,
        )
    }
}

fun transactionsToTransactionItems(transactions: List<Transaction>): List<TransactionItem> {
    val itemsList = arrayListOf<TransactionItem>()
    for(transaction in transactions) {
        itemsList.add(TransactionItem(true, transaction, null))
        for(item in transaction.items) {
            itemsList.add(TransactionItem(false, null, item))
        }
    }
    return itemsList
}