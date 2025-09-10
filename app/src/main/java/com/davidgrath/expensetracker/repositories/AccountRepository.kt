package com.davidgrath.expensetracker.repositories

import android.util.Log
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.entities.db.AccountDb
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository
@Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAccountByIdSingle(id: Long): Single<AccountDb> {
        return accountDao.findByIdSingle(id).toSingle()
    }
}