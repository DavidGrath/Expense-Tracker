package com.davidgrath.expensetracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb

@Database(version = 1,
    entities = [
        CategoryDb::class, ImageDb::class, ProfileDb::class, TransactionDb::class, TransactionItemDb::class, TransactionItemImagesDb::class,
        EvidenceDb::class
    ]
)
@TypeConverters(Converters::class)
abstract class ExpenseTrackerDatabase: RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun imageDao(): ImageDao
    abstract fun profileDao(): ProfileDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun transactionItemImagesDao(): TransactionItemImagesDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseTrackerDatabase? = null
        fun getDatabase(context: Context): ExpenseTrackerDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.databaseBuilder(context, ExpenseTrackerDatabase::class.java, Constants.DATABASE_NAME).build()
                INSTANCE = instance
                instance
            }
        }
    }
}