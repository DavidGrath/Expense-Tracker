package com.davidgrath.expensetracker

import android.app.Application
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.PurchaseItemDb
import com.davidgrath.expensetracker.entities.db.TransactionDb

class ExpenseTracker: Application() {

    var incrementId = 0L
    data class TempDb(
        val transactions: MutableList<TransactionDb>,
        val purchaseItems: MutableList<PurchaseItemDb>,
        val categories: MutableList<CategoryDb> = Utils.CORE_CATEGORIES.mapIndexed { index, s -> CategoryDb(index.toLong(), 0, s, false, null) }.toMutableList()
    )
    interface TempDbListener {
        fun onDbChanged(tempDb: TempDb)
    }
    val tempListeners = mutableListOf<TempDbListener>()

    var tempDb = TempDb(mutableListOf(), mutableListOf())
    fun addTransaction(transactionDb: TransactionDb): Long {
        tempDb.transactions += transactionDb.copy(id = ++incrementId)
        for(listener in tempListeners) {
            listener.onDbChanged(tempDb)
        }
        return incrementId
    }

    fun addPurchaseItem(purchaseItemDb: PurchaseItemDb) {
        tempDb.purchaseItems += purchaseItemDb.copy(id = ++incrementId)
        for(listener in tempListeners) {
            listener.onDbChanged(tempDb)
        }
    }
}