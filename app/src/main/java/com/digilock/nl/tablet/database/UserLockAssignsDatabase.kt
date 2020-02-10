package com.digilock.nl.tablet.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.digilock.nl.tablet.data.UserLockAssign
import com.digilock.nl.tablet.database.dao.UserLockAssignDao

/**
 * - UserLockAssignsDatabase database class for [UserLockAssignDao]
 */
@Database(entities = [(UserLockAssign::class)], version = 1, exportSchema = false)
abstract class UserLockAssignsDatabase: RoomDatabase() {
    abstract fun userLockAssignDao(): UserLockAssignDao
}