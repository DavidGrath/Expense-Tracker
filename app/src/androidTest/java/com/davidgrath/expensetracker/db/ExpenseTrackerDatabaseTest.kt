package com.davidgrath.expensetracker.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.test.TestContentProvider
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class ExpenseTrackerDatabaseTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InstrumentedTestExpenseTrackerDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    val TEST_DB = "migration_test"
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        app.tempInit().subscribeOn(Schedulers.io()).blockingSubscribe()
        (app.appComponent as InstrumentedTestComponent).inject(this)
    }

    @Test
    fun migrationTest1To2() {
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO ProfileDb (name, stringId, createdAt, createdAtOffset, createdAtTimezone) VALUES ('Default', '${Constants.DEFAULT_PROFILE_ID}', '${dateTime}', '${offset}', '$zone')",)
            execSQL("INSERT INTO SellerDb (profileId, name, createdAt, createdAtOffset, createdAtTimezone) VALUES (1, 'Restaurant', '${dateTime}', '${offset}', '$zone')")
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ExpenseTrackerDatabase.MIGRATION_1_2)
    }

    @Test
    fun migrationTest2To3() {
        var db = helper.createDatabase(TEST_DB, 3)
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, ExpenseTrackerDatabase.MIGRATION_2_3)
    }
}