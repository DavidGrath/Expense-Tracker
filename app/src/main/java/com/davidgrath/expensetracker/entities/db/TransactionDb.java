package com.davidgrath.expensetracker.entities.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jilt.Builder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;

import java.math.BigDecimal;

@Builder
@NullMarked
@Entity(indices = {@Index(value = {"datedAt"})})
public class TransactionDb implements Comparable<TransactionDb> {
    @PrimaryKey(autoGenerate = true)
    @Nullable
    private Long id;
    private final Long accountId;
    private final BigDecimal amount;
    private final String currencyCode;
    @Nullable
    private final String referenceNumber;
    private final Boolean cashOrCredit; //TODO Possibly revise to enum Mode {Cash,Transfer,POS,etc}
    /**
     * Maximum 300 NFC Normalized code points
     */
    @Nullable
    private final String note;
    @Nullable
    private final Long sellerID;
    @Nullable
    private final Long sellerLocationId;
    private final String createdAt;
    private final String createdAtOffset;
    private final String createdAtTimezone;
    /**
     * To allow for reordering of transactions as well as imposing an ordering on timeless transactions
     */
    private final Integer ordinal;
    private final String datedAt;
    @Nullable
    private final String datedAtTime;
    @Nullable
    private final String datedAtOffset;
    @Nullable
    private final String datedAtTimezone;

    public TransactionDb(@Nullable Long id, Long accountId, BigDecimal amount, String currencyCode, @Nullable String referenceNumber, Boolean cashOrCredit, @Nullable String note, @Nullable Long sellerID, @Nullable Long sellerLocationId, String createdAt, String createdAtOffset, String createdAtTimezone, Integer ordinal, String datedAt, @Nullable String datedAtTime, @Nullable String datedAtOffset, @Nullable String datedAtTimezone) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.referenceNumber = referenceNumber;
        this.cashOrCredit = cashOrCredit;
        this.note = note;
        this.sellerID = sellerID;
        this.sellerLocationId = sellerLocationId;
        this.createdAt = createdAt;
        this.createdAtOffset = createdAtOffset;
        this.createdAtTimezone = createdAtTimezone;
        this.ordinal = ordinal;
        this.datedAt = datedAt;
        this.datedAtTime = datedAtTime;
        this.datedAtOffset = datedAtOffset;
        this.datedAtTimezone = datedAtTimezone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public Boolean getCashOrCredit() {
        return cashOrCredit;
    }

    public String getNote() {
        return note;
    }

    public Long getSellerID() {
        return sellerID;
    }

    public Long getSellerLocationId() {
        return sellerLocationId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtOffset() {
        return createdAtOffset;
    }

    public String getCreatedAtTimezone() {
        return createdAtTimezone;
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public String getDatedAt() {
        return datedAt;
    }

    public String getDatedAtTime() {
        return datedAtTime;
    }

    public String getDatedAtOffset() {
        return datedAtOffset;
    }

    public String getDatedAtTimezone() {
        return datedAtTimezone;
    }

    @Override
    public int compareTo(TransactionDb other) {
        return this.id.compareTo(other.id);
    }
    @Ignore
    public @Nullable LocalDateTime getLocalDateTime() {
        if(datedAtTime == null) {
            return null;
        }
        LocalDate utcDate = LocalDate.parse(datedAt);
        LocalTime utcTime = LocalTime.parse(datedAtTime);
        ZoneOffset offset = ZoneOffset.of(datedAtOffset);
        LocalDateTime utcDateTime = utcDate.atTime(utcTime);
        OffsetDateTime offsetDateTime = utcDateTime.atOffset(offset);
        LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
        return localDateTime;
    }

}