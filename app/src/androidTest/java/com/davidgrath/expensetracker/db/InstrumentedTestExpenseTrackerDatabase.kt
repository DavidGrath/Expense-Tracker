package com.davidgrath.expensetracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import com.davidgrath.expensetracker.db.Converters
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb

@Database(version = 1, entities = [CategoryDb::class, ImageDb::class, ProfileDb::class, TransactionDb::class, TransactionItemDb::class, TransactionItemImagesDb::class])
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