package com.digilock.nl.tablet.database.dao

import android.arch.persistence.room.*
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.util.DB_QUERY_ALL_LOCKS
import io.reactivex.Flowable

/**
 * - LockDao defining the method for [Lock]
 */

@Dao
interface LockDao {
    @Query(DB_QUERY_ALL_LOCKS)
    fun getAllLocks(): Flowable<List<Lock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocks(locks: List<Lock>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateLocks(locks: List<Lock>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLock(lock: Lock)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateLock(lock: Lock)

    @Delete
    fun removeLock(lock: Lock)
}