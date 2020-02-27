package com.digilock.nl.tablet.main

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.digilock.nl.tablet.bluetooth.Report
import com.digilock.nl.tablet.comm.CommPacket
import com.digilock.nl.tablet.data.*
import com.digilock.nl.tablet.database.dao.*
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import com.digilock.nl.tablet.websocket.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.digilock.nl.tablet.websocket.WsClientService.Companion.LOG_TAG
import io.reactivex.Flowable
import io.reactivex.Observable
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.*
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.experimental.or


class MainRepository(private val userDao: UserDao,
                     private val lockDao: LockDao,
                     private val credDao: CredentialDao,
                     private val userCredAssignDao: UserCredAssignDao,
                     private val userLockAssignDao: UserLockAssignDao,
                     private val sysPref: SharedPreferences
                     ): MainDataSource {

    private var locks = ArrayList<Lock>()

    private var allUsers = ArrayList<User>()
    private var users = ArrayList<User>()               // Active users
    private var disabledUsers = ArrayList<User>()       // Disabled users
    private var credentials = ArrayList<Credential>()
    private var userLockAssigns = ArrayList<UserLockAssign>()
    private var userCredAssigns = ArrayList<UserCredAssign>()

    private var userIDNameMap = HashMap<Int, String>()

    private var credTypeMap = HashMap<String, Byte>()               // 1: RFID, 2: Mobile ID, 4: Pin Code
    private var lockAssignMap = HashMap<Int, Boolean>()             // Current user lock assignments
    private var lockNameIDMap = HashMap<String, Int>()
    private var lockIDNameMap = HashMap<Int, String>()
    private var lockNameSNMap = HashMap<String, String>()
    private var lockSNNameMap = HashMap<String, String>()
    private var lockIDPermitMap = HashMap<Int, Byte>()
    private var lockIDFuncMap = HashMap<Int, String>()

    private var auditTrails = ArrayList<Report>()
    private var filterAuditTrails = ArrayList<Report>()

    private var lockUsageInfos = ArrayList<Report>()
    private var filterLockUsageInfos = ArrayList<Report>()

    private var assignedStatusLocks = ArrayList<Lock>()


    override fun getSendPacket(cmd: Byte): CommPacket {
        val txPacket = CommPacket()

        txPacket.executeBytes.cmd = cmd

        txPacket.fromExecuteBytesToBuffer()
        txPacket.packetBuffer[txPacket.packetSize - 1] = crc8ofByteArrayRange(txPacket.packetBuffer, 0, txPacket.packetSize - 2)

        return txPacket
    }

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


    /*
        LOCKS related
     */
    override fun obtainLocks(): Flowable<List<Lock>> {
        return lockDao.getAllLocks()
                .doOnNext {
                    locks.clear()
                    locks.addAll(it)

                    lockAssignMap.clear()
                    lockNameIDMap.clear()
                    lockIDNameMap.clear()
                    lockIDPermitMap.clear()
                    lockIDFuncMap.clear()
                    lockNameSNMap.clear()
                    lockSNNameMap.clear()
                    locks.forEach {lock ->
                        lockAssignMap[lock.lockId] = false
                        lockNameIDMap[lock.lockName] = lock.lockId
                        lockIDNameMap[lock.lockId] = lock.lockName
                        lockIDPermitMap[lock.lockId] = lock.credPermit
                        lockIDFuncMap[lock.lockId] = lock.lockFunc
                        lockNameSNMap[lock.lockName] = lock.lockSN
                        lockSNNameMap[lock.lockSN] = lock.lockName
                    }
                }
    }

    override fun filterLocks(position: Int): List<Lock> {
        var filterLocks = ArrayList<Lock>()
        when(position) {
            LOCK_FILTER_ALL.toInt() -> return locks
            LOCK_FILTER_SHARED.toInt() -> {
                locks.forEach {lock ->
                    if(lock.lockFunc.equals(LOCK_FUNC_SHARED_USE))  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_ASSIGNED.toInt() -> {
                locks.forEach {lock ->
                    if(lock.lockFunc.equals(LOCK_FUNC_ASSIGNED_USE))  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_LOCKED.toInt() -> {
                locks.forEach {lock ->
                    if(lock.lockState == STATE_LOCKED)  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_UNLOCKED.toInt() -> {
                locks.forEach {lock ->
                    if(lock.lockState == STATE_UNLOCKED)  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_STATE_UNKNOWN.toInt() -> {
                locks.forEach {lock ->
                    if(lock.lockState == STATE_UNKNOWN)  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_RFID.toInt() -> {
                locks.forEach {lock ->
                    if(lock.credPermit and CREDENTIAL_RFID == CREDENTIAL_RFID)  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_KEYPAD.toInt() -> {
                locks.forEach {lock ->
                    if(lock.credPermit and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)  filterLocks.add(lock)
                }
            }
            LOCK_FILTER_MOBILEID.toInt() -> {
                locks.forEach {lock ->
                    if(lock.credPermit and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)  filterLocks.add(lock)
                }
            }
            else -> {
            }
        }

        return filterLocks
    }

    override fun getAllLockCount(): Int {
        return locks.size
    }

    override fun getLocks(): List<Lock> {
        return locks
    }

    override fun getLockID(position: Int): Int {
        return locks[position].lockId
    }

    override fun getLock(position: Int): Lock {
        return locks[position]
    }

    override fun getToggleLockStatePacket(lockSN: String): CommPacket {
        val packet = CommPacket()

        packet.executeBytes.cmd = CMD_TOGGLE_LOCK_STATUS_BT
        val lockSNArray = lockSN.toByteArray()

        var offset = 0
        packet.executeBytes.para[offset++] = PT_LOCK_SN
        packet.executeBytes.para[offset++] = PT_LENGTH_LOCK_SN
        for(index in 0 until PT_LENGTH_LOCK_SN) {
            packet.executeBytes.para[offset++] = lockSNArray[index]
        }

        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun updateLockState(packet: CommPacket): Observable<List<Lock>> {
        val updLocks = ArrayList<Lock>()

        return Observable.just(true)
                .map {
                    val lockSN = String(byteArrayOf(packet.executeBytes.para[2], packet.executeBytes.para[3],packet.executeBytes.para[4],packet.executeBytes.para[5] ))
                    var lockState = packet.executeBytes.para[8]

                    locks.forEach {lock ->
                        if(lock.lockSN.equals(lockSN)) {
                            lock.lockState = lockState
                            updLocks.add(lock)
                        }
                    }

                    if(updLocks.size > 0)   lockDao.updateLocks(updLocks)

                   return@map locks
                }
    }

    override fun getLockFunc(lockName: String): String {
        locks.forEach {lock ->
            if(lock.lockName.equals(lockName)) {
                return lock.lockFunc
            }
        }

        return ""
    }

    /*
        USERS related
     */
    override fun obtainUsers(): Flowable<List<User>> {
        return userDao.getAllUsers()
                .doOnNext {
                    allUsers.clear()
                    allUsers.addAll(it)

                    users.clear()
                    disabledUsers.clear()
                    allUsers.forEach {
                        if(it.userState)    users.add(it)
                        else disabledUsers.add(it)
                    }

                    userIDNameMap.clear()
                    allUsers.forEach {user ->
                        userIDNameMap[user.userId] = "${user.fstName} ${user.lstName}"
                    }
                }
    }

    override fun filterUsers(position: Int): List<User> {
        var filterUsers = ArrayList<User>()
        when(position) {
            USER_FILTER_ALL -> return allUsers
            USER_FILTER_ACTIVE -> return users
            USER_FILTER_DISABLED -> return disabledUsers
            USER_FILTER_RFID -> {
                users.forEach {user ->
                    if(user.credpermit and CREDENTIAL_RFID == CREDENTIAL_RFID)  filterUsers.add(user)
                }
            }
            USER_FILTER_PINCODE -> {
                users.forEach {user ->
                    if(user.credpermit and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)  filterUsers.add(user)
                }
            }
            USER_FILTER_MOBILEID -> {
                users.forEach {user ->
                    if(user.credpermit and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)  filterUsers.add(user)
                }
            }
            else -> {
            }
        }

        return filterUsers
    }

    override fun getAllUserCount(): Int {
        return allUsers.size
    }

    override fun getUsers(): List<User> {
        return users
    }

    override fun getUserID(position: Int): Int {
        return users[position].userId
    }

    override fun userNameExists(fstName: String, lstName: String): Boolean {
        allUsers.forEach { user ->
            if(user.userState && (user.fstName+user.lstName).equals(fstName+lstName)) return true
        }

        return false
    }

    override fun disableUser(user: User): Observable<Boolean> {
        user.userState = false
        user.endDate = sdtf.format(Date())

        val deletedUserLockAssigns = ArrayList<UserLockAssign>()
        val updUserCredAssigns = ArrayList<UserCredAssign>()

        return Observable.just(true)
                .doOnNext {
                    userLockAssigns.forEach {
                        if(it.userId == user.userId) {
                            deletedUserLockAssigns.add(it)
                        }
                    }
                    if(deletedUserLockAssigns.size > 0) {
                        userLockAssigns.removeAll(deletedUserLockAssigns)
                        userLockAssignDao.removeUserLockAssigns(deletedUserLockAssigns)
                    }

                    userCredAssigns.forEach {it ->
                        if((it.userId == user.userId) and it.isActive) {
                            it.isActive = false
                            it.endDate = sdtf.format(Date())
                            updUserCredAssigns.add(it)
                        }
                    }
                    if(updUserCredAssigns.size > 0) userCredAssignDao.updateUserCredAssigns(updUserCredAssigns)

                    userDao.updateUser(user)
                    users.remove(user)
                }
    }

    override fun enableUser(user: User): Observable<Boolean> {
        val newUser = User(startDate = sdtf.format(Date()), dept = user.dept, notes = user.notes, email = user.email, phoneNum = user.phoneNum, fstName = user.fstName, lstName = user.lstName)
        return Observable.just(true)
                .doOnNext {
                    userDao.insertUser(newUser)
                }
    }

    override fun updateUserCredPermit(userID: Int): Observable<Boolean> {
        var permit: Byte = 0

        userCredAssigns.forEach {
            if(it.isActive) {
                if(it.userId == userID) {
                    val credSN = it.credSN

                    permit = permit or credTypeMap[credSN]!!
                }
            }
        }

        users.forEach {user ->
            if(user.userId == userID)   user.credpermit = permit
        }

        return Observable.just(true)
                .doOnNext{
                    userDao.updateUsers(users)
                }
    }

    override fun getAssignedLockNames(user: User): List<String> {
        val lockNames = ArrayList<String>()

        userLockAssigns.forEach {
            if(it.userId == user.userId) {
                val lockName = lockIDNameMap[it.lockId]

                if(!lockNames.contains(lockName))   lockNames.add(lockName!!)
            }
        }

        return lockNames
    }

    /*
        USERLOCKASSIGNS related
     */

    override fun obtainUserLockAssigns(): Flowable<List<UserLockAssign>> {
        return userLockAssignDao.getAllUserLockAssigns()
                .doOnNext {
                    userLockAssigns.clear()
                    userLockAssigns.addAll(it)

                    userLockAssigns.forEach { userLockAssign ->
                        lockAssignMap[userLockAssign.lockId] = true
                    }
                }
    }


    /*
        USERCREDASSIGNS related

    */
    override fun obtainUserCredAssigns(): Flowable<List<UserCredAssign>> {
        return userCredAssignDao.getAllUserCredAssigns()
                .doOnNext {
                    userCredAssigns.clear()
                    userCredAssigns.addAll(it)
                }
    }

    override fun exportReport(context: Context, reportType: Byte, dstFolder: String): Observable<Boolean> {
        return Observable.just(true)
                .doOnNext {
                    when(reportType) {
                        REPORT_AUDIT_TRAIL -> {
                            exportAuditTrailReport(context, dstFolder, TITLE_AUDIT_TRAIL)
                        }
                        REPORT_SHARED_USE_LOCKED -> {
                            exportSharedStatusReport(context, dstFolder, TITLE_SHARED_USE_LOCKED, true)
                        }
                        REPORT_SHARED_USE_UNLOCKED -> {
                            exportSharedStatusReport(context, dstFolder, TITLE_SHARED_USE_UNLOCKED, false)
                        }
                        REPORT_ASSIGNED_USE_HAS_USER -> {
                            exportAssignedStatusReport(context, dstFolder, TITLE_ASSIGNED_USE_HAS_USER, true)
                        }
                        REPORT_ASSIGNED_USE_NO_USER -> {
                            exportAssignedStatusReport(context, dstFolder, TITLE_ASSIGNED_USE_NO_USER, false)
                        }
                        else -> {

                        }
                    }
                }
    }

    private fun exportAuditTrailReport(context: Context, dstFolder: String, title: String) {
        val dstDir = dstFolder + AUDIT_TRAIL_FOLDER
        try {
            val dir = File(dstDir)
            if (!dir.exists()) dir.mkdirs()
            if (!dir.exists()) {
                throw Exception("Failed to create audit trail export folder, ${dstDir}.")
            }

            val file = File(dir, AUDIT_TRAIL_FILE)
            if(file.exists())   file.delete()
        } catch (e: Exception) {
            throw Exception("Error happened to create audit trail export folder, msg: ${e.message}")
        }

        try {
            val file = File(File(dstDir), AUDIT_TRAIL_FILE)
            file.setExecutable(true)
            file.setReadable(true)
            file.setWritable(true)

            val f = FileOutputStream(file)
            val pw = PrintWriter(f)
            var strFileLine = ""

            // Add Report Title
            strFileLine = title

            strFileLine += "\r\n"
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add report header line
            strFileLine = getPrintString("Lock Name", DEF_LOCK_NAME_SPACE)
            strFileLine += getPrintString("Functionality", DEF_LOCK_FUNC_SPACE)
            strFileLine += getPrintString("User Name", DEF_USER_NAME_SPACE)
            strFileLine += getPrintString("Action", DEF_ACTION_SPACE)
            strFileLine += getPrintString("Date", DEF_DATE_SPACE)
            strFileLine += getPrintString("Cred. Type", DEF_CRED_TYPE_SPACE)
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add audit trails
            auditTrails.forEach {audittrail ->
                strFileLine = getPrintString(audittrail.lockName, DEF_LOCK_NAME_SPACE)
                strFileLine += getPrintString(audittrail.lockFunc, DEF_LOCK_FUNC_SPACE)
                strFileLine += getPrintString(audittrail.userName, DEF_USER_NAME_SPACE)
                strFileLine += getPrintString(audittrail.action, DEF_ACTION_SPACE)
                strFileLine += getPrintString(audittrail.dateTime, DEF_DATE_SPACE)
                strFileLine += getPrintString(audittrail.credType, DEF_CRED_TYPE_SPACE)

                strFileLine += "\r\n"
                pw.println(strFileLine)
            }

            strFileLine = "\r\n"
            pw.println(strFileLine)


            pw.flush()
            pw.close()
            f.close()

            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun exportSharedStatusReport(context: Context, dstFolder: String, title: String, locked: Boolean) {
        val dstDir = dstFolder + SHARED_USE_FOLDER
        var fileName = ""
        if(locked)  fileName = SHARED_USE_LOCKED_FILE
        else fileName = SHARED_USE_UNLOCKED_FILE

        try {
            val dir = File(dstDir)
            if (!dir.exists()) dir.mkdirs()
            if (!dir.exists()) {
                throw Exception("Failed to create shared status export folder, ${dstDir}.")
            }

            val file = File(dir, fileName)
            if(file.exists())   file.delete()
        } catch (e: Exception) {
            throw Exception("Error happened to create shared status export folder, msg: ${e.message}")
        }

        try {
            val  file = File(File(dstDir), fileName)
            file.setExecutable(true)
            file.setReadable(true)
            file.setWritable(true)

            val f = FileOutputStream(file)
            val pw = PrintWriter(f)
            var strFileLine = ""

            // Add Report Title
            strFileLine = title
            strFileLine += "    Count: ${filterLockUsageInfos.size}"

            strFileLine += "\r\n"
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add report header line
            strFileLine = getPrintString("Lock Name", DEF_LOCK_NAME_SPACE)
            strFileLine += getPrintString("Functionality", DEF_LOCK_FUNC_SPACE)
            if(locked) {
                strFileLine += getPrintString("User Name", DEF_USER_NAME_SPACE)
                strFileLine += getPrintString("Date Time", DEF_DATE_SPACE)
                strFileLine += getPrintString("Cred. Type", DEF_CRED_TYPE_SPACE)
            }
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add shared status lock
            filterLockUsageInfos.forEach { report ->
                strFileLine = getPrintString(report.lockName, DEF_LOCK_NAME_SPACE)
                strFileLine += getPrintString(report.lockFunc, DEF_LOCK_FUNC_SPACE)
                if(locked) {
                    strFileLine += getPrintString(report.userName, DEF_USER_NAME_SPACE)
                    strFileLine += getPrintString(report.dateTime, DEF_DATE_SPACE)
                    strFileLine += getPrintString(report.credType, DEF_CRED_TYPE_SPACE)
                }

                strFileLine += "\r\n"
                pw.println(strFileLine)
            }

            strFileLine = "\r\n"
            pw.println(strFileLine)


            pw.flush()
            pw.close()
            f.close()

            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun exportAssignedStatusReport(context: Context, dstFolder: String, title: String, hasUser: Boolean) {
        val dstDir = dstFolder + ASSIGNED_USE_FOLDER
        var fileName = ""
        if(hasUser)  fileName = ASSIGNED_USE_HAS_USER_FILE
        else fileName = ASSIGNED_USE_NO_USER_FILE

        try {
            val dir = File(dstDir)
            if (!dir.exists()) dir.mkdirs()
            if (!dir.exists()) {
                throw Exception("Failed to create shared status export folder, ${dstDir}.")
            }

            val file = File(dir, fileName)
            if(file.exists())   file.delete()
        } catch (e: Exception) {
            throw Exception("Error happened to create shared status export folder, msg: ${e.message}")
        }

        try {
            val  file = File(File(dstDir), fileName)
            file.setExecutable(true)
            file.setReadable(true)
            file.setWritable(true)

            val f = FileOutputStream(file)
            val pw = PrintWriter(f)
            var strFileLine = ""

            // Add Report Title
            strFileLine = title
            strFileLine += "    Lock Count: ${assignedStatusLocks.size}"

            strFileLine += "\r\n"
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add report header line
            strFileLine = getPrintString("Lock Name", DEF_LOCK_NAME_SPACE)
            strFileLine += getPrintString("Functionality", DEF_LOCK_FUNC_SPACE)
            if(hasUser) strFileLine += getPrintString("Assigned User", DEF_ASSIGNED_USER_SPACE)
            strFileLine += "\r\n"
            pw.println(strFileLine)

            // Add assigned status lock
            assignedStatusLocks.forEach {lock ->
                if(hasUser) {
                    val lockUsers = getLockAssignUsers(lock.lockName)

                    lockUsers.forEach {
                        strFileLine = getPrintString(lock.lockName, DEF_LOCK_NAME_SPACE)
                        strFileLine += getPrintString(lock.lockFunc, DEF_LOCK_FUNC_SPACE)
                        strFileLine += getPrintString(it!!, DEF_ASSIGNED_USER_SPACE)

                        strFileLine += "\r\n"
                        pw.println(strFileLine)
                    }
                } else {
                    strFileLine = getPrintString(lock.lockName, DEF_LOCK_NAME_SPACE)
                    strFileLine += getPrintString(lock.lockFunc, DEF_LOCK_FUNC_SPACE)

                    strFileLine += "\r\n"
                    pw.println(strFileLine)
                }
            }

            strFileLine = "\r\n"
            pw.println(strFileLine)


            pw.flush()
            pw.close()
            f.close()

            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun sortByLockName(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.lockName }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.lockName }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }
    }

    override fun sortByLockFunc(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.lockFunc }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.lockFunc }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }

    }

    override fun sortByUserName(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.userName }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.userName }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }

    }

    override fun sortByLockAction(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.action }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.action }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }

    }

    override fun sortByAuditDT(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.dateTime }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.dateTime }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }

    }

    override fun sortByCredType(sortMode: Byte) {
        if(sortMode == AUDIT_SORT_ASC) {
            val sortedAuditTrails = auditTrails.sortedWith(compareBy({ it.credType }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        } else {
            val sortedAuditTrails = auditTrails.sortedWith(compareByDescending ({ it.credType }))
            filterAuditTrails = ArrayList(sortedAuditTrails)
        }
    }

    private fun getPrintString(strInput: String, length: Int): String {
    var strOutPut = ""
    val leftPadding: Int
    val rightPadding: Int

    if (strInput.length < length) {
        leftPadding = (length - strInput.length) / 2
        rightPadding = length - strInput.length - leftPadding

        for (i in 0 until leftPadding) {
            strOutPut += " "
        }

        strOutPut += strInput

        for (i in 0 until rightPadding) {
            strOutPut += " "
        }
    } else if (strInput.length > length) {
        strOutPut = strInput.substring(0, length)
    } else {
        strOutPut = strInput
    }

    return strOutPut
}


override fun downloadDB(context: Context): Observable<Boolean> {
        val strDstDir = Environment.getExternalStorageDirectory().getPath()

        return Observable.just(true)
                .doOnNext {
                    try {
                        val dir = File(strDstDir + DOWNLOAD_DIR_NAME)
                        if (!dir.exists()) dir.mkdirs()
                        if (!dir.exists()) {
                            throw Exception("Failed to create download folder.")
                        }
                    } catch (e: Exception) {
                        throw Exception("Error happened to create download folder: ${e.message}")
                    }

                    try {
                        copyDirectory(context, File(context.getFilesDir().getParent() + "/databases"), File(strDstDir + DOWNLOAD_DIR_NAME))
                    } catch (e: Exception) {
                        throw Exception("Error happened to save download files: ${e.message}")
                    }
                }
    }

    override fun restoreDB(context: Context): Observable<Boolean> {
        val strSrcDir = Environment.getExternalStorageDirectory().getPath()

        return Observable.just(true)
                .doOnNext {
                    try {
                        val dir = File(strSrcDir + DOWNLOAD_DIR_NAME)
                        if (!dir.exists()) {
                            throw Exception("Source folder did not exists.")
                        }
                    } catch (e: Exception) {
                        throw Exception("Source folder did not exists: ${e.message}")
                    }

                    try {
                        copyDirectory(context, File(strSrcDir + DOWNLOAD_DIR_NAME), File(context.getFilesDir().getParent() + "/databases"))
                    } catch (e: Exception) {
                        throw Exception("Error happened to copy restore files: ${e.message}")
                    }
                }
    }

    @Throws(IOException::class)
    fun copyDirectory(context: Context, sourceDir: File, destDir: File) {
        // creates the destination directory if it does not exist
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        // throws exception if the source does not exist
        if (!sourceDir.exists()) {
            throw IllegalArgumentException("sourceDir does not exist")
        }

        // throws exception if the arguments are not directories
        if (sourceDir.isFile || destDir.isFile) {
            throw IllegalArgumentException(
                    "Either sourceDir or destDir is not a directory")
        }

        copyDirectoryImpl(context, sourceDir, destDir)
    }

    @Throws(IOException::class)
    private fun copyDirectoryImpl(context: Context, sourceDir: File, destDir: File) {
        val items = sourceDir.listFiles()
        if (items != null && items.size > 0) {
            for (anItem in items) {
                if (anItem.isDirectory) {
                    // create the directory in the destination
/*
                    val newDir = File(destDir, anItem.name)
                    println("CREATED DIR: " + newDir.absolutePath)
                    newDir.mkdir()

                    // copy the directory (recursive call)
                    copyDirectory(anItem, newDir)
*/
                } else {
                    // copy the file
                    val destFile = File(destDir, anItem.name)
                    copySingleFile(anItem, destFile)

                    MediaScannerConnection.scanFile(context, arrayOf(destFile.toString()), null, null)

                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copySingleFile(sourceFile: File, destFile: File) {
        println("COPY FILE: " + sourceFile.absolutePath
                + " TO: " + destFile.absolutePath)
        if (!destFile.exists()) {
            destFile.createNewFile()
        }

        var sourceChannel: FileChannel? = null
        var destChannel: FileChannel? = null

        try {
            sourceChannel = FileInputStream(sourceFile).getChannel()
            destChannel = FileOutputStream(destFile).channel
            sourceChannel!!.transferTo(0, sourceChannel!!.size(), destChannel)
        } finally {
            if (sourceChannel != null) {
                sourceChannel!!.close()
            }
            if (destChannel != null) {
                destChannel!!.close()
            }
        }
    }

    override fun getEmptyAuditPacket(): CommPacket {
        val packet = CommPacket()

        packet.executeBytes.cmd = CMD_CLEAR_AUDIT_BT

        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun getDoAuditPacketByName(lockName: String): CommPacket {
        val packet = CommPacket()

        packet.executeBytes.cmd = CMD_LOCK_DO_AUDIT_BT

        packet.executeBytes.para[0] = PT_LOCK_SN
        packet.executeBytes.para[1] = PT_LENGTH_LOCK_SN

        if(lockNameSNMap.containsKey(lockName)) {
            val lockSNArray = lockNameSNMap[lockName]!!.toByteArray()
            for (index in 0 until PT_LENGTH_LOCK_SN) {
                packet.executeBytes.para[2 + index] = lockSNArray[index]
            }
        }

        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun getDoAuditPacketByFuncType(lockFuncType: Byte): CommPacket {
        val packet = CommPacket()

        when(lockFuncType) {
            AUDIT_LOCK_FUNC_FILTER_ALL -> {
                packet.executeBytes.cmd = CMD_ALL_DO_AUDIT_BT
            }
            AUDIT_LOCK_FUNC_FILTER_SHARED -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_SHARED_BT
            }
            AUDIT_LOCK_FUNC_FILTER_ASSIGNED -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_ASSIGNED_BT
            }
            AUDIT_LOCK_FUNC_FILTER_LOCKED -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_LOCKED_BT
            }
            AUDIT_LOCK_FUNC_FILTER_UNLOCKED -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_UNLOCKED_BT
            }
            AUDIT_LOCK_FUNC_FILTER_RFID -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_RFID_BT
            }
            AUDIT_LOCK_FUNC_FILTER_KEYPAD -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_PINCODE_BT
            }
            AUDIT_LOCK_FUNC_FILTER_MOBILEID -> {
                packet.executeBytes.cmd = CMD_DO_AUDIT_MOBILEID_BT
            }
        }

        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun getDoLockUsageInfoPacket(): CommPacket {
        val packet = CommPacket()


        packet.executeBytes.cmd = CMD_ALL_LOCK_USAGE_INFO_BT

        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun getLockUsageInfoSize(): Int {
        return lockUsageInfos.size
    }

    override fun getFilterLockUsageInfoSize(): Int {
        return filterLockUsageInfos.size
    }

    override fun getFilterLockUsageInfos(): List<Report> {
        return filterLockUsageInfos
    }

    override fun getLockStatusPacket(lockName: String): CommPacket {
        val packet = CommPacket()


        if(lockName == ALL_LOCKS) {
            packet.executeBytes.cmd = CMD_GET_ALL_LOCKS_STATUS_BT
        } else {
            packet.executeBytes.cmd = CMD_GET_LOCK_STATUS_BT

            var offset = 0
            packet.executeBytes.para[offset++] = PT_LOCK_SN
            packet.executeBytes.para[offset++] = PT_LENGTH_LOCK_SN

            if(lockNameSNMap.containsKey(lockName)) {
                val lockSNArray = lockNameSNMap[lockName]!!.toByteArray()
                for (index in 0 until PT_LENGTH_LOCK_SN) {
                    packet.executeBytes.para[offset++] = lockSNArray[index]
                }
            }
        }


        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun getSyncControllerPacketList(reassign: Boolean): ArrayList<CommPacket> {
        val packetList = ArrayList<CommPacket>()

        // Remove all lock credential assignments
        if(reassign) {
            val packet = CommPacket()

            packet.executeBytes.cmd = CMD_DEASSIGN_ALL_LOCK_CREDENTIAL_BT

            packet.executeBytes.para[0] = PT_LOCK_SN
            packet.executeBytes.para[1] = PT_LENGTH_LOCK_SN
            for (index in 0 until PT_LENGTH_LOCK_SN) {
                packet.executeBytes.para[2 + index] = 0x00
            }

            packet.fromExecuteBytesToBuffer()
            packet.packetBuffer[packet.packetSize - 1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize - 2)

            packetList.add(packet)
        }

        // Update locks
        locks.forEach {
            val packet = CommPacket()

            val lockSNArray = it.lockSN.toByteArray()

            var offset = 0
            packet.executeBytes.cmd = CMD_ADD_LOCK_BT

            packet.executeBytes.para[offset++] = PT_LOCK_SN
            packet.executeBytes.para[offset++] = PT_LENGTH_LOCK_SN
            for (index in 0 until PT_LENGTH_LOCK_SN) {
                packet.executeBytes.para[offset++] = lockSNArray[index]
            }

            offset++    //PT_LOCK_FUNC
            offset++    // 1
            packet.executeBytes.para[offset++] = lockFuncs.indexOf(it.lockFunc).toByte()

            packet.fromExecuteBytesToBuffer()
            packet.packetBuffer[packet.packetSize - 1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize - 2)

            packetList.add(packet)
        }

        // Insert lock credential assignments from database
        val lockNameUserIDListMap = HashMap<String, ArrayList<Int>>()
        userLockAssigns.forEach {userLockAssign ->
            var userIDList = ArrayList<Int>()
            if(lockIDNameMap.containsKey(userLockAssign.lockId)) {
                val lockName = lockIDNameMap[userLockAssign.lockId]
                if (lockName != null) {
                    if (lockNameUserIDListMap.containsKey(lockName)) userIDList = lockNameUserIDListMap[lockName]!!
                    userIDList.add(userLockAssign.userId)
                    userIDList = ArrayList(userIDList.distinct())
                    lockNameUserIDListMap[lockName!!] = userIDList
                }
            }
        }


        lockNameUserIDListMap.forEach {map ->
            if(map.value.size > 0) {
                packetList.addAll(getSyncPacketListByLockName(map.key, map.value))
            }

        }

        return packetList
    }

    private fun getSyncPacketListByLockName(lockName: String, userIDList: ArrayList<Int>): ArrayList<CommPacket> {
        val packetList = ArrayList<CommPacket>()

        val lockId = lockNameIDMap[lockName]
        val lockSN = lockNameSNMap[lockName]
        val lockPermit = lockIDPermitMap[lockId]
        val lockFunc = lockIDFuncMap[lockId]
        val lockSNArray = lockSN!!.toByteArray()


        userIDList.forEach {userID ->
            userCredAssigns.forEach { userCredAssign ->
                if (userCredAssign.isActive) {
                    if (userCredAssign.userId == userID) {
                        if(credTypeMap.containsKey(userCredAssign.credSN)) {
                            val credType = credTypeMap[userCredAssign.credSN]

                            if ((credType!! and lockPermit!!) == credType) {
                                val packet = CommPacket()
                                var offset = 0

                                // LOCK ID
                                packet.executeBytes.cmd = CMD_INSERT_LOCK_CREDENTIAL_ASSIGN_BT

                                packet.executeBytes.para[offset++] = PT_LOCK_SN
                                packet.executeBytes.para[offset++] = PT_LENGTH_LOCK_SN
                                for (index in 0 until PT_LENGTH_LOCK_SN) {
                                    packet.executeBytes.para[offset++] = lockSNArray[index]
                                }

                                // LOCK FUNCTION
                                packet.executeBytes.para[offset++] = PT_LOCK_FUNC
                                packet.executeBytes.para[offset++] = 1
                                packet.executeBytes.para[offset++] = lockFuncs.indexOf(lockFunc).toByte()

                                // Credential ID
                                val credSNArray = getCredentailSNArray(userCredAssign.credSN)
                                packet.executeBytes.para[offset++] = PT_CREDENTIAL_SN
                                packet.executeBytes.para[offset++] = credSNArray.size.toByte()
                                for (index in 0 until credSNArray.size) {
                                    packet.executeBytes.para[offset++] = credSNArray[index]
                                }

                                // Credential Type
                                packet.executeBytes.para[offset++] = PT_CREDENTIAL_SN
                                packet.executeBytes.para[offset++] = 1
                                packet.executeBytes.para[offset++] = credType!!

                                packet.fromExecuteBytesToBuffer()
                                packet.packetBuffer[packet.packetSize - 1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize - 2)

                                packetList.add(packet)
                            }


                        }
                    }
                }
            }
        }


        return packetList
    }

    override fun getLockName(lockSN: String): String {
        locks.forEach {lock ->
            if(lock.lockSN.equals(lockSN))  return lock.lockName
        }

        return UNKNOWN_LOCK
    }

    override fun getUserName(credSN: String, sAuditDT: String): String {
        val auditDT = sdtf.parse(sAuditDT)
        val auditMinutes = auditDT.getTime() / 60000

        if(credSN.equals(SHARED_CREDENTIAL))    return SHARED_USER_NAME
        if(credSN.equals(ADMIN_CREDENTIAL))    return ADMIN_USER_NAME

        userCredAssigns.forEach {userCredAssign ->
            if(userCredAssign.credSN.equals(credSN)) {
                if(userCredAssign.isActive) {
                    val startDT = sdtf.parse(userCredAssign.startDate)
                    val startMinutes = startDT.getTime() / 60000

                    if(auditMinutes > startMinutes) return userIDNameMap[userCredAssign.userId]!!
                } else {
                    val startDT = sdtf.parse(userCredAssign.startDate)
                    val startMinutes = startDT.getTime() / 60000
                    val endDT = sdtf.parse(userCredAssign.endDate)
                    val endMinutes = endDT.getTime() / 60000

                    if((auditMinutes >= startMinutes) and (auditMinutes <= endMinutes)) return userIDNameMap[userCredAssign.userId]!!
                }
            }
        }

        return "Unknown"
    }

    override fun updateLockStatus(lockSN: String, lockStatus: Byte): Observable<List<Lock>> {
        val updLocks = ArrayList<Lock>()

        return Observable.just(true)
                .map{
                    locks.forEach {lock ->
                        if(lock.lockSN.equals(lockSN)) {
                            Log.i(LOG_TAG, "Update Lock Status: lockSN: ${lockSN} LockStatus: ${lockStatus} ")
                            lock.lockState = lockStatus
                            updLocks.add(lock)
                        }
                    }

                    if(updLocks.size > 0)   lockDao.updateLocks(updLocks)

                    return@map locks
                }
    }

    override fun getLockNameBylockSN(lockSN: String): String {
        return lockSNNameMap[lockSN]!!
    }

    /*
        REPORTS layout related
     */

    override fun getFilterAuditTrailSize(): Int {
        return filterAuditTrails.size
    }

    override fun getFilterAuditTrails(): List<Report> {
        return filterAuditTrails
    }

    override fun clearAuditTrails() {
        auditTrails.clear()
        filterAuditTrails.clear()
    }

    override fun addAuditTrail(report: Report) {
        auditTrails.add(report)
        filterAuditTrails.add(report)
    }

    override fun filterAuditTrailByLockName(lockName: String) {
        filterAuditTrails.clear()

        auditTrails.forEach { auditTrail ->
            if(auditTrail.lockName.equals(lockName))    filterAuditTrails.add(auditTrail)
        }
    }

    override fun filterAuditTrails(auditFilter: Byte) {
        filterAuditTrails.clear()

        auditTrails.forEach {auditTrail ->
            when(auditFilter) {
                AUDIT_LOCK_FUNC_FILTER_SHARED -> {
                    if(auditTrail.lockFunc.equals(LOCK_FUNC_SHARED_USE))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_ASSIGNED -> {
                    if(auditTrail.lockFunc.equals(LOCK_FUNC_ASSIGNED_USE))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_LOCKED -> {
                    if(auditTrail.action.equals(ACTION_IS_LOCKED))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_UNLOCKED -> {
                    if(auditTrail.action.equals(ACTION_IS_UNLOCKED))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_RFID -> {
                    if(auditTrail.credType.equals(RFID_TAG))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_KEYPAD -> {
                    if(auditTrail.credType.equals(PINCODE_TAG))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_MOBILEID -> {
                    if(auditTrail.credType.equals(MOBILEID_TAG))    filterAuditTrails.add(auditTrail)
                }
                AUDIT_LOCK_FUNC_FILTER_ADMIN -> {
                    if(auditTrail.credType.equals(ADMIN_TAG))    filterAuditTrails.add(auditTrail)
                }
                else -> {
                    filterAuditTrails.add(auditTrail)
                }
            }
        }
    }

    override fun clearLockUsageInfos() {
        lockUsageInfos.clear()
        filterLockUsageInfos.clear()
    }

    override fun addLockUsageInfo(lockUsageInfo: Report) {
        lockUsageInfos.add(lockUsageInfo)
    }

    override fun filterLockUsageInfos(lockUsageInfoFilter: Byte) {
        filterLockUsageInfos.clear()

        lockUsageInfos.forEach { lockUsageInfo ->
            when (lockUsageInfoFilter) {
                LOCK_USAGE_INFO_FILTER_LOCKED -> {
                    if (lockUsageInfo.lockFunc.equals(LOCK_FUNC_SHARED_USE) && lockUsageInfo.action.equals(ACTION_IS_LOCKED)) filterLockUsageInfos.add(lockUsageInfo)
                }
                else -> {
                    if (lockUsageInfo.lockFunc.equals(LOCK_FUNC_SHARED_USE) && lockUsageInfo.action.equals(ACTION_IS_UNLOCKED)) filterLockUsageInfos.add(lockUsageInfo)
                }
            }
        }
    }


    override fun genAssignedStatusLocks(hasUser: Boolean) {
        assignedStatusLocks.clear()

        if(hasUser) {
            locks.forEach { lock ->
                if (lock.lockFunc.equals(LOCK_FUNC_ASSIGNED_USE) && lockAssignMap[lock.lockId]!!) assignedStatusLocks.add(lock)
            }
        } else {
            locks.forEach { lock ->
                if (lock.lockFunc.equals(LOCK_FUNC_ASSIGNED_USE) && !lockAssignMap[lock.lockId]!!) assignedStatusLocks.add(lock)
            }
        }
    }

    override fun getAssignedUseLocks(): List<Lock> {
        return assignedStatusLocks
    }

    override fun getAssignedStatusReportSize(): Int {
        return assignedStatusLocks.size
    }

    override fun getLockAssignUsers(lockName: String): Array<String?> {
        val assignedUsers = ArrayList<String>()

        userLockAssigns.forEach {
            if(it.lockId == lockNameIDMap[lockName]!!)  assignedUsers.add(userIDNameMap[it.userId]!!)
        }

        val array = arrayOfNulls<String>(assignedUsers.size)
        assignedUsers.toArray(array)

        return array
    }

    override fun savePairedController(name: String, address: String) {
        with(sysPref.edit()) {
            putString(PAIRED_CONTROLLER_NAME, name)
            putString(PAIRED_CONTROLLER_IP_ADDRESS, address)
            apply()
        }
    }

    override fun getPairedControllerIPAddress(): String = sysPref.getString(PAIRED_CONTROLLER_IP_ADDRESS, "")

    override fun getPairedControllerName(): String = sysPref.getString(PAIRED_CONTROLLER_NAME, "")

    override fun scanConnectedDevices(): Observable<List<DeviceData>> {
        val devices = ArrayList<DeviceData>()

        return Observable.just(true)
                .map {
                    try {
                        val jsonObject = JSONObject()
                        jsonObject.put(JSON_CMD_TYPE, CMD_NL_DISCOVERY)
                        val msg = jsonObject.toString()

                        val group: InetAddress = InetAddress.getByName("228.5.6.7")
                        val socket = MulticastSocket()
                        socket.joinGroup(group)

                        socket.soTimeout = CONNECT_DEVICE_WAIT_PERIOD

                        while(true) {
                            val packet = DatagramPacket(msg.toByteArray(), msg.length, group, 6789)

                            socket.send(packet)

                            val lmessage = ByteArray(4096)
                            val rcvPacket = DatagramPacket(lmessage, lmessage.size)

                            socket.receive(rcvPacket)
                            val stringData = String(lmessage, 0, rcvPacket.getLength())

                            if(stringData.length > 0) {
                                val jsonObject: JsonObject = JsonParser().parse(stringData).getAsJsonObject()
                                when(jsonObject.get(JSON_CMD_TYPE).asString) {
                                    CMD_NL_DISCOVERY -> {
                                        val ipAddr = jsonObject.get(BODY_IP_ADDRESS).asString
                                        val macAddress = jsonObject.get(BODY_MAC_ADDRESS).asString
                                        Log.i(LOG_TAG, "Found device: ${ipAddr}, ${macAddress}")

                                        devices.add(DeviceData("Connected Device", macAddress, ipAddr))

                                        val jsonObject = JSONObject()
                                        jsonObject.put(JSON_CMD_TYPE, CMD_NL_STOP_RESPONSE)
                                        val msg = jsonObject.toString()

                                        val sendPacket = DatagramPacket(msg.toByteArray(), msg.length, rcvPacket.address, 6789)
                                        socket.send(sendPacket)
                                    }
                                }
                            }
                        }
                    }
                    catch(e: Exception){
                        Log.i(LOG_TAG, "Error: ${e.message}")
                    }

                 return@map devices
                }
    }

    companion object {
        private val LOG_TAG: String = MainRepository::class.java.simpleName
    }

}