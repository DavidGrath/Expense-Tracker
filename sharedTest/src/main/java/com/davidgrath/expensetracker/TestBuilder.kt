package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.entities.TransactionMode
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionDbBuilder
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDbBuilder
import java.math.BigDecimal

class TestBuilder {
    companion object {
        fun defaultTransactionBuilder(accountId: Long, amount: BigDecimal): TransactionDbBuilder {
            val builder = TransactionDbBuilder()
                .amount(amount)
                .accountId(accountId).currencyCode("USD")
                .debitOrCredit(true)
                .note(null)
                .sellerId(null)
                .sellerLocationId(null)
                .createdAt("2025-06-30T08:00:00")
                .createdAtOffset("Z")
                .createdAtTimezone("UTC")
                .ordinal(0)
                .mode(TransactionMode.Other)
                .datedAt("2025-06-30")
                .datedAtTime("08:00:00")
            return builder
        }

        fun defaultTransaction(accountId: Long, amount: BigDecimal): TransactionDb {
            return defaultTransactionBuilder(accountId, amount).build()
        }

        fun defaultTransactionItemBuilder(transactionId: Long, price: BigDecimal, primaryCategoryId: Long): TransactionItemDbBuilder {
            val builder = TransactionItemDbBuilder()
                .transactionId(transactionId)
                .amount(price)
                .description("Description")
                .variation("")
                .primaryCategoryId(primaryCategoryId)
                .createdAt("2025-06-30T08:00:00")
                .createdAtOffset("Z")
                .createdAtTimezone("UTC")
            return builder
        }

    }
}