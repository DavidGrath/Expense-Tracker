package com.davidgrath.expensetracker

import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.math.BigDecimal

/*
class TransactionBuilder(private val expenseTrackerDatabase: ExpenseTrackerDatabase, private val timeAndLocaleHandler: TimeAndLocaleHandler) {

    private val items = arrayListOf<TransactionItemDb>()
    private var account: AccountDb
    private var newAccount: AccountDb? = null
    private val repeatDates = arrayListOf<LocalDate>()
    private var defaultItemPrice: BigDecimal? = null
    private var debitOrCredit: Boolean = true
    private var date: LocalDate? = null

    private val itemsDao = expenseTrackerDatabase.transactionItemDao()
    private val categoryDao = expenseTrackerDatabase.categoryDao()
    private val profileDao = expenseTrackerDatabase.profileDao()
    private val accountDao = expenseTrackerDatabase.accountDao()
    private val transactionDao = expenseTrackerDatabase.transactionDao()
    private val transactionItemDao = expenseTrackerDatabase.transactionItemDao()
    private var hasCommitted = false

    private val profile: ProfileDb

    init {
        profile = profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()!!
        account = accountDao.getAllByProfileIdSingle(profile.id!!).subscribeOn(Schedulers.io()).blockingGet().first()
    }

    fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder {
        this.defaultItemPrice = defaultPrice
        return this
    }

    fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder {
        val category = categoryDao.findByStringId(category).subscribeOn(Schedulers.io()).blockingGet()
        if(category == null) {
            throw IllegalStateException("Category does not exist")
        }
        items.add(TransactionItemDb(null, -1, amount, null, 1, description, "", null, category.id!!, "", "", ""))
        return this
    }

    fun withItem(description: String): TransactionBuilder {
        if(defaultItemPrice == null) {
            throw IllegalStateException("Default item price not set")
        }
        return withItem(description, "miscellaneous", defaultItemPrice!!)
    }

    fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder {
        if(newAccount != null) {
            throw IllegalStateException("New account already added")
        }
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
        val xAccount = AccountDb(null, profile.id!!, currencyCode, null, "", accountName, dateTime, offset, zone)
        val accountId = accountDao.insertAccount(xAccount).subscribeOn(Schedulers.io()).blockingGet()
        newAccount = xAccount.copy(id = accountId)
        return this
    }

    fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder {
        this.repeatDates.addAll(dates)
        return this
    }

    fun atDate(localDate: LocalDate): TransactionBuilder {
        if(this.date != null) {
            throw IllegalStateException("Date has already been set")
        }
        this.date = localDate
        return this
    }

    fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder {
        this.debitOrCredit = debitOrCredit
        return this
    }

    fun getAccount(): AccountDb {
        return newAccount?: account
    }

    fun commit(): List<Long> {
        if(hasCommitted) {
            throw IllegalStateException("Transaction already committed")
        }
        if(items.isEmpty()) {
            throw IllegalStateException("Transaction must have at least one item")
        }
        val sum = items.map { it.amount }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        val account = newAccount?: account
        val firstDate = this.date?: LocalDate.now(timeAndLocaleHandler.getClock())
        val time = LocalTime.now(timeAndLocaleHandler.getClock())
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())

        val repeats = listOf(firstDate) + repeatDates
        val ids = repeats.map { repeatDate ->
            val transaction = TransactionDb(null, account.id!!, sum, account.currencyCode, null, debitOrCredit, null, null, null, dateTime, offset, zone, 1, repeatDate.toString(), time.toString(), offset, zone)
            val transactionId = transactionDao.insertTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()

            for(item in items) {
                val correctedItem = item.copy(transactionId = transactionId, createdAt = dateTime, createdAtOffset = offset, createdAtTimezone = zone)
                transactionItemDao.insertTransactionItem(correctedItem).subscribeOn(Schedulers.io()).blockingSubscribe()
            }
            transactionId
        }
        hasCommitted = true
        return ids
    }

    */
/*interface TransactionItemBuilder {
        fun withItem(description: String, category: String, amount: BigDecimal): TransactionItemBuilder
    }

    interface XTransactionBuilder {
        fun
    }*//*


}*/
