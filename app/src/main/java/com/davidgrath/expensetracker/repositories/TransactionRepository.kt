package com.davidgrath.expensetracker.repositories

import com.davidgrath.expensetracker.db.dao.TempImagesDao
import com.davidgrath.expensetracker.db.dao.TempTransactionDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemDao
import com.davidgrath.expensetracker.db.dao.TempTransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.util.SortedMap
import java.util.TreeMap

class TransactionRepository(private val transactionDao: TempTransactionDao, private val transactionItemDao: TempTransactionItemDao, private val transactionItemImagesDao: TempTransactionItemImagesDao, private val imagesDao: TempImagesDao) {

    fun addTransaction(amount: BigDecimal, description: String, categoryId: Long): Single<Long> {
        val date = ZonedDateTime.now()
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val dateString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val offset = date.offset.id
        val zone = date.zone.id
        val transaction = TransactionDb(null, 0, amount, "USD", false, dateString, offset, zone, dateString, offset, zone)
        return transactionDao.addTransaction(transaction).flatMap { id ->
            val item = TransactionItemDb(null, id, amount, null, 1, description, "", null, categoryId, dateString, offset, zone)
            transactionItemDao.addTransactionItem(item).map { id }
        }
    }

    fun addTransaction(transactionDb: TransactionDb, items: List<TransactionItemDb>): Single<List<Long>> {
        return transactionDao.addTransaction(transactionDb).flatMap { id ->
            val idItems = items.map { it.copy(transactionId = id) }
            transactionItemDao.addTransactionItems(idItems)

        }
    }

    fun addTransaction(transactionDb: TransactionDb): Single<Long> {
        return transactionDao.addTransaction(transactionDb)
    }
    fun addTransactionItem(item: TransactionItemDb): Single<Long> {
        return transactionItemDao.addTransactionItem(item)
    }
    fun getTransactions(): Observable<SortedMap<TransactionDb, List<TransactionItemDb>>> {
        //TODO Terrible solution
        return Observable.combineLatest(transactionDao.getTransactions(), transactionItemDao.getTransactionItems(), transactionItemImagesDao.getTransactionItemImages()) { transactions, items, itemImages ->
            transactions
        }.map { transactions ->
            val map = TreeMap<TransactionDb, List<TransactionItemDb>>()
            for(t in transactions) {
                val items = transactionItemDao.getTransactionItemsSingle(t.id!!).subscribeOn(Schedulers.io()).blockingGet()
                map.put(t, items)
            }
            map
        }
    }

    fun getTransactionItemImages(itemId: Long): Single<List<ImageDb>> {
        return transactionItemImagesDao.getTransactionItemImagesByItemIdSingle(itemId)
            .map {  list ->
                list.map { itemImage -> imagesDao.findById(itemImage.imageID) }
            }
    }
}