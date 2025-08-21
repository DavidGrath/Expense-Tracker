package com.davidgrath.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import com.davidgrath.expensetracker.db.Converters
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb

@Database(version = 1, entities = [
    CategoryDb::class, ImageDb::class, ProfileDb::class, TransactionDb::class, TransactionItemDb::class, TransactionItemImagesDb::class,
    EvidenceDb::class
])
@TypeConverters(Converters::class)
abstract class TestExpenseTrackerDatabase: ExpenseTrackerDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: TestExpenseTrackerDatabase? = null
        fun getDatabase(context: Context): TestExpenseTrackerDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.inMemoryDatabaseBuilder(context, TestExpenseTrackerDatabase::class.java).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}