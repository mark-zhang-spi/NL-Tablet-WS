package com.digilock.nl.tablet.database.dao

import android.arch.persistence.room.*
import com.digilock.nl.tablet.data.UserCredAssign
import com.digilock.nl.tablet.util.DB_QUERY_ALL_USERCREDASSIGNS
import com.digilock.nl.tablet.util.DB_QUERY_USERCREDASSIGNS_BY_USERID
import com.digilock.nl.tablet.util.DB_REMOVE_ALL_USERCREDASSIGNS
import io.reactivex.Flowable


/**
 * - UserCredAssignDao defining the method for [UserCredAssign]
 */

@Dao
interface UserCredAssignDao {

    @Query(DB_QUERY_ALL_USERCREDASSIGNS)
    fun getAllUserCredAssigns(): Flowable<List<UserCredAssign>>

    @Query(DB_QUERY_USERCREDASSIGNS_BY_USERID)
    fun getCredAssignsByUserId(userId: Int): Flowable<List<UserCredAssign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserCredAssigns(userCredAssigns: List<UserCredAssign>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUserCredAssigns(userCredAssigns: List<UserCredAssign>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserCredAssign(userCredAssign: UserCredAssign)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUserCredAssign(userCredAssign: UserCredAssign)

    @Query(DB_REMOVE_ALL_USERCREDASSIGNS)
    fun removeAll()

    @Delete
    fun removeUserCredAssign(userCredAssign: UserCredAssign)
}