package com.davidgrath.expensetracker

import android.content.Context
import androidx.core.net.toUri
import com.davidgrath.expensetracker.db.ExpenseTrackerDatabase
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.io.File
import java.math.BigDecimal

class DataBuilder(private val context: Context, private val expenseTrackerDatabase: ExpenseTrackerDatabase, private val timeAndLocaleHandler: TimeAndLocaleHandler) {
    fun createTransaction(): TransactionBuilder {
        val transactionBuilder = TransactionBuilderImpl(context, expenseTrackerDatabase, timeAndLocaleHandler)
        return transactionBuilder
    }

    fun createBasicTransaction(description: String, category: String, amount: BigDecimal): Long {
        val transactionBuilder = TransactionBuilderImpl(context, expenseTrackerDatabase, timeAndLocaleHandler)
            .withItem(description, category, amount)
        val id = transactionBuilder.commit().first()
        return id
    }

    private class TransactionBuilderImpl(private val context: Context, private val expenseTrackerDatabase: ExpenseTrackerDatabase, private val timeAndLocaleHandler: TimeAndLocaleHandler): TransactionBuilder {
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
        private val imagesDao = expenseTrackerDatabase.imageDao()
        private val itemImagesDao = expenseTrackerDatabase.transactionItemImagesDao()
        private var hasCommitted = false
        private var imagesMap = mutableMapOf<Int, List<TestData.Resource>>()

        private val profile: ProfileDb

        init {
            profile = profileDao.findByStringId(Constants.DEFAULT_PROFILE_ID).subscribeOn(Schedulers.io()).blockingGet()!!
            account = accountDao.getAllByProfileIdSingle(profile.id!!).subscribeOn(Schedulers.io()).blockingGet().first()
        }

        override fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder {
            this.defaultItemPrice = defaultPrice
            return this
        }

        override fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder {
            val categoryDb = categoryDao.findByStringId(category).subscribeOn(Schedulers.io()).blockingGet()
            if(categoryDb == null) {
                throw IllegalStateException("Category does not exist")
            }
            items.add(TransactionItemDb(null, -1, amount, null, 1, description, "", null, categoryDb.id!!, "", "", ""))
            return this
        }

        override fun withItem(description: String): TransactionBuilder {
            if(defaultItemPrice == null) {
                throw IllegalStateException("Default item price not set")
            }
            return withItem(description, "miscellaneous", defaultItemPrice!!)
        }

        override fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder {
            if(newAccount != null) {
                throw IllegalStateException("New account already added")
            }
            val (dateTime, offset, zone) = dateTimeOffsetZone(timeAndLocaleHandler.getClock())
            val xAccount = AccountDb(null, profile.id!!, currencyCode, null, "", accountName, dateTime, offset, zone)
            val accountId = accountDao.insertAccount(xAccount).subscribeOn(Schedulers.io()).blockingGet()
            newAccount = xAccount.copy(id = accountId)
            return this
        }

        override fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder {
            this.repeatDates.addAll(dates)
            return this
        }

        override fun atDate(localDate: LocalDate): TransactionBuilder {
            if(this.date != null) {
                throw IllegalStateException("Date has already been set")
            }
            this.date = localDate
            return this
        }

        override fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder {
            this.debitOrCredit = debitOrCredit
            return this
        }

        fun getAccount(): AccountDb {
            return newAccount?: account
        }

        override fun commit(): List<Long> {
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
            val sha256Map = mutableMapOf<String, Long>()
            val mainImagesFolder = file(context.filesDir, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
            val classLoader = TransactionItemBuilderImpl::class.java.classLoader
            val ids = repeats.map { repeatDate ->
                val transaction = TransactionDb(null, account.id!!, sum, account.currencyCode, null, debitOrCredit, null, null, null, dateTime, offset, zone, 1, repeatDate.toString(), time.toString(), offset, zone)
                val transactionId = transactionDao.insertTransaction(transaction).subscribeOn(Schedulers.io()).blockingGet()

                items.forEachIndexed { index, item ->
                    val correctedItem = item.copy(transactionId = transactionId, createdAt = dateTime, createdAtOffset = offset, createdAtTimezone = zone)
                    val itemId = transactionItemDao.insertTransactionItem(correctedItem).subscribeOn(Schedulers.io()).blockingGet()
                    val list = imagesMap[index]
                    if(list != null) {
                        for(resource in list) {
                            val imageId = if(sha256Map.contains(resource.sha256)) {
                                sha256Map[resource.sha256]!!
                            } else {
                                val resourceInputStream = classLoader.getResourceAsStream(resource.resourceName)
                                val file = File(mainImagesFolder, resource.fileName)
                                val outputStream = file.outputStream()
                                val copied = resourceInputStream.copyTo(outputStream)
                                resourceInputStream.close()
                                outputStream.close()
                                val imageDb = ImageDb(null, file.length(), resource.sha256, "image/jpeg", file.toUri().toString(), "", "", "") //TODO date time
                                val imageId = imagesDao.insertImage(imageDb).blockingGet()
                                sha256Map[resource.sha256] = imageId
                                imageId
                            }
                            val itemImage = TransactionItemImagesDb(null, itemId, imageId, "", "", "") //TODO date time
                            itemImagesDao.insertItemImage(itemImage).subscribeOn(Schedulers.io()).blockingSubscribe()

                        }
                    }
                }
                transactionId
            }
            hasCommitted = true
            return ids
        }

        override fun withDetailedItem(): TransactionItemBuilder {
            val itemBuilder = TransactionItemBuilderImpl(this, this.expenseTrackerDatabase)
            return itemBuilder
        }

        override fun repeatIntoDateRange(startDate: LocalDate, endDate: LocalDate): TransactionBuilder {
            if(startDate > endDate) {
                throw IllegalArgumentException("Invalid date range")
            }
            val dates = arrayListOf<LocalDate>()
            var runningDate = startDate
            while(runningDate <= endDate) {
                dates.add(runningDate)
                runningDate = runningDate.plusDays(1)
            }
            this.repeatDates.addAll(dates)
            return this
        }

        fun addItem(transactionItemDb: TransactionItemDb) {
            this.items.add(transactionItemDb)
        }

        fun addItem(transactionItemDb: TransactionItemDb, images: List<TestData.Resource>) {
            this.items.add(transactionItemDb)
            val index = this.items.size - 1
            imagesMap[index] = images
        }
    }

    private class TransactionItemBuilderImpl(private val xTransactionBuilderImpl: TransactionBuilderImpl, private val expenseTrackerDatabase: ExpenseTrackerDatabase): TransactionItemBuilder {

        private var item = TransactionItemDb(null, -1, BigDecimal.ZERO, null, 1, "", "", null, -1, "", "", "")
        private val categoryDao = expenseTrackerDatabase.categoryDao()
        private var images = listOf<TestData.Resource>()
        private var mainDetailsSet = false

        override fun commit(): List<Long> {
            build()
            return xTransactionBuilderImpl.commit()
        }

        override fun mainDetails(description: String, category: String, amount: BigDecimal): TransactionItemBuilder {
            if(mainDetailsSet) {
                throw IllegalStateException("Main details have already been set")
            }
            val categoryDb = categoryDao.findByStringId(category).subscribeOn(Schedulers.io()).blockingGet()
            if(categoryDb == null) {
                throw IllegalStateException("Category does not exist")
            }
            item = TransactionItemDb(null, -1, amount, null, 1, description, "", null, categoryDb.id!!, "", "", "")
            mainDetailsSet = true
            return this
        }

        override fun otherDetails(brand: String?, quantity: Int, variation: String, referenceNumber: String?): TransactionItemBuilder {
            item = item.copy(brand = brand, quantity = quantity, variation = variation, referenceNumber = referenceNumber)
            return this
        }

        override fun withImages(vararg resource: TestData.Resource): TransactionItemBuilder {
            this.images = resource.toList()
            return this
        }

        override fun build(): TransactionBuilder {
            if(!mainDetailsSet ||
                item.amount.compareTo(BigDecimal.ZERO) == 0 || item.description.isBlank() || item.primaryCategoryId == -1L) {
                throw IllegalStateException("Item not properly initialized")
            }
            if(images.isEmpty()) {
                xTransactionBuilderImpl.addItem(item)
            } else {
                xTransactionBuilderImpl.addItem(item, images)
            }
            return xTransactionBuilderImpl
        }
    }
}

interface Commit {
    /**
     * Returns the list of transaction IDs
     */
    fun commit(): List<Long>
}
interface TransactionBuilder: Commit {
    fun withItem(description: String): TransactionBuilder
    fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder
    fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder
    fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder
    fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder
    fun withDetailedItem(): TransactionItemBuilder
    fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder

    /**
     * Inclusive, Inclusive
     */
    fun repeatIntoDateRange(startDate: LocalDate, endDate: LocalDate): TransactionBuilder
    fun atDate(date: LocalDate): TransactionBuilder
}

interface TransactionItemBuilder: Commit {
    fun mainDetails(description: String, category: String, amount: BigDecimal): TransactionItemBuilder
    fun otherDetails(brand: String?, quantity: Int, variation: String, referenceNumber: String?): TransactionItemBuilder
    fun withImages(vararg resource: TestData.Resource): TransactionItemBuilder
    fun build(): TransactionBuilder
}