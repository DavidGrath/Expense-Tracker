package com.davidgrath.expensetracker.di

import android.app.Application
import com.davidgrath.expensetracker.DraftFileHandler
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.db.dao.SellerDao
import com.davidgrath.expensetracker.db.dao.SellerLocationDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.utils.ImageHelper
import com.davidgrath.expensetracker.utils.ImageHelperImpl
import dagger.Module
import dagger.Provides
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
    fun evidenceDao(): EvidenceDao {
        return appDatabase().evidenceDao()
    }

    @Provides
    fun accountDao(): AccountDao {
        return appDatabase().accountDao()
    }
    @Provides
    fun sellerDao(): SellerDao {
        return appDatabase().sellerDao()
    }

    @Provides
    fun sellerLocationDao(): SellerLocationDao {
        return appDatabase().sellerLocationDao()
    }


    @Provides
    fun fileHandler(): DraftFileHandler {
        return fileHandler
    }

    @Singleton
    @Provides
    fun timeHandler(): TimeAndLocaleHandler {
        return MainTimeAndLocaleHandler()
    }

    @Singleton
    @Provides
    fun imageHelper(): ImageHelper {
        return ImageHelperImpl()
    }
}