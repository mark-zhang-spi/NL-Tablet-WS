package com.digilock.nl.tablet.users

import com.digilock.nl.tablet.data.*
import io.reactivex.Flowable
import io.reactivex.Observable

interface UserInfoDataSource {
    fun obtainCredentials(): Flowable<List<Credential>>

    fun obtainUsers(): Flowable<List<User>>
    fun getUser(userID: Int): User?

    fun obtainLocks(): Flowable<List<Lock>>
    fun getPermitLocks(): List<Lock>
    fun updatePermitLocks()

    fun obtainLockAssignsByUserId(userId: Int): Flowable<List<UserLockAssign>>

    fun obtainCredAssignsByUserId(userId: Int): Flowable<Byte>

    fun updateCredPermit(add: Boolean, cred: Byte)

    fun updateLockAssignment(position: Int, isChecked: Boolean)

    fun getLockAssignment(position: Int): Boolean

    fun userNameExists(fstName: String, lstName: String): Boolean

    fun insertNewUser(permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String): Observable<Int>
    fun updateUserInfo(userID: Int, permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String): Observable<Boolean>

    fun getWaitedCredentialType(): Byte

    fun removeWaitedCredentialType()

    fun updateDatabase(): Observable<Boolean>

    fun credAssignExists(credentialSN: String): Boolean

    fun saveCredential(credentialSN: String)

    fun transferCredential(credSN: String)

    fun disableUser(userID: Int): Observable<Boolean>
}