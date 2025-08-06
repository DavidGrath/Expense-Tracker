package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

@Entity
data class TransactionDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val accountId: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val cashOrCredit: Boolean, //TODO Possibly revise to enum Mode {Cash,Transfer,POS,etc}
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String,
    val datedAt: String,
    val datedAtOffset: String,
    val datedAtTimezone: String
): Comparable<TransactionDb> {
    override fun compareTo(other: TransactionDb): Int {
        return this.id!!.compareTo(other.id!!)
    }
}