package com.digilock.nl.tablet.locks

import android.content.SharedPreferences
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.data.UserLockAssign
import com.digilock.nl.tablet.database.dao.LockDao
import com.digilock.nl.tablet.database.dao.UserLockAssignDao
import io.reactivex.Flowable
import io.reactivex.Observable

import java.util.*
import kotlin.collections.ArrayList


class LockInfoRepository(private val lockDao: LockDao,
                         private val userLockAssignDao: UserLockAssignDao,
                         private val sysPref: SharedPreferences ): LockInfoDataSource {

    private var locks = ArrayList<Lock>()
    private var userLockAssigns = ArrayList<UserLockAssign>()

    override fun obtainLocks(): Flowable<List<Lock>> {
        return lockDao.getAllLocks()
                .doOnNext {
                    locks.clear()
                    locks.addAll(it)
                }
    }

    override fun obtainUserLockAssigns(): Flowable<List<UserLockAssign>> {
        return userLockAssignDao.getAllUserLockAssigns()
                .doOnNext {
                    userLockAssigns.clear()
                    userLockAssigns.addAll(it)

                    userLockAssigns.forEach { userLockAssign ->
//                        lockAssignMap[userLockAssign.lockId] = true
                    }
                }
    }

    override fun getLock(lockID: Int): Lock? {
        locks.forEach {  lock ->
            if(lock.lockId == lockID)   return lock
        }

        return null
    }

    override fun lockNameExists(lockName: String): Boolean {
        locks.forEach {  lock ->
            if(lock.lockName.toLowerCase().equals(lockName.toLowerCase()))  return true
        }

        return false
    }

    override fun lockSNExists(lockSN: String): Boolean {
        locks.forEach {  lock ->
            if(lock.lockSN.equals(lockSN))  return true
        }

        return false
    }

    override fun insertNewLock(permit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String): Observable<Boolean> {
        return Observable.just(true)
                .doOnNext {
                    lockDao.insertLock(Lock(lockSN = lockSN, lockName = lockName, lockLocation = location, lockNotes = desc, lockType = lockType, lockFunc = lockFunc, credPermit = permit))
                }

    }

    override fun updateLockInfo(lockId: Int, permit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String): Observable<Boolean> {
        return Observable.just(true)
                .doOnNext {
                    lockDao.updateLock(Lock(lockId= lockId, lockSN = lockSN, lockName = lockName, lockLocation = location, lockNotes = desc, lockType = lockType, lockFunc = lockFunc, credPermit = permit))
                }
    }

    override fun deleteLock(lock: Lock): Observable<List<Lock>> {
        val deletedUserLockAssigns = ArrayList<UserLockAssign>()

        return Observable.just(true)
                .map {
                    userLockAssigns.forEach {
                        if(it.lockId == lock.lockId) {
                            deletedUserLockAssigns.add(it)
                        }
                    }

                    if(userLockAssigns.size > 0) {
                        userLockAssigns.removeAll(deletedUserLockAssigns)
                        userLockAssignDao.removeUserLockAssigns(deletedUserLockAssigns)
                    }

                    lockDao.removeLock(lock)
                    locks.remove(lock)

                    return@map locks
                }
    }


    companion object {
        private val LOG_TAG: String = LockInfoRepository::class.java.simpleName
    }

}