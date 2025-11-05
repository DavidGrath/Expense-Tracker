package com.davidgrath.expensetracker.entities.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["profileId"])],
    foreignKeys = [
        ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["profileId"]),
        ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["productId"]),
        ForeignKey(ProfileDb::class, parentColumns = ["id"], childColumns = ["imageId"])]
)
data class ProductImagesDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val profileId: Long,
    val productId: Long,
    val imageId: Long,
    val createdAt: String,
    val createdAtOffset: String,
    val createdAtTimezone: String
)