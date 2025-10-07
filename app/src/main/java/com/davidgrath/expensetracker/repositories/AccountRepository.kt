package com.davidgrath.expensetracker.repositories

import android.util.Log
import com.davidgrath.expensetracker.dateTimeOffsetZone
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.di.TimeHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository
@Inject constructor(
    private val accountDao: AccountDao,
    private val timeHandler: TimeHandler
) {
    fun getAccountByIdSingle(id: Long): Maybe<AccountDb> {
        return accountDao.findByIdSingle(id)
            .subscribeOn(Schedulers.io())
    }

    fun getAccountsForProfile(profileId: Long): Observable<List<AccountDb>> {
        return accountDao.getAllByProfileId(profileId)
            .subscribeOn(Schedulers.io())
            .doOnNext {
                LOGGER.info("getAccountsForProfile: {} items", it.size)
            }
    }
    fun getAccountsForProfileSingle(profileId: Long): Single<List<AccountDb>> {
        return accountDao.getAllByProfileIdSingle(profileId)
            .subscribeOn(Schedulers.io())
            .doOnSuccess {
                LOGGER.info("getAccountsForProfileSingle: {} items", it.size)
            }
    }

    fun createAccount(profileId: Long, name: String, currencyCode: String): Single<Long> { //TODO ISO 4217 validation
        val (dateTime, offset, zone) = dateTimeOffsetZone(timeHandler.getClock())
        val account = AccountDb(
            null,
            profileId,
            currencyCode,
            null,
            "",
            name,
            dateTime,
            offset,
            zone
        )
        return accountDao.insertAccount(account).subscribeOn(Schedulers.io())
    }

    fun editAccountName(accountId: Long, name: String): Single<Int> {
        return accountDao.updateAccountName(accountId, name).subscribeOn(Schedulers.io())
    }

    fun deleteAccount(profileId: Long, accountId: Long): Single<Int> {
        return accountDao.getAllByProfileIdSingle(profileId).flatMap {
            if(it.size <= 1) {
                val exception = Exception("Cannot delete account when only 1 left for profile")
                LOGGER.error("deleteAccount", exception)
                Single.error(exception)
            } else {
                accountDao.deleteAccountById(profileId, accountId).subscribeOn(Schedulers.io())
                    .doOnSuccess {
                        LOGGER.info("deleteAccount result: {}", it)
                    }
            }
        }.subscribeOn(Schedulers.io())
    }

    companion object {
        private final val LOGGER = LoggerFactory.getLogger(AccountRepository::class.java)
    }
}