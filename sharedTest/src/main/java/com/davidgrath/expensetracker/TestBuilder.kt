package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionDbBuilder
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import java.math.BigDecimal

class TestBuilder {
    companion object {
        fun defaultTransactionBuilder(amount: BigDecimal = BigDecimal.ZERO): TransactionDbBuilder {
            val builder = TransactionDbBuilder()
                .amount(amount)
                .accountId(0).currencyCode("USD")
                .isCashless(false)
                .note(null)
                .sellerID(null)
                .sellerLocationId(null)
                .createdAt("2025-08-18T03:00:00")
                .createdAtOffset("+05:00")
                .createdAtTimezone("America/New_York")
                .ordinal(0)
                .datedAtTime("03:00:00")
                .datedAtOffset("+05:00")
                .datedAtTimezone("America/New_York")
            return builder
        }

        fun defaultTransaction(amount: BigDecimal): TransactionDb {
            return defaultTransactionBuilder(amount).build()
        }


    }
}