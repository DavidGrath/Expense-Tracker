package com.davidgrath.expensetracker.ui.main.accounts

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TabLayoutItemClick
import com.davidgrath.expensetracker.TestBuilder
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.clickRecyclerViewItem
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.getDefaultAccountId
import com.davidgrath.expensetracker.repositories.AccountRepository
import com.davidgrath.expensetracker.repositories.CategoryRepository
import com.davidgrath.expensetracker.repositories.ProfileRepository
import com.davidgrath.expensetracker.ui.main.MainActivity
import io.reactivex.rxjava3.schedulers.Schedulers
import org.hamcrest.Matchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class AccountsFragmentTest {

    @Inject
    lateinit var profileRepository: ProfileRepository
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var transactionItemDao: TransactionItemDao
    @Inject
    lateinit var transactionDao: TransactionDao
    lateinit var app: TestExpenseTracker

    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        onView(withId(R.id.tab_layout_main)).perform(TabLayoutItemClick(2))

        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @Test
    fun whenClickViewStatisticsThenFilterApplied() {

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        val category = categoryRepository.findByStringId("miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        val transactionBuilder = TestBuilder.defaultTransactionBuilder(accountId, BigDecimal(100))
        val id = transactionDao.insertTransaction(transactionBuilder.build()).subscribeOn(Schedulers.io()).blockingGet()
        val itemBuilder = TestBuilder.defaultTransactionItemBuilder(id, BigDecimal(100), category.id!!)
        val itemId = transactionItemDao.insertTransactionItem(itemBuilder.build()).subscribeOn(
            Schedulers.io()).blockingGet()

        val secondTransaction = transactionBuilder.amount(BigDecimal(200)).debitOrCredit(false).build()
        val id2 = transactionDao.insertTransaction(secondTransaction).subscribeOn(Schedulers.io()).blockingGet()
        transactionItemDao.insertTransactionItemMultiple(listOf(
            itemBuilder.transactionId(id2).build(),
            itemBuilder.transactionId(id2).build()
        )
        ).subscribeOn(Schedulers.io()).blockingSubscribe()

        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(
            Schedulers.io()).blockingGet()
        val newAccountId = accountRepository.createAccount(profile.id!!, "British", "GBP").blockingGet()

        clickRecyclerViewItem<AccountsRecyclerAdapter.AccountsViewHolder>(
            R.id.recyclerview_accounts,
            0,
            R.id.image_view_account_view_stats
        )
        onView(withId(R.id.fragment_statistics)).check(matches(isDisplayed()))
        onView(withId(R.id.text_view_statistics_transaction_count)).check(matches(withText(Matchers.matchesRegex(".*\\b2\\b.*")))) //TODO Maybe use ViewModel's LiveData.value instead
        onView(withId(R.id.text_view_statistics_item_count)).check(matches(withText(Matchers.matchesRegex(".*\\b3\\b.*"))))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AccountsFragmentTest::class.java)
    }
}