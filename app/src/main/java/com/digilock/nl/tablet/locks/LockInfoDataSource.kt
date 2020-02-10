package com.digilock.nl.tablet.locks

import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.data.UserLockAssign
import io.reactivex.Flowable
import io.reactivex.Observable

interface LockInfoDataSource {
    fun obtainLocks(): Flowable<List<Lock>>
    fun getLock(lockID: Int): Lock?
    fun lockNameExists(lockName: String): Boolean
    fun lockSNExists(lockSN: String): Boolean
    fun insertNewLock(permit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String): Observable<Boolean>
    fun updateLockInfo(lockId: Int, permit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String): Observable<Boolean>
    fun deleteLock(lock: Lock): Observable<List<Lock>>
    fun obtainUserLockAssigns(): Flowable<List<UserLockAssign>>
}