package com.digilock.nl.tablet.database.dao

import android.arch.persistence.room.*
import com.digilock.nl.tablet.data.Credential
import com.digilock.nl.tablet.util.DB_QUERY_ALL_CREDS
import io.reactivex.Flowable

/**
 * - CredentialDao defining the method for [Credential]
 */

@Dao
interface CredentialDao {

    @Query(DB_QUERY_ALL_CREDS)
    fun getAllCredentials(): Flowable<List<Credential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCredentials(users: List<Credential>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCredentials(creds: List<Credential>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCredential(cred: Credential)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCredential(cred: Credential)

    @Delete
    fun removeCredential(cred: Credential)
}