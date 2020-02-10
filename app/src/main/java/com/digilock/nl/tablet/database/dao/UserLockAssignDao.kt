package com.digilock.nl.tablet.database.dao

import android.arch.persistence.room.*
import com.digilock.nl.tablet.data.UserLockAssign
import com.digilock.nl.tablet.util.DB_QUERY_ALL_USERLOCKASSIGNS
import com.digilock.nl.tablet.util.DB_QUERY_USERLOCKASSIGNS_BY_USERID
import io.reactivex.Flowable

/**
 * - UserLockAssignDao defining the method for [UserLockAssign]
 */

@Dao
interface UserLockAssignDao {

    @Query(DB_QUERY_ALL_USERLOCKASSIGNS)
    fun getAllUserLockAssigns(): Flowable<List<UserLockAssign>>

    @Query(DB_QUERY_USERLOCKASSIGNS_BY_USERID)
    fun getLockAssignsByUserId(userId: Int): Flowable<List<UserLockAssign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserLockAssigns(userLockAssigns: List<UserLockAssign>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUserLockAssigns(userLockAssigns: List<UserLockAssign>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserLockAssign(userLockAssign: UserLockAssign)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUserLockAssign(userAssign: UserLockAssign)

    @Delete
    fun removeUserLockAssign(userLockAssign: UserLockAssign)

    @Delete
    fun removeUserLockAssigns(userLockAssigns: List<UserLockAssign>)
}