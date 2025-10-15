package com.davidgrath.expensetracker.ui.transactiondetails

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.InstrumentedTestExpenseTracker
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.di.InstrumentedTestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.TransactionItemRepository
import com.davidgrath.expensetracker.repositories.TransactionRepository
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class TransactionDetailsActivityInstrumentedTest {

    lateinit var transactionDetailsActivityScenario: ActivityScenario<TransactionDetailsActivity>
//    lateinit var transactionDetailsActivityScenario: ActivityScenario<AddDetailedTransactionActivity>
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var transactionRepository: TransactionRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var transactionItemRepository: TransactionItemRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var profileDao: ProfileDao
    @Inject
    lateinit var database: ExpenseTrackerDatabase
    lateinit var app: InstrumentedTestExpenseTracker
    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<InstrumentedTestExpenseTracker>()
        val appComponent = app.appComponent as InstrumentedTestComponent
        appComponent.inject(this)

        val (id, itemId) = saveBasicTransaction(BigDecimal.TEN, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()

        val intent = Intent(app, TransactionDetailsActivity::class.java)
        intent.putExtra(TransactionDetailsActivity.ARG_TRANSACTION_ID, id)
        transactionDetailsActivityScenario =
            ActivityScenario.launch<TransactionDetailsActivity>(intent)

        /*val intent = Intent(app, AddDetailedTransactionActivity::class.java).also {
            it.putExtra(AddDetailedTransactionActivity.ARG_MODE, "edit")
            it.putExtra(AddDetailedTransactionActivity.ARG_EDIT_TRANSACTION_ID, id)
        }
        transactionDetailsActivityScenario =
            ActivityScenario.launch<AddDetailedTransactionActivity>(intent)*/
    }

    @After
    fun tearDown() {
        timeAndLocaleHandler.changeZone(ZoneId.systemDefault())
        Single.fromCallable { database.clearAllTables() }.subscribeOn(Schedulers.io()).blockingSubscribe()
    }

    @Test
    fun givenTransactionZoneIsNotSameAsSystemZoneWhenLoadEditThenOriginalZoneDisplayedAndNoticeDisplayed() {

        timeAndLocaleHandler.changeZone(ZoneId.of("Pacific/Honolulu")) //-10 hours changes the date as well
        transactionDetailsActivityScenario.recreate()

        val customDate = LocalDate.of(2025, 6, 29)
        val customTime = LocalTime.of( 22, 0, 0)
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)

        val dateString = dateFormat.format(customDate)
        val timeString = timeFormat.format(customTime)

        onView(withId(R.id.text_view_transaction_details_date)).check(matches(withText(dateString)))
        onView(withId(R.id.text_view_transaction_details_time)).check(matches(withText(timeString)))
        onView(withId(R.id.text_view_transaction_details_zone_difference_notice)).check(matches(isDisplayed()))
    }

    fun saveBasicTransaction(amount: BigDecimal, categoryStringId: String = "miscellaneous"): Single<Pair<Long, Long>> {
        val profile = profileDao.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val accountId = accountRepository.getAccountsForProfileSingle(profile.id!!).blockingGet().firstOrNull()!!.id

        val transaction = TestBuilder.defaultTransaction(accountId!!, amount)
        val id = transactionRepository.addTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()
        val category = categoryRepository.findByStringId(categoryStringId).subscribeOn(Schedulers.io()).blockingGet()!!
        val item = TestBuilder.defaultTransactionItemBuilder(id, amount, category.id!!).build()
        return transactionItemRepository.addTransactionItem(item).subscribeOn(Schedulers.io()).map { itemId ->
            id to itemId
        }
    }
}