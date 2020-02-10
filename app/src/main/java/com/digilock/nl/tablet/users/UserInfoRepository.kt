package com.digilock.nl.tablet.users

import android.content.SharedPreferences
import com.digilock.nl.tablet.data.*
import com.digilock.nl.tablet.database.dao.*
import com.digilock.nl.tablet.util.CREDENTIAL_MOBILEID
import com.digilock.nl.tablet.util.CREDENTIAL_PINCODE
import com.digilock.nl.tablet.util.CREDENTIAL_RFID
import com.digilock.nl.tablet.util.constants.credentialPermits
import com.digilock.nl.tablet.util.sdtf
import io.reactivex.Flowable
import io.reactivex.Observable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor


class UserInfoRepository(private val userDao: UserDao,
                         private val lockDao: LockDao,
                         private val credDao: CredentialDao,
                         private val userLockAssignDao: UserLockAssignDao,
                         private val userCredAssignDao: UserCredAssignDao,
                         private val sysPref: SharedPreferences): UserInfoDataSource {

    private var users = ArrayList<User>()
    private var locks = ArrayList<Lock>()
    private var permitLocks = ArrayList<Lock>()
    private var credentials = ArrayList<Credential>()
    private var newCredentials = ArrayList<Credential>()
    private var userLockAssigns = ArrayList<UserLockAssign>()

    private var allUserCredAssigns = ArrayList<UserCredAssign>()
    private var userCredAssigns = ArrayList<UserCredAssign>()
    private var newUserCredAssigns = ArrayList<UserCredAssign>()

    private var credTypeMap = HashMap<String, Byte>()              // 1: RFID, 2: Mobile ID, 4: Pin Code
    private var lockIDAssignMap = HashMap<Int, Boolean>()             // Current user lock assignments

    private var orgCredentialTypes: Byte = 0
    private var curCredentialTypes: Byte = 0

    private var allWaitedCredentialTypes: Byte = 0
    private var curWaitedCredentialType: Byte = 0

    private var userID: Long = 0


    override fun obtainCredentials(): Flowable<List<Credential>> {
        return credDao.getAllCredentials()
                .doOnNext {
                    credentials.clear()
                    credentials.addAll(it)

                    credTypeMap.clear()
                    credentials.forEach {credential ->
                        credTypeMap[credential.credSN] = credential.credType
                    }
                }
    }

    override fun obtainUsers(): Flowable<List<User>> {
        return userDao.getAllActiveUsers()
                .doOnNext {
                    users.clear()
                    users.addAll(it)
                }
    }

    override fun getUser(userID: Int): User? {
        users.forEach {  user ->
            if(user.userId == userID)   return user
        }

        return null
    }

    override fun obtainLocks(): Flowable<List<Lock>> {
        return lockDao.getAllLocks()
                .doOnNext {
                    locks.clear()
                    locks.addAll(it)

                    lockIDAssignMap.clear()
                    locks.forEach {lock ->
                        lockIDAssignMap[lock.lockId] = false
                    }
                }
    }

    override fun updateCredPermit(add: Boolean, cred: Byte) {
        if(add) {
            curCredentialTypes = curCredentialTypes or cred
        } else {
            curCredentialTypes = curCredentialTypes xor cred
        }

        updatePermitLocks()
    }

    override fun getPermitLocks(): List<Lock> {
        return permitLocks
    }

    override fun updatePermitLocks() {
        permitLocks.clear()

        locks.forEach {lock ->
            if(curCredentialTypes.toInt() and lock.credPermit.toInt() != 0)  permitLocks.add(lock)
        }
    }

    override fun obtainLockAssignsByUserId(userId: Int): Flowable<List<UserLockAssign>> {
        return userLockAssignDao.getLockAssignsByUserId(userId)
                .doOnNext {
                    userLockAssigns.clear()
                    userLockAssigns.addAll(it)

                    userLockAssigns.forEach { userLockAssign ->
                        lockIDAssignMap[userLockAssign.lockId] = true
                    }
                }
    }

    override fun obtainCredAssignsByUserId(userId: Int): Flowable<Byte> {
        return userCredAssignDao.getAllUserCredAssigns()
                .map {
                    allUserCredAssigns.clear()
                    allUserCredAssigns.addAll(it)

                    userCredAssigns.clear()
                    allUserCredAssigns.forEach {userCredAssign ->
                        if(userCredAssign.userId.equals(userId) and userCredAssign.isActive) userCredAssigns.add(userCredAssign)
                    }

                    userCredAssigns.forEach {userCredAssign ->
                        if(credTypeMap.containsKey(userCredAssign.credSN)) {
                            orgCredentialTypes = orgCredentialTypes or credTypeMap[userCredAssign.credSN]!!
                        }
                    }

                    return@map orgCredentialTypes
                }
    }

    override fun updateLockAssignment(position: Int, isChecked: Boolean){
        lockIDAssignMap[permitLocks[position].lockId] = isChecked
    }

    override fun getLockAssignment(position: Int): Boolean {
        if(position >= permitLocks.size) return false

        return lockIDAssignMap[permitLocks[position].lockId]!!
    }


    override fun userNameExists(fstName: String, lstName: String): Boolean {
        users.forEach { user ->
            if(user.userState && (user.fstName+user.lstName).equals(fstName+lstName)) return true
        }

        return false
    }

    override fun insertNewUser(permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String): Observable<Int> {
        val startDT = sdtf.format(Date())
        return Observable.just(true)
                .map {
                    userID = userDao.insertUser(User(userState = true, startDate = startDT, fstName = fstName, lstName = lstName, dept = dept, email = email, phoneNum = phone, notes = notes))

                    allWaitedCredentialTypes = permit
                    if(allWaitedCredentialTypes and CREDENTIAL_RFID == CREDENTIAL_RFID)   curWaitedCredentialType = CREDENTIAL_RFID
                    else if(allWaitedCredentialTypes and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)   curWaitedCredentialType = CREDENTIAL_MOBILEID
                    else if(allWaitedCredentialTypes and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)   curWaitedCredentialType = CREDENTIAL_PINCODE
                    else curWaitedCredentialType = 0

                    return@map userID.toInt()
                }
    }

    override fun updateUserInfo(userId: Int, permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String): Observable<Boolean> {
        val user = getUser(userId)
        return Observable.just(true)
                .doOnNext {
                    userDao.updateUser(User(userId = userId, userState = true, startDate = user!!.startDate, fstName = fstName, lstName = lstName, dept = dept, email = email, phoneNum = phone, notes = notes))

                    decCredentialTypes(permit)      // Update in here first

                    allWaitedCredentialTypes = getIncCredentialTypes(permit)
                    updateCurWaitedCredentialType()

                    userID = userId.toLong()
                }
    }

    private fun updateCurWaitedCredentialType() {
        if(allWaitedCredentialTypes and CREDENTIAL_RFID == CREDENTIAL_RFID)   curWaitedCredentialType = CREDENTIAL_RFID
        else if(allWaitedCredentialTypes and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)   curWaitedCredentialType = CREDENTIAL_MOBILEID
        else if(allWaitedCredentialTypes and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)   curWaitedCredentialType = CREDENTIAL_PINCODE
        else curWaitedCredentialType = 0
    }

    private fun decCredentialTypes(permit: Byte) {
        var decCredentialTypes: Byte = 0

        if(orgCredentialTypes and CREDENTIAL_RFID == CREDENTIAL_RFID) {
            if(permit and CREDENTIAL_RFID == 0.toByte())    decCredentialTypes = decCredentialTypes or CREDENTIAL_RFID
        }

        if(orgCredentialTypes and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID) {
            if(permit and CREDENTIAL_MOBILEID == 0.toByte())    decCredentialTypes = decCredentialTypes or CREDENTIAL_MOBILEID
        }

        if(orgCredentialTypes and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE) {
            if(permit and CREDENTIAL_PINCODE == 0.toByte())    decCredentialTypes = decCredentialTypes or CREDENTIAL_PINCODE
        }

        // Terminate assigned credentials
        val endDT = sdtf.format(Date())
        if(decCredentialTypes and CREDENTIAL_RFID == CREDENTIAL_RFID) {
            userCredAssigns.forEach {userCredAssign ->
                if(credTypeMap[userCredAssign.credSN] == CREDENTIAL_RFID) {
                    userCredAssign.isActive = false
                    userCredAssign.endDate = endDT
                }
            }
        }

        if(decCredentialTypes and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID) {
            userCredAssigns.forEach {userCredAssign ->
                if(credTypeMap[userCredAssign.credSN] == CREDENTIAL_MOBILEID) {
                    userCredAssign.isActive = false
                    userCredAssign.endDate = endDT
                }
            }
        }

        if(decCredentialTypes and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE) {
            userCredAssigns.forEach {userCredAssign ->
                if(credTypeMap[userCredAssign.credSN] == CREDENTIAL_PINCODE) {
                    userCredAssign.isActive = false
                    userCredAssign.endDate = endDT
                }
            }
        }
    }

    private fun getIncCredentialTypes(permit: Byte): Byte {
        var credentialTypes: Byte = 0

        if(permit and CREDENTIAL_RFID == CREDENTIAL_RFID) {
            if(orgCredentialTypes and CREDENTIAL_RFID == 0.toByte())    credentialTypes = credentialTypes or CREDENTIAL_RFID
        }

        if(permit and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID) {
            if(orgCredentialTypes and CREDENTIAL_MOBILEID == 0.toByte())    credentialTypes = credentialTypes or CREDENTIAL_MOBILEID
        }

        if(permit and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE) {
            if(orgCredentialTypes and CREDENTIAL_PINCODE == 0.toByte())    credentialTypes = credentialTypes or CREDENTIAL_PINCODE
        }

        return credentialTypes
    }

    override fun getWaitedCredentialType(): Byte {
        return curWaitedCredentialType
    }

    override fun removeWaitedCredentialType() {
        allWaitedCredentialTypes = allWaitedCredentialTypes xor curWaitedCredentialType

        updateCurWaitedCredentialType()
    }

    override fun updateDatabase(): Observable<Boolean> {
        return Observable.just(true)
                .doOnNext {
                    // Update credential table
                    newCredentials.forEach {newCredential ->
                        credDao.insertCredential(newCredential)
                    }

                    // Delete all previous user lock assignments
                    userLockAssignDao.removeUserLockAssigns(userLockAssigns)

                    userLockAssigns.clear()
                    lockIDAssignMap.forEach{
                        if(it.value) {
                            userLockAssigns.add(UserLockAssign(userId = userID.toInt(), lockId = it.key))   // timeStamp will be added in future
                        }
                    }
                    // Insert new user lock assignments
                    userLockAssignDao.insertUserLockAssigns(userLockAssigns)

                    // update user credential assignment
                    userCredAssignDao.updateUserCredAssigns(userCredAssigns)
                    userCredAssignDao.updateUserCredAssigns(allUserCredAssigns)

                    newUserCredAssigns.forEach {newUserCredAssign ->
                        userCredAssignDao.insertUserCredAssign(newUserCredAssign)
                    }
                }
    }

    override fun credAssignExists(credentialSN: String): Boolean {
        allUserCredAssigns.forEach {userCredAssign ->
            if(userCredAssign.credSN.equals(credentialSN) and userCredAssign.isActive) return true
        }

        return false
    }

    override fun saveCredential(credentialSN: String) {
        val startDT = sdtf.format(Date())

        var bCredExists = false
        credentials.forEach {credential ->
            if(credential.credSN.equals(credentialSN))  bCredExists = true
        }

        if(!bCredExists)    newCredentials.add(Credential(credentialSN, curWaitedCredentialType))
        newUserCredAssigns.add(UserCredAssign(userId = userID.toInt(), credSN = credentialSN, isActive = true, startDate = startDT))

        removeWaitedCredentialType()
    }

    override fun transferCredential(credentialSN: String) {
        val sDT = sdtf.format(Date())

        newUserCredAssigns.add(UserCredAssign(userId = userID.toInt(), credSN = credentialSN, isActive = true, startDate = sDT))

        allUserCredAssigns.forEach {userCredAssign ->
            if(userCredAssign.credSN.equals(credentialSN) and userCredAssign.isActive) {
                userCredAssign.endDate = sDT
                userCredAssign.isActive = false
            }
        }

        removeWaitedCredentialType()
    }

    override fun disableUser(userID: Int): Observable<Boolean> {
        var user: User? = null

        users.forEach {
            if(it.userId == userID)   user = it
        }
        if(user == null) {
            return Observable.just(false)
        }

        user!!.userState = false
        user!!.endDate = sdtf.format(Date())

        val deletedUserLockAssigns = ArrayList<UserLockAssign>()
        val updUserCredAssigns = ArrayList<UserCredAssign>()

        return Observable.just(true)
                .doOnNext {
                    userLockAssigns.forEach {
                        if(it.userId == user!!.userId) {
                            deletedUserLockAssigns.add(it)
                        }
                    }
                    if(deletedUserLockAssigns.size > 0) {
                        userLockAssigns.removeAll(deletedUserLockAssigns)
                        userLockAssignDao.removeUserLockAssigns(deletedUserLockAssigns)
                    }

                    userCredAssigns.forEach {it ->
                        if((it.userId == user!!.userId) and it.isActive) {
                            it.isActive = false
                            it.endDate = sdtf.format(Date())
                            updUserCredAssigns.add(it)
                        }
                    }
                    if(updUserCredAssigns.size > 0) userCredAssignDao.updateUserCredAssigns(updUserCredAssigns)

                    userDao.updateUser(user!!)
                    users.remove(user!!)
                }
    }

    companion object {
        private val LOG_TAG: String = UserInfoRepository::class.java.simpleName
    }
}