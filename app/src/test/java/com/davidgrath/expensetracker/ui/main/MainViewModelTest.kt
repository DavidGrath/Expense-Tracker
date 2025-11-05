package com.davidgrath.expensetracker.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.DataBuilder
import com.davidgrath.expensetracker.R
import com.davidgrath.expensetracker.TestExpenseTracker
import com.davidgrath.expensetracker.assertEqualsBD
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.di.TestComponent
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.repositories.ProfileRepository
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.MonthDay
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    @get:Rule
    val mainActivityScenario = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var expenseTrackerDatabase: ExpenseTrackerDatabase
    @Inject
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Inject
    lateinit var categoryDao: CategoryDao
    @Inject
    lateinit var profileRepository: ProfileRepository
    lateinit var app: TestExpenseTracker

    @Before
    fun setUp() {

        app = RuntimeEnvironment.getApplication() as TestExpenseTracker
        (app.appComponent as TestComponent).inject(this)
    }

    @After
    fun tearDown() {
        timeAndLocaleHandler.changeLocale(Locale.US)
        timeAndLocaleHandler.changeClock(Clock.fixed(LocalDateTime.parse("2025-06-30T08:00:00.000").toInstant(ZoneOffset.UTC), ZoneId.of("UTC")))
    }

    @Test
    fun dateModeTestDaily() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")
            // Daily - assert StartDate == EndDate, consider xLyoffset
            viewModel.setDateMode(StatisticsConfig.DateMode.Daily)
            assertEquals(today, viewModel.statisticsConfig.rangeStartDay)
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)

            viewModel.setXLyOffset(-6)
            assertEquals(LocalDate.parse("2025-06-24"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(LocalDate.parse("2025-06-24"), viewModel.statisticsConfig.rangeEndDay)
        }
    }

    @Test
    fun dateModeTestPastXDays() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")
            // PastXDays - assert StartDate is correct
            viewModel.setDateMode(StatisticsConfig.DateMode.PastXDays)
            viewModel.setXDaysPast(-1) //Should default to 1
            assertEquals(today, viewModel.statisticsConfig.rangeStartDay)
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            viewModel.setXDaysPast(7)
            assertEquals(
                LocalDate.parse("2025-06-24"), //"Today" is counted as a day, so only 6 is subtracted
                viewModel.statisticsConfig.rangeStartDay
            )
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
        }
    }

    @Test
    fun dateModeTestPastWeek() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")

            // PastWeek - assert StartDate is correct, DayOfWeek is correct
            viewModel.setDateMode(StatisticsConfig.DateMode.PastWeek)
            assertEquals(DayOfWeek.MONDAY, today.getDayOfWeek())
            assertEquals(LocalDate.parse("2025-06-24"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(DayOfWeek.TUESDAY, viewModel.statisticsConfig.rangeStartDay!!.dayOfWeek)
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
        }
    }

    @Test
    fun dateModeTestWeekly() {
        // Weekly - assert StartDate is correct, DayOfWeek is correct, consider useLocalFirstDay, consider xLyOffset
        timeAndLocaleHandler.changeLocale(Locale.FRANCE) //Monday
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")


            viewModel.setDateMode(StatisticsConfig.DateMode.Weekly)
            assertEquals(today, viewModel.statisticsConfig.rangeStartDay)
            assertEquals(DayOfWeek.MONDAY, viewModel.statisticsConfig.rangeStartDay!!.dayOfWeek)
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(DayOfWeek.MONDAY, viewModel.statisticsConfig.rangeEndDay!!.dayOfWeek)
        }


        timeAndLocaleHandler.changeLocale(Locale.US) //Sunday
        scenario = ActivityScenario.launch(MainActivity::class.java) //Reinitialize
        scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")


            viewModel.setDateMode(StatisticsConfig.DateMode.Weekly)
            assertEquals(LocalDate.parse("2025-06-29"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(DayOfWeek.SUNDAY, viewModel.statisticsConfig.rangeStartDay!!.dayOfWeek)
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(DayOfWeek.MONDAY, viewModel.statisticsConfig.rangeEndDay!!.dayOfWeek)

            viewModel.setXLyOffset(-1)
            assertEquals(LocalDate.parse("2025-06-22"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(DayOfWeek.SUNDAY, viewModel.statisticsConfig.rangeStartDay!!.dayOfWeek)
            assertEquals(LocalDate.parse("2025-06-28"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(DayOfWeek.SATURDAY, viewModel.statisticsConfig.rangeEndDay!!.dayOfWeek)

            viewModel.setFirstWeekDay(DayOfWeek.WEDNESDAY)
            assertEquals(LocalDate.parse("2025-06-18"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(DayOfWeek.WEDNESDAY, viewModel.statisticsConfig.rangeStartDay!!.dayOfWeek)
            assertEquals(LocalDate.parse("2025-06-24"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(DayOfWeek.TUESDAY, viewModel.statisticsConfig.rangeEndDay!!.dayOfWeek)
        }
    }

    @Test
    fun dateModeTestPastMonth() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            // PastMonth - assert StartDate is correct, account for when the previous month has unequal days to the current month
            //Jul and June - greater pair
            //August and July - equal pair
            //June and May - lesser pair

            val todayWhereMonthDaysLessThanPreviousMonthDays = LocalDate.parse("2025-06-30")
            var todayWhereMonthDaysEqualToPreviousMonthDays = LocalDate.parse("2025-08-31")
            var todayWhereMonthDaysGreaterThanPreviousMonthDays = LocalDate.parse("2025-07-16")


            viewModel.setDateMode(StatisticsConfig.DateMode.PastMonth)
            assertEquals(LocalDate.parse("2025-05-31"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--05-31"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay!!)
            ) //Redundant, I know
            assertEquals(todayWhereMonthDaysLessThanPreviousMonthDays, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(MonthDay.parse("--06-30"), MonthDay.from(viewModel.statisticsConfig.rangeEndDay!!))

            timeAndLocaleHandler.changeClock(Clock.fixed(todayWhereMonthDaysEqualToPreviousMonthDays.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")))

            viewModel.setDateMode(StatisticsConfig.DateMode.PastMonth)
            assertEquals(LocalDate.parse("2025-08-01"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--08-01"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay!!)
            )
            assertEquals(todayWhereMonthDaysEqualToPreviousMonthDays, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(MonthDay.parse("--08-31"), MonthDay.from(viewModel.statisticsConfig.rangeEndDay!!))

            timeAndLocaleHandler.changeClock(Clock.fixed(todayWhereMonthDaysGreaterThanPreviousMonthDays.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")))

            viewModel.setDateMode(StatisticsConfig.DateMode.PastMonth)
            assertEquals(LocalDate.parse("2025-06-17"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--06-17"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay!!)
            )
            assertEquals(todayWhereMonthDaysGreaterThanPreviousMonthDays, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(MonthDay.parse("--07-16"), MonthDay.from(viewModel.statisticsConfig.rangeEndDay!!))

        }
    }

    @Test
    fun dateModeTestMonthly() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")

            // Monthly - assert StartDate is correct, consider monthlyDayOfMonth, consider xLyOffset
            viewModel.setXLyOffset(0)
            viewModel.setDateMode(StatisticsConfig.DateMode.Monthly)
            assertEquals(LocalDate.parse("2025-06-01"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                1,
                viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth
            ) //Redundant, I know
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(30, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)

            // Assert same day if first day

            val newToday = LocalDate.parse("2025-05-01")
            timeAndLocaleHandler.changeClock(Clock.fixed(newToday.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")))
            viewModel.setDateMode(StatisticsConfig.DateMode.Monthly)
            assertEquals(newToday, viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                1,
                viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth
            )
            assertEquals(newToday, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(1, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)

            timeAndLocaleHandler.changeClock(Clock.fixed(LocalDateTime.parse("2025-06-30T08:00:00.000").toInstant(ZoneOffset.UTC), ZoneId.of("UTC")))

            // Account for months less than 31 days
            viewModel.setXLyOffset(-2) //April
            viewModel.setMonthlyDayOfMonth(31)
            assertEquals(LocalDate.parse("2025-04-30"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(30, viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth)
            assertEquals(LocalDate.parse("2025-05-29"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(29, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)

            viewModel.setXLyOffset(-4) //Leap Day
            viewModel.setMonthlyDayOfMonth(29)
            assertEquals(LocalDate.parse("2025-02-28"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(28, viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth)
            assertEquals(LocalDate.parse("2025-03-27"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(27, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)
            viewModel.setXLyOffset(-16)
            assertEquals(LocalDate.parse("2024-02-29"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(29, viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth)
            assertEquals(LocalDate.parse("2024-03-28"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(28, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)

            viewModel.setXLyOffset(-3)
            viewModel.setMonthlyDayOfMonth(1)
            assertEquals(LocalDate.parse("2025-03-01"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(1, viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth)
            assertEquals(LocalDate.parse("2025-03-31"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(31, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)

            viewModel.setMonthlyDayOfMonth(15)
            assertEquals(LocalDate.parse("2025-03-15"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(15, viewModel.statisticsConfig.rangeStartDay!!.dayOfMonth)
            assertEquals(LocalDate.parse("2025-04-14"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(14, viewModel.statisticsConfig.rangeEndDay!!.dayOfMonth)
        }
    }

    @Test
    fun dateModeTestPastYear() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")
            // PastYear - assert StartDate is correct, MonthDay is correct. Ignore Feb 29th for now
            viewModel.setDateMode(StatisticsConfig.DateMode.PastYear)
            assertEquals(LocalDate.parse("2024-07-01"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--07-01"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay!!)
            )
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(
                MonthDay.parse("--06-30"),
                MonthDay.from(viewModel.statisticsConfig.rangeEndDay!!)
            )
        }
    }

    @Test
    fun dateModeTestYearly() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")

            // Yearly -  assert StartDate is correct, MonthDay is correct, consider xLyOffset, monthDayOfYear. Disregard --02-29 for the time being
            viewModel.setXLyOffset(0)
            viewModel.setDateMode(StatisticsConfig.DateMode.Yearly)
            assertEquals(LocalDate.parse("2025-01-01"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--01-01"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay)
            )
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(
                MonthDay.parse("--06-30"),
                MonthDay.from(viewModel.statisticsConfig.rangeEndDay)
            )

            viewModel.setMonthDayOfYear(MonthDay.parse("--02-15"))

            assertEquals(LocalDate.parse("2025-02-15"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--02-15"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay)
            )
            assertEquals(today, viewModel.statisticsConfig.rangeEndDay)
            assertEquals(
                MonthDay.parse("--06-30"),
                MonthDay.from(viewModel.statisticsConfig.rangeEndDay)
            )

            viewModel.setXLyOffset(-1)

            assertEquals(LocalDate.parse("2024-02-15"), viewModel.statisticsConfig.rangeStartDay)
            assertEquals(
                MonthDay.parse("--02-15"),
                MonthDay.from(viewModel.statisticsConfig.rangeStartDay)
            )
            assertEquals(LocalDate.parse("2025-02-14"), viewModel.statisticsConfig.rangeEndDay)
            assertEquals(
                MonthDay.parse("--02-14"),
                MonthDay.from(viewModel.statisticsConfig.rangeEndDay)
            )
        }
    }

    @Test
    fun dateModeTestRange() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")

            // Range - if both dates equal, reset to null; if user can somehow unselect both dates from the Material Dialog, then set to All
            viewModel.setDateMode(StatisticsConfig.DateMode.Daily)
            viewModel.setDateMode(StatisticsConfig.DateMode.Range)
            assertNull(viewModel.statisticsConfig.rangeStartDay)
            assertNull(viewModel.statisticsConfig.rangeEndDay)
        }
    }

    @Test
    fun dateModeTestAll() {
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel
            val today = LocalDate.parse("2025-06-30")


            // All - assert both dates null, but try and take note of the earliest date from the user's transactions and use to offset visualisations
            viewModel.setDateMode(StatisticsConfig.DateMode.All)
            assertNull(viewModel.statisticsConfig.rangeStartDay)
            assertNull(viewModel.statisticsConfig.rangeEndDay)
        }
    }

    @Test
    fun givenPreviousXLyNotSameWhenSetDateModeToXLyThenOffsetReset() {
        mainActivityScenario.scenario.onActivity {
            val originalOffset = -2
            val resetOffset = 0
            val viewModel = it.viewModel
            viewModel.setDateMode(StatisticsConfig.DateMode.Daily)
            viewModel.setXLyOffset(originalOffset)
            viewModel.setDateMode(StatisticsConfig.DateMode.PastMonth)
            viewModel.setDateMode(StatisticsConfig.DateMode.Yearly)
            assertEquals(resetOffset, viewModel.statisticsConfig.xLyOffset)
        }
    }

    @Test
    fun givenPreviousXLySameWhenSetDateModeToXLyThenOffsetSame() {
        mainActivityScenario.scenario.onActivity {
            val originalOffset = -2
            val viewModel = it.viewModel
            viewModel.setDateMode(StatisticsConfig.DateMode.Daily)
            viewModel.setXLyOffset(originalOffset)
            viewModel.setDateMode(StatisticsConfig.DateMode.PastMonth)
            viewModel.setDateMode(StatisticsConfig.DateMode.Daily)
            assertEquals(originalOffset, viewModel.statisticsConfig.xLyOffset)
        }
    }


    @Test
    @Ignore("Too much trouble with LiveData")
    fun weekdaysFilterTest() {
        val totalMondaysAndSundaysInJune2025 = 10
        val monthlySum = BigDecimal(12750)
        val sundaySum = BigDecimal(1850)
        val mondaySum = BigDecimal(2400)
        val expectedSum = mondaySum + sundaySum
        val dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(150))
            .atDate(LocalDate.parse("2025-06-15"))
            .repeatIntoDateRange(LocalDate.parse("2025-06-01"), LocalDate.parse("2025-06-14"))
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description 2", "utilities", BigDecimal(700))
            .atDate(LocalDate.parse("2025-06-30"))
            .repeatIntoDateRange(LocalDate.parse("2025-06-16"), LocalDate.parse("2025-06-29"))
            .commit()

        val countingResource = CountingIdlingResource("ViewModel stats weekdaysFilterTest")
        IdlingRegistry.getInstance().register(countingResource)
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel

//            countingResource.increment()
//            countingResource.increment()

            viewModel.statsTransactionAndItemCount.observe(it) {
                LOGGER.debug("Q: {}", it)
//                countingResource.decrement()
            }
            viewModel.statsTotalExpense.observe(it) {
                LOGGER.debug("R: {}", it)
//                countingResource.decrement()
            }
            viewModel.setDateMode(StatisticsConfig.DateMode.Monthly)
//            countingResource.increment()
//            countingResource.increment()
            Thread.sleep(1_100)
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) //Dirty workaround to make sure the LiveData value is updated

            var transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            var transactionCount = transactionAndItemCount.transactionCount
            var debitSum = viewModel.statsTotalExpense.value!!
            assertEquals(30, transactionCount)
            assertEqualsBD(monthlySum, debitSum)

//            countingResource.increment()
//            countingResource.increment()
            viewModel.setDateMode(StatisticsConfig.DateMode.PastXDays)
            viewModel.setXDaysPast(30)
            viewModel.setFilterWeekDays(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY))
            Thread.sleep(2_100)
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(totalMondaysAndSundaysInJune2025, transactionCount)
            assertEqualsBD(expectedSum, debitSum)
        }
    }

    @Test
    fun categoriesFilterTest() {
        val profile = profileRepository.getByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()
        val food = categoryDao.findByProfileIdAndStringId(profile.id!!, "food").subscribeOn(Schedulers.io()).blockingGet()!!
        val misc = categoryDao.findByProfileIdAndStringId(profile.id!!, "miscellaneous").subscribeOn(Schedulers.io()).blockingGet()!!
        val util = categoryDao.findByProfileIdAndStringId(profile.id!!, "utilities").subscribeOn(Schedulers.io()).blockingGet()!!
        val entertainment = categoryDao.findByProfileIdAndStringId(profile.id!!, "entertainment").subscribeOn(Schedulers.io()).blockingGet()!!

        val totalTransactionCount = 4
        val miscCount = 2
        val utilCount = 2
        val miscUtilCount = 3
        val foodCount = 1
        val entertainmentCount = 0
        val miscSum = BigDecimal(1550)
        val utilSum = BigDecimal(1432)
        val miscUtilSum = BigDecimal(1582)
        val foodSum = BigDecimal(100)
        val grandSum = BigDecimal(1682)
        val entertainmentSum = BigDecimal.ZERO

        val dataBuilder = DataBuilder(app, expenseTrackerDatabase, timeAndLocaleHandler)
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(150))
//            .withItem("Desc", "transportation", BigDecimal(150))
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description 2", "miscellaneous", BigDecimal(700))
            .withItem("Desc 2", "utilities", BigDecimal(700))
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description 3", "utilities", BigDecimal(32))
//            .withItem("Desc 2", "utilities", BigDecimal(700))
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description 4", "food", BigDecimal(100))
            .commit()

        val countingResource = CountingIdlingResource("ViewModel stats categoriesFilterTest")
        IdlingRegistry.getInstance().register(countingResource)
        mainActivityScenario.scenario.onActivity {
            val viewModel = it.viewModel

            countingResource.increment()
            countingResource.increment()

            viewModel.statsTransactionAndItemCount.observe(it) {
                countingResource.decrement()
            }
            viewModel.statsTotalExpense.observe(it) {
                countingResource.decrement()
            }
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) //Dirty workaround to make sure the LiveData value is updated

            var transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            var transactionCount = transactionAndItemCount.transactionCount
            var debitSum = viewModel.statsTotalExpense.value!!
            assertEquals(totalTransactionCount, transactionCount)
            assertEqualsBD(grandSum, debitSum)

            //Misc
            countingResource.increment()
            countingResource.increment()
            viewModel.setCategories(listOf(misc.id!!))
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(miscCount, transactionCount)
            assertEqualsBD(miscSum, debitSum)


            //Util
            countingResource.increment()
            countingResource.increment()
            viewModel.setCategories(listOf(util.id!!))
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(utilCount, transactionCount)
            assertEqualsBD(utilSum, debitSum)


            //Misc and Util
            countingResource.increment()
            countingResource.increment()
            viewModel.setCategories(listOf(misc.id!!, util.id!!))
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(miscUtilCount, transactionCount)
            assertEqualsBD(miscUtilSum, debitSum)

            //Food
            countingResource.increment()
            countingResource.increment()
            viewModel.setCategories(listOf(food.id!!))
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(foodCount, transactionCount)
            assertEqualsBD(foodSum, debitSum)


            //Entertainment
            countingResource.increment()
            countingResource.increment()
            viewModel.setCategories(listOf(entertainment.id!!))
            onView(withId(R.id.linear_layout_transactions_stats)).check(matches(isDisplayed())) // Workaround

            transactionAndItemCount = viewModel.statsTransactionAndItemCount.value!!
            transactionCount = transactionAndItemCount.transactionCount
            debitSum = viewModel.statsTotalExpense.value!!

            assertEquals(entertainmentCount, transactionCount)
            assertEqualsBD(entertainmentSum, debitSum)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainViewModelTest::class.java)
    }
}