package com.davidgrath.expensetracker.entities.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jilt.Builder;
import org.jspecify.annotations.NullMarked;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;

import java.math.BigDecimal;

@Builder
@NullMarked
@Entity(indices = [Index(value = ["datedAt"])])
data class TransactionDb(
    @PrimaryKey(autoGenerate = true)
    var id: Long?,
    val accountId: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val referenceNumber: String?,
    val debitOrCredit: Boolean,
    val isCashless: Boolean, //TODO Possibly revise to enum Mode {Cash,Transfer,POS,etc}
    /**
     * Maximum 500 NFC Normalized code points
     */
    val note: String?,
    val sellerID: Long?,
    val sellerLocationId: Long?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String,
    /**
     * To allow for reordering of transactions as well as imposing an ordering on timeless transactions
     */
    val ordinal: Int,
    val datedAt: String,

    val datedAtTime: String?,
    val datedAtOffset: String?,
    val datedAtTimezone: String?,
): Comparable<TransactionDb> {


    override fun compareTo(other: TransactionDb): Int {
        return this.id!!.compareTo(other.id!!)
    }
    @Ignore
    fun getDatedLocalDateTime(): LocalDateTime? {
        if(datedAtTime == null) {
            return null
        }
        val utcDate = LocalDate.parse(datedAt)
        val utcTime = LocalTime.parse(datedAtTime)
        val offset = ZoneOffset.of(datedAtOffset)
        val utcDateTime = utcDate.atTime(utcTime)
        val offsetDateTime = utcDateTime.atOffset(offset)
        val localDateTime = offsetDateTime.toLocalDateTime()
        return localDateTime
    }
}