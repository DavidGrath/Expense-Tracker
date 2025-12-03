package com.davidgrath.expensetracker.entities.db;

import androidx.room.Entity;
import androidx.room.ForeignKey
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.TransactionMode

import org.jilt.Builder;
import org.jspecify.annotations.NullMarked;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.math.BigDecimal;

/**
 * The basis for everything else in the app
 */
@Builder
@Entity(
    indices = [
        Index(value = ["accountId", "datedAt", "ordinal"], unique = true),
    Index(value = ["sellerId"]),
    Index(value = ["sellerLocationId"]),
              ], // TODO Apparently I can't use function-based indexes. Rework other 'date' queries
    foreignKeys = [
        ForeignKey(AccountDb::class, parentColumns = ["id"], childColumns = ["accountId"]),
        ForeignKey(SellerDb::class, parentColumns = ["id"], childColumns = ["sellerId"]),
        ForeignKey(SellerLocationDb::class, parentColumns = ["id"], childColumns = ["sellerLocationId"])
    ]
)
data class TransactionDb(
    @PrimaryKey(autoGenerate = true)
    var id: Long?,
    val accountId: Long,
    val amount: BigDecimal,
    val currencyCode: String,
    val referenceNumber: String?,
    val debitOrCredit: Boolean,
    val mode: TransactionMode,
    /**
     * Maximum 500 NFC Normalized code points //TODO Actually implement the NFC part
     */
    val note: String?,
    val sellerId: Long?,
    val sellerLocationId: Long?,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String,
    /**
     * To allow for reordering of transactions
     */
    val ordinal: Int,
    val datedAt: String,
    val datedAtTime: String?,
): Comparable<TransactionDb> {

    override fun compareTo(other: TransactionDb): Int {
        return this.id!!.compareTo(other.id!!)
    }
}