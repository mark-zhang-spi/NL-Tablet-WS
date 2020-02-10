package com.digilock.nl.tablet.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.digilock.nl.tablet.data.UserCredAssign
import com.digilock.nl.tablet.database.dao.UserCredAssignDao

/**
 * - UserCredAssignsDatabase database class for [UserCredAssignDao]
 */
@Database(entities = [(UserCredAssign::class)], version = 1, exportSchema = false)
abstract class UserCredAssignsDatabase: RoomDatabase() {
    abstract fun userCredAssignDao(): UserCredAssignDao
}