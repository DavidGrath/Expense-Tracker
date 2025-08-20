package com.davidgrath.expensetracker.di

import android.app.Application
import androidx.room.Room
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import dagger.Module
import dagger.Provides
import org.threeten.bp.Clock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Module
class MainModule(private val application: Application, private val fileHandler: DraftFileHandler) {

    @Provides
    fun application(): Application {
        return application
    }

    @Provides
    fun appDatabase(): ExpenseTrackerDatabase {
        return ExpenseTrackerDatabase.getDatabase(application)
    }

    @Provides
    fun categoryDao(): CategoryDao {
        return appDatabase().categoryDao()
    }

    @Provides
    fun imagesDao(): ImageDao {
        return appDatabase().imageDao()
    }

    @Provides
    fun profileDao(): ProfileDao {
        return appDatabase().profileDao()
    }

    @Provides
    fun transactionDao(): TransactionDao {
        return appDatabase().transactionDao()
    }

    @Provides
    fun transactionItemDao(): TransactionItemDao {
        return appDatabase().transactionItemDao()
    }

    @Provides
    fun transactionItemImagesDao(): TransactionItemImagesDao {
        return appDatabase().transactionItemImagesDao()
    }


    @Provides
    fun fileHandler(): DraftFileHandler {
        return fileHandler
    }

    @Provides
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }
}