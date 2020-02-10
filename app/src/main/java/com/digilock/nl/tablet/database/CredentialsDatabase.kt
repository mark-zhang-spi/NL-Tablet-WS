package com.digilock.nl.tablet.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.digilock.nl.tablet.data.Credential
import com.digilock.nl.tablet.database.dao.CredentialDao

/**
 * - CredentialsDatabase database class for [CredentialDao]
 */
@Database(entities = [(Credential::class)], version = 1, exportSchema = false)
abstract class CredentialsDatabase: RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
}