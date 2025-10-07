package com.davidgrath.expensetracker

import android.content.Context
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.di.TestComponent
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Locale
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class ExpenseTrackerTest {

    @Inject
    lateinit var profileDao: ProfileDao
    @Inject
    lateinit var accountDao: AccountDao
    val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun initializeTest() {
        val app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        val preferences = app.getSharedPreferences(Constants.DEFAULT_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        //Assert default profile exists
        assertEquals(Constants.DEFAULT_PROFILE_ID, preferences.getString(Constants.PreferenceKeys.Device.CURRENT_PROFILE, null))
        val profile = profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        assertNotNull(profile)
        //Assert default account exists
        val accounts = accountDao.getAllByProfileIdSingle(profile!!.id!!).subscribeOn(Schedulers.io()).blockingGet()
        val account = accounts.firstOrNull()
        assertNotNull(account)
        assertEquals("USD", account!!.currencyCode)
    }
}