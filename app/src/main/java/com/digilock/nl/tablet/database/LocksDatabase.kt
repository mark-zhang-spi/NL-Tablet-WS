package com.digilock.nl.tablet.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.database.dao.LockDao

/**
 * - LocksDatabase database class for [LockDao]
 */
@Database(entities = [(Lock::class)], version = 1, exportSchema = false)
abstract class LocksDatabase: RoomDatabase() {
    abstract fun lockDao(): LockDao
}