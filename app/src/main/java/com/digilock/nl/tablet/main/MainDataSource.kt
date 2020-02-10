package com.digilock.nl.tablet.main

import android.content.Context
import com.digilock.nl.tablet.bluetooth.Report
import com.digilock.nl.tablet.comm.CommPacket
import com.digilock.nl.tablet.data.*
import io.reactivex.Flowable
import io.reactivex.Observable

interface MainDataSource {
    fun getSendPacket(cmd: Byte): CommPacket

    fun obtainLocks(): Flowable<List<Lock>>
    fun obtainUsers(): Flowable<List<User>>
    fun obtainCredentials(): Flowable<List<Credential>>
    fun obtainUserLockAssigns(): Flowable<List<UserLockAssign>>
    fun obtainUserCredAssigns(): Flowable<List<UserCredAssign>>
    fun updateUserCredPermit(userID: Int): Observable<Boolean>

    fun filterLocks(position: Int): List<Lock>
    fun getAllLockCount(): Int
    fun getLocks(): List<Lock>
    fun getLockID(position: Int): Int
    fun getLock(position: Int): Lock
    fun getLockFunc(lockName: String): String

    fun filterUsers(position: Int): List<User>
    fun getAllUserCount(): Int
    fun getUsers(): List<User>
    fun getUserID(position: Int): Int
    fun userNameExists(fstName: String, lstName: String): Boolean
    fun disableUser(user: User): Observable<Boolean>
    fun enableUser(user: User): Observable<Boolean>
    fun getAssignedLockNames(user: User): List<String>

    fun exportReport(context: Context, reportType: Byte, dstFolder: String): Observable<Boolean>

    fun getFilterAuditTrailSize(): Int
    fun getFilterAuditTrails(): List<Report>
    fun clearAuditTrails()
    fun addAuditTrail(report: Report)
    fun filterAuditTrails(auditFilter: Byte)
    fun filterAuditTrailByLockName(lockName: String)

    fun addLockUsageInfo(lockUsageInfo: Report)
    fun filterLockUsageInfos(lockUsageInfoFilter: Byte)
    fun getFilterLockUsageInfos(): List<Report>
    fun getFilterLockUsageInfoSize(): Int
    fun getLockUsageInfoSize(): Int
    fun clearLockUsageInfos()

    fun genAssignedStatusLocks(hasUser: Boolean)
    fun getAssignedUseLocks(): List<Lock>
    fun getAssignedStatusReportSize(): Int

    fun sortByLockName(sortMode: Byte)
    fun sortByLockFunc(sortMode: Byte)
    fun sortByUserName(sortMode: Byte)
    fun sortByLockAction(sortMode: Byte)
    fun sortByAuditDT(sortMode: Byte)
    fun sortByCredType(sortMode: Byte)

    fun getSyncControllerPacketList(reassign: Boolean): ArrayList<CommPacket>
    fun getEmptyAuditPacket(): CommPacket
    fun getDoAuditPacketByName(lockName: String): CommPacket
    fun getDoAuditPacketByFuncType(lockFuncType: Byte): CommPacket

    fun getLockAssignUsers(lockName: String): Array<String?>

    fun getDoLockUsageInfoPacket(): CommPacket

    fun getLockName(lockSN: String): String
    fun getUserName(credSN: String, sDateTime: String): String


    fun updateLockStatus(lockSN: String, lockStatus: Byte): Observable<List<Lock>>


    fun getLockNameBylockSN(lockSN: String): String


    fun getToggleLockStatePacket(lockSN: String): CommPacket
    fun getLockStatusPacket(lockSN: String): CommPacket

    fun updateLockState(packet: CommPacket): Observable<List<Lock>>

    fun savePairedController(name: String, address: String)
    fun getPairedControllerIPAddress(): String
    fun getPairedControllerName(): String

    fun downloadDB(context: Context): Observable<Boolean>
    fun restoreDB(context: Context): Observable<Boolean>

    fun pingNetwork(context: Context): Observable<Boolean>
}