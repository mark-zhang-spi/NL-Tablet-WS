package com.digilock.nl.tablet.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.digilock.nl.tablet.data.User
import com.digilock.nl.tablet.database.dao.UserDao

/**
 * - UsersDatabase database class for [UserDao]
 */
@Database(entities = [(User::class)], version = 1, exportSchema = false)
abstract class UsersDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
}