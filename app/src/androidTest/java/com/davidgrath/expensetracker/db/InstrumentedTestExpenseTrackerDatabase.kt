package com.davidgrath.expensetracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.davidgrath.expensetracker.db.Converters
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.db.SellerLocationDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemCategoriesDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb

@Database(version = 3,
    entities = [
        CategoryDb::class, ImageDb::class, ProfileDb::class, TransactionDb::class, TransactionItemDb::class, TransactionItemImagesDb::class,
        EvidenceDb::class, AccountDb::class, TransactionItemCategoriesDb::class, SellerDb::class, SellerLocationDb::class
    ])
@TypeConverters(Converters::class)
abstract class InstrumentedTestExpenseTrackerDatabase: ExpenseTrackerDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: InstrumentedTestExpenseTrackerDatabase? = null
        fun getDatabase(context: Context): InstrumentedTestExpenseTrackerDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.inMemoryDatabaseBuilder(context, InstrumentedTestExpenseTrackerDatabase::class.java).build()
                INSTANCE = instance
                instance
            }
        }
    }
}