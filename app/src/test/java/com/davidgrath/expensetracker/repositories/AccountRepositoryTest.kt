package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.ExpenseTracker
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.test.TestContentProvider
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class AccountRepositoryTest {

    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var database: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    lateinit var app: ExpenseTracker
    lateinit var dataBuilder: DataBuilder


    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        app.tempInit().subscribeOn(Schedulers.io()).blockingSubscribe()
        (app.appComponent as TestComponent).inject(this)
        dataBuilder = DataBuilder(app, database, timeAndLocaleHandler)
        Robolectric.setupContentProvider(TestContentProvider::class.java, TestContentProvider.AUTHORITY)
    }

    @After
    fun tearDown() {
        app.filesDir.deleteRecursively()
        Single.fromCallable { database.clearAllTables() }.subscribeOn(Schedulers.io()).blockingSubscribe()
        println("clearAllTables")
    }

    @Test
    fun givenOneAccountLeftForProfileWhenDeleteThenFail() { //TODO Care about cascading delete later
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(
            Schedulers.io()).blockingGet()
        val defaultAccountId = getDefaultAccountId(profileRepository, accountRepository)

        val result = accountRepository.deleteAccount(profile.id!!, defaultAccountId).onErrorReturn { -1 } .blockingGet()

        Assert.assertEquals(-1, result)
        Assert.assertEquals(
            1,
            accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().size
        )
    }
}