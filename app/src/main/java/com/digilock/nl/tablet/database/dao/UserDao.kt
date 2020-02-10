package com.digilock.nl.tablet.database.dao

import android.arch.persistence.room.*
import com.digilock.nl.tablet.data.User
import com.digilock.nl.tablet.util.DB_QUERY_ALL_ACTIVE_USERS
import com.digilock.nl.tablet.util.DB_QUERY_ALL_USERS
import io.reactivex.Flowable

/**
 * - UserDao defining the method for [User]
 */

@Dao
interface UserDao {

    @Query(DB_QUERY_ALL_USERS)
    fun getAllUsers(): Flowable<List<User>>

    @Query(DB_QUERY_ALL_ACTIVE_USERS)
    fun getAllActiveUsers(): Flowable<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<User>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUser(user: User)

    @Delete
    fun removeUser(user: User)
}