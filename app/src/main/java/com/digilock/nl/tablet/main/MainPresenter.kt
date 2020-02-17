package com.digilock.nl.tablet.main

import android.content.Context
import android.os.CountDownTimer
import android.os.Environment
import android.text.Editable
import android.util.Log
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.bluetooth.Report
import com.digilock.nl.tablet.comm.CommPacket
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.data.User
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import com.github.bwixted.arpcachereader.ArpCacheReader
import com.securitypeople.packagehold.util.scheduler.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * - MainPresenter
 */
class MainPresenter(private val view: MainContract.View,
                    private val dataSource: MainDataSource,
                    private val scheduler: BaseSchedulerProvider): MainContract.Presenter {

    init {
        view.presenter = this
    }

    private val compositeDisposable = CompositeDisposable()

    /*
        LOCKS related
    */
    private val mLockList = arrayListOf<Lock>()

    /*
        USERS related
    */
    private val mUserList = arrayListOf<User>()

    /*
        REPORTS related
     */
    private var mDoingAudit = false
    private var mReportType = REPORT_NONE

    private var mSortByLockName: Byte = AUDIT_SORT_NONE
    private var mSortByLockFunc: Byte = AUDIT_SORT_NONE
    private var mSortByUserName: Byte = AUDIT_SORT_NONE
    private var mSortByLockAction: Byte = AUDIT_SORT_NONE
    private var mSortByAuditDT: Byte = AUDIT_SORT_NONE
    private var mSortByCredType: Byte = AUDIT_SORT_NONE

    private var mAuditFilterType: Byte = AUDIT_LOCK_FUNC_FILTER_ALL

    private var mDoingLockUsageInfo = false
    private var mLockUsageInfoFilterType: Byte = LOCK_USAGE_INFO_FILTER_LOCKED
    /*
        SETTINGS related
    */
    private var lockNames = ArrayList<String>()

    /*
        Mobile Communication
     */
    private var btRxPacket = CommPacket()

    override fun start() {
        compositeDisposable.clear()

        // Get all Users from database
        val obtainCredsDisposable = obtainCreds()
        compositeDisposable.add(obtainCredsDisposable)

        // Get all Locks from database
        val obtainLocksDisposable = obtainLocks()
        compositeDisposable.add(obtainLocksDisposable)


        // Get all Users from database//
        val obtainUsersDisposable = obtainUsers()
        compositeDisposable.add(obtainUsersDisposable)


        // Get all Users from database//
        val obtainUserCredAssignsDisposable = obtainUserCredAssigns()
        compositeDisposable.add(obtainUserCredAssignsDisposable)


        // Get all User-Lock assignments from database
        val obtainUserLockAssignsDisposable = obtainUserLockAssigns()
        compositeDisposable.add(obtainUserLockAssignsDisposable)

        view.showLockNamesFilter(true)
    }

    private fun obtainCreds(): Disposable = dataSource.obtainCredentials()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(MainPresenter.LOG_TAG, "Start: read credential info success .")
                    },

                    {
                        Log.e(MainPresenter.LOG_TAG, "Read credential information failed: \n ${it.localizedMessage}")
                        view.showErrorMessage("Error: Read credential information failed: \n ${it.localizedMessage}")
                    }
            )


    private fun obtainLocks(): Disposable = dataSource.obtainLocks()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {locks ->
                        Log.i(LOG_TAG, "Start: read lock info success .")

                        view.showUsersScreen()

                        lockNames.clear()
                        locks.forEach {lock ->
                            lockNames.add(lock.lockName)
                        }

                        view.updateLockNames(lockNames)

                        // Get all Users from database//
                        val obtainUserLockAssignsDisposable = obtainUserLockAssigns()
                        compositeDisposable.add(obtainUserLockAssignsDisposable)
                    },

                    {
                        Log.e(LOG_TAG, "Read lock information failed: \n ${it.localizedMessage}")
                        view.showErrorMessage("Error: Read lock information failed: ${it.localizedMessage}")
                    }
            )

    private fun obtainUsers(): Disposable = dataSource.obtainUsers()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {users ->
                        Log.i(LOG_TAG, "Start: read user information success .")
                        view.showUsersScreen()
                    },

                    {
                        Log.e(LOG_TAG, "Read user information failed: \n ${it.localizedMessage}")
                        view.showErrorMessage("Error: Read user information failed: ${it.localizedMessage}")
                    }
            )

    private fun obtainUserLockAssigns(): Disposable = dataSource.obtainUserLockAssigns()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user lock assignment infomation success .")
                    },

                    {
                        Log.e(LOG_TAG, "Read user lock assignment information failed: \n ${it.localizedMessage}")
                        view.showErrorMessage("Error: Read user lock assignment information failed: ${it.localizedMessage}")
                    }
            )

    private fun obtainUserCredAssigns(): Disposable = dataSource.obtainUserCredAssigns()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user credential assignment infomation success .")
                    },

                    {
                        Log.e(LOG_TAG, "MainActivity: Read user credential assignment information failed: \n ${it.localizedMessage}")
                        view.showErrorMessage("MainActivity::Error: Read user credential assignment information failed: ${it.localizedMessage}")
                    }
            )


    override fun filterLocks(position: Int) {
        updateLockScreen(dataSource.filterLocks(position))
    }

    private fun updateLockScreen(locks: List<Lock>) {
        mLockList.clear()
        mLockList.addAll(locks)
        if(dataSource.getAllLockCount() == 0) {
            view.showNoLockScreen()
        } else {
            view.showFilterLocksScreen()
        }
    }

/*
    private fun updateLockScreen() {
        if(dataSource.getAllLockCount() == 0) {
            view.showNoLockScreen()
        } else {
            view.showLocksScreen()
        }
    }
*/

    override fun filterUsers(position: Int) {
        updateUserScreen(dataSource.filterUsers(position))
    }

    private fun updateUserScreen(users: List<User>) {
        mUserList.clear()
        mUserList.addAll(users)

        if(dataSource.getAllUserCount() == 0) {
            view.showNoUserScreen()
        } else {
            view.showFilterUsersScreen()
        }
    }


    override fun reportRowsCount(): Int {
        Log.i(LOG_TAG, "Adpater audit trail count: ${dataSource.getFilterAuditTrailSize()}")
        if(mReportType == REPORT_AUDIT_TRAIL)   return dataSource.getFilterAuditTrailSize()
        else if(mReportType == REPORT_SHARED_USE_LOCKED)   return dataSource.getFilterLockUsageInfoSize()
        else if(mReportType == REPORT_SHARED_USE_UNLOCKED)   return dataSource.getFilterLockUsageInfoSize()
        else if(mReportType == REPORT_ASSIGNED_USE_HAS_USER)   return dataSource.getAssignedStatusReportSize()
        else if(mReportType == REPORT_ASSIGNED_USE_NO_USER)   return dataSource.getAssignedStatusReportSize()
        else return 0
    }

    override fun onBindReportRowAtPosition(context: Context, position: Int, rowView: ReportRowView) {
        if(mReportType == REPORT_AUDIT_TRAIL) {
            val auditTrails = dataSource.getFilterAuditTrails()
            if (position >= auditTrails.size) return;

            rowView.setIndex("${position + 1}")
            rowView.setLockName(auditTrails[position].lockName)
            rowView.setLockFunc(auditTrails[position].lockFunc)
            rowView.setUserName(auditTrails[position].userName)
            rowView.setAction(auditTrails[position].action)
            rowView.setDateTime(auditTrails[position].dateTime)
            rowView.setCredType(auditTrails[position].credType)

            rowView.showReportDataItems(mReportType.toInt())
        } else if(mReportType == REPORT_SHARED_USE_LOCKED) {
            val reports = dataSource.getFilterLockUsageInfos()
            if (position >= reports.size) return;

            rowView.setIndex("${position + 1}")
            rowView.setLockName(reports[position].lockName)
            rowView.setLockFunc(reports[position].lockFunc)
            rowView.setUserName(reports[position].userName)
            rowView.setDateTime(reports[position].dateTime)
            rowView.setCredType(reports[position].credType)

            rowView.showReportDataItems(mReportType.toInt())

            view.updateSharedStatusReportCount(true, reports.size)
        } else if(mReportType == REPORT_SHARED_USE_UNLOCKED) {
            val reports = dataSource.getFilterLockUsageInfos()
            if (position >= reports.size) return;

            rowView.setIndex("${position + 1}")
            rowView.setLockName(reports[position].lockName)
            rowView.setLockFunc(reports[position].lockFunc)

            rowView.showReportDataItems(mReportType.toInt())

            view.updateSharedStatusReportCount(false, reports.size)

        } else if(mReportType == REPORT_ASSIGNED_USE_HAS_USER) {
            val assignedUseLocks = dataSource.getAssignedUseLocks()
            if (position >= assignedUseLocks.size) return;

            rowView.setIndex("${position + 1}")
            rowView.setLockName(assignedUseLocks[position].lockName)
            rowView.setLockFunc(assignedUseLocks[position].lockFunc)
            rowView.setLockUsers(context, dataSource.getLockAssignUsers(assignedUseLocks[position].lockName))


            rowView.showReportDataItems(mReportType.toInt())
        } else if(mReportType == REPORT_ASSIGNED_USE_NO_USER) {
            val getLockAssignUsers = dataSource.getAssignedUseLocks()
            if (position >= getLockAssignUsers.size) return;

            rowView.setIndex("${position + 1}")
            rowView.setLockName(getLockAssignUsers[position].lockName)
            rowView.setLockFunc(getLockAssignUsers[position].lockFunc)

            rowView.showReportDataItems(mReportType.toInt())
        }
    }

    override fun selectLockFuncType(position: Int) {
        if(position == AUDIT_LOCK_FUNC_FILTER_LOCKID.toInt()) view.showLockNamesFilter(false)
        else {
            mAuditFilterType = position.toByte()
            dataSource.filterAuditTrails(mAuditFilterType)
            view.showLockNamesFilter(true)
            view.updateReportToEnd()
        }
    }

    override fun selectLockName(position: Int) {
        if(position >= lockNames.size)  return

        dataSource.filterAuditTrailByLockName(lockNames[position])
        view.updateReportToEnd()
    }

    override fun getEchoPacket(): CommPacket {
        val packet = CommPacket()

        packet.executeBytes.cmd = CMD_ECHO_BT
        packet.fromExecuteBytesToBuffer()
        packet.packetBuffer[packet.packetSize-1] = crc8ofByteArrayRange(packet.packetBuffer, 0, packet.packetSize-2)

        return packet
    }

    override fun syncController() {
        val packetList = dataSource.getSyncControllerPacketList(true)
        if(packetList.size == 0)    {
            view.showCommMessage(FAILED_TO_CREATE_SYNC_CONTROLLER_PACKET)
            return
        }

        view.pauseUpdateTimer()
        var index = 0
        val timer = object: CountDownTimer(100000, 250) {
            override fun onTick(millisUntilFinished: Long) {
                view.sendWsPacket(packetList[index++])

                if(index >= packetList.size) {
                    view.showCommMessage(SYNC_CONTROLLER_COMPLETE)
                    view.resumeUpdateTimer()

                    cancel()
                }
            }

            override fun onFinish() {
                view.resumeUpdateTimer()
            }
        }

        timer.start()
    }

    override fun emptyAudit() {
        view.showAuditTrail(false)

        dataSource.clearAuditTrails()
        view.updateReports()

        view.sendWsPacket(dataSource.getEmptyAuditPacket())
    }

    override fun doAudit(lockFuncTypePos: Int, lockNamePos: Int) {
        view.showAuditTrail(false)

        dataSource.clearAuditTrails()
        view.updateReportToEnd()

        mAuditFilterType = AUDIT_LOCK_FUNC_FILTER_ALL

        mDoingAudit = true
        mReportType = REPORT_AUDIT_TRAIL

        view.sendWsPacket(dataSource.getDoAuditPacketByFuncType(AUDIT_LOCK_FUNC_FILTER_ALL))
    }

    /*
    override fun doAudit(lockFuncTypePos: Int, lockNamePos: Int) {
        view.showAuditTrail(false)

        dataSource.clearAuditTrails()
        view.updateAuditTrailToEnd()

        mDoingAudit = true
        mReportType = REPORT_AUDIT_TRAIL
        if(lockFuncTypePos == AUDIT_FILTER_LOCKID) {
            view.sendWsPacket(dataSource.getDoAuditPacketByName(curLockName))
        } else {
            view.sendWsPacket(dataSource.getDoAuditPacketByFuncType(lockFuncTypePos.toByte()))
        }
    }
*/

    override fun doLockUsageInfo(lockStatusPos: Byte) {
        view.showAuditTrail(false)

        dataSource.clearLockUsageInfos()

        mLockUsageInfoFilterType = lockStatusPos

        mDoingLockUsageInfo = true
        mReportType = (REPORT_SHARED_USE_LOCKED + lockStatusPos).toByte()

        view.sendWsPacket(dataSource.getDoLockUsageInfoPacket())

        if(lockStatusPos.toInt() == SHARED_STATUS_FILTER_LOCKED)
            view.showSharedStatus(false, true, 0)
        else
            view.showSharedStatus(false, false, 0)
    }


    override fun sharedStatus(lockStatusPos: Int) {
        if(dataSource.getLockUsageInfoSize() == 0 && !mDoingLockUsageInfo) {
            view.showErrorMessage(LOCK_USAGE_INFO_IS_EMPTY)
        }

        if(lockStatusPos == SHARED_STATUS_FILTER_LOCKED) {
            mReportType = REPORT_SHARED_USE_LOCKED
            dataSource.filterLockUsageInfos(SHARED_STATUS_FILTER_LOCKED.toByte())
            view.showSharedStatus(false, true, dataSource.getFilterLockUsageInfoSize())
        }
        else {
            mReportType = REPORT_SHARED_USE_UNLOCKED
            dataSource.filterLockUsageInfos(SHARED_STATUS_FILTER_UNLOCKED.toByte())
            view.showSharedStatus(false, false, dataSource.getFilterLockUsageInfoSize())
        }

        view.updateReports()
    }

    override fun assignedStatus(userAssignedPos: Int) {
        if(userAssignedPos == ASSIGNED_STATUS_FILTER_WITH_USER ) {
            mReportType = REPORT_ASSIGNED_USE_HAS_USER
            dataSource.genAssignedStatusLocks(true)
            view.showAssignedStatus(false, true, dataSource.getAssignedStatusReportSize())
        }
        else {
            mReportType = REPORT_ASSIGNED_USE_NO_USER
            dataSource.genAssignedStatusLocks(false)
            view.showAssignedStatus(false, false, dataSource.getAssignedStatusReportSize())
        }

        view.updateReports()
    }

    override fun getAllLockStatus() {
        view.sendWsPacket(dataSource.getLockStatusPacket(ALL_LOCKS))
    }


    override fun showLocksScreenClicked(position: Int) {
        updateLockScreen(dataSource.filterLocks(position))
    }

    override fun showUsersScreenClicked(position: Int) {
        updateUserScreen(dataSource.filterUsers(position))
    }

    override fun lockCount(): Int {
        Log.i(LOG_TAG, "Adpater lock count: ${dataSource.getLocks().size}")
        return mLockList.size
    }

    override fun onBindLockRowAtPosition(position: Int, rowView: LockRowView) {
        if(position >= mLockList.size)  return;

        val lock = mLockList[position]
        rowView.setLockName(lock.lockName)
        rowView.setLockState(lock.lockState)
    }

    override fun userCount(): Int {
        Log.i(LOG_TAG, "Adpater user count: ${dataSource.getUsers().size}")
        return mUserList.size
    }

    override fun onBindUserRowAtPosition(position: Int, rowView: UserRowView) {
        if(position >= mUserList.size)  return;

        val user = mUserList[position]
        rowView.setUserName("${user.fstName} ${user.lstName}")
        rowView.setUserDepartment(user.dept)
        rowView.setUserState(user.userState)
        rowView.setUserDTInfo("${user.startDate}\n${user.endDate}")
        rowView.setAssignedLocks(dataSource.getAssignedLockNames(user))
    }


    override fun showLockDetail(position: Int) {
        view.showLockDetail(mLockList[position].lockId)
    }


    override fun connectControllerTimeout() {
        view.showErrorMessage(CONNECT_CONTROLLER_TIMEOUT)
    }

    override fun toggleLockState(position: Int) {
        view.toggleLockState(mLockList[position].lockSN)
    }

    override fun getToggleLockStatePacket(lockSN: String): CommPacket {
        return dataSource.getToggleLockStatePacket(lockSN)
    }

    override fun getLockStatusPacket(lockSN: String): CommPacket {
        return dataSource.getLockStatusPacket(lockSN)
    }

    /*
        USERS related
     */

    override fun confirmChangeUserState(position: Int) {
        if(!mUserList[position].userState) {
            if(dataSource.userNameExists(mUserList[position].fstName, mUserList[position].lstName)) {
                view.showErrorMessage(ENABLED_USER_NAME_EXISTS)
                return
            }
        }

        view.confirmChangeUserState(mUserList[position].userId, mUserList[position].userState)
    }

    override fun changeUserState(userID: Int) {
        var user: User? = null

        mUserList.forEach {
            if(it.userId == userID) user = it
        }
        if(user == null)    return

        if(user!!.userState) {
            val Disposable = dataSource.disableUser(user!!)
                    .subscribeOn(scheduler.io())
                    .observeOn(scheduler.ui())
                    .subscribe(
                            {
                                Log.i(LOG_TAG, "Disable user success .")
                            },

                            {
                                Log.e(LOG_TAG, "Disable user success failed: \n ${it.localizedMessage}")
                                view.showErrorMessage("Error: Disable user success failed: ${it.localizedMessage}")
                            }
                    )
            compositeDisposable.add(Disposable)
        } else {
            val Disposable = dataSource.enableUser(user!!)
                    .subscribeOn(scheduler.io())
                    .observeOn(scheduler.ui())
                    .subscribe(
                            {
                                Log.i(LOG_TAG, "Enable user success .")
                                view.showFilterUsersScreen(USER_FILTER_ACTIVE)
                            },

                            {
                                Log.e(LOG_TAG, "Enable user failed: \n ${it.localizedMessage}")
                                view.showErrorMessage("Error: Enable user failed: ${it.localizedMessage}")
                            }
                    )
            compositeDisposable.add(Disposable)
        }
    }

    override fun showUserDetail(position: Int) {
        if(!mUserList[position].userState) {
            view.showErrorMessage(USER_IS_DISABLED_TAG)
            return
        }

        view.showUserDetail(mUserList[position].userId)
    }

    override fun updateUserCredPermit(userID: Int) {
        val Disposable = dataSource.updateUserCredPermit(userID)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Delete lock infomation success .")
                        },

                        {
                            Log.e(LOG_TAG, "Delete lock information failed: \n ${it.localizedMessage}")
                            view.showErrorMessage("Error: Delete lock information failed: ${it.localizedMessage}")
                        }
                )
        compositeDisposable.add(Disposable)
    }

    override fun downloadDB(context: Context) {
        val Disposable = dataSource.downloadDB(context)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Backup database success .")
                            view.showErrorMessage("Backup database success.")
                        },

                        {
                            Log.e(LOG_TAG, "Backup database failed: \n ${it.localizedMessage}")
                            view.showErrorMessage("Error: Backup database failed: ${it.localizedMessage}")
                        }
                )
        compositeDisposable.add(Disposable)
    }

    override fun restoreDB(context: Context) {
        val Disposable = dataSource.restoreDB(context)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Restore database success .")
                            view.showErrorMessage("Restore database success.")
                            view.reStartApp()
                        },

                        {
                            Log.e(LOG_TAG, "Restore database failed: \n ${it.localizedMessage}")
                            view.showErrorMessage("Error: Restore database failed: ${it.localizedMessage}")
                        }
                )
        compositeDisposable.add(Disposable)
    }

    override fun connectBleController(context: Context) {
        val address = dataSource.getPairedControllerIPAddress()

        if(address.isNullOrEmpty()) {
            view.showErrorMessage(context.getString(R.string.no_paired_controller))
        } else {
            view.connectController(address)
        }
    }

    override fun showSettings() {
        view.showPairedControllerName(dataSource.getPairedControllerName())
    }

    override fun scanController(context: Context) {
        dataSource.savePairedController("", "")      // Clear paired websocket controller IP address
        view.startScanController()

        val Disposable = dataSource.findConnectedDevices()
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            val arpCacheReader = ArpCacheReader()
                            if(arpCacheReader.init(context)) {
                                Log.i(LOG_TAG, "ARP init completed.")

                                val devices = ArrayList<DeviceData>()
                                val networkNodeMap = arpCacheReader.networkNodes
                                for(entry in networkNodeMap.entries) {
                                    val networkNode = entry.value

                                    val device = networkNode.device
                                    val mac = networkNode.mac
                                    val ip = networkNode.ip

                                    if(!mac .equals("00:00:00:00:00:00")) {
                                        devices.add(DeviceData("Unknown", mac, ip))
                                    }
                                }

                                view.stopScanController()
                                view.updateControllerDevicesView(devices)
                            }
                        },
                        {
                            Log.i(LOG_TAG, "ARP init completed.")
                        }
                )

        compositeDisposable.add(Disposable)

    }

    override fun setControllerIP(context: Context, ipPart1: Editable, ipPart2: Editable, ipPart3: Editable, ipPart4: Editable) {
        if(ipPart1.isNullOrEmpty()) {
            view.showErrorMessage(context.getString(R.string.ip_field_empty))
            return
        }

        if(ipPart2.isNullOrEmpty()) {
            view.showErrorMessage(context.getString(R.string.ip_field_empty))
            return
        }

        if(ipPart3.isNullOrEmpty()) {
            view.showErrorMessage(context.getString(R.string.ip_field_empty))
            return
        }

        if(ipPart4.isNullOrEmpty()) {
            view.showErrorMessage(context.getString(R.string.ip_field_empty))
            return
        }

        if(ipPart1.toString().toInt() > 255) {
            view.showErrorMessage(context.getString(R.string.ip_field_too_large))
            return
        }

        if(ipPart2.toString().toInt() > 255) {
            view.showErrorMessage(context.getString(R.string.ip_field_too_large))
            return
        }

        if(ipPart3.toString().toInt() > 255) {
            view.showErrorMessage(context.getString(R.string.ip_field_too_large))
            return
        }

        if(ipPart4.toString().toInt() > 255) {
            view.showErrorMessage(context.getString(R.string.ip_field_too_large))
            return
        }

        val ipAddress = "${ipPart1.toString()}.${ipPart2.toString()}.${ipPart3.toString()}.${ipPart4.toString()}"
        dataSource.savePairedController("", ipAddress)

        view.connectController(ipAddress)
    }

    override fun savePairedController(name: String, address: String) {
        dataSource.savePairedController(name, address)
    }

    override fun processMobilePacket(buf: ByteArray) {
        btRxPacket.packetBuffer = buf.copyOfRange(0, WS_PACKET_SIZE)
        btRxPacket.fromBufferToExecuteBytes()
        if(btRxPacket.executeBytes.crcValue != crc8ofByteArrayRange(btRxPacket.packetBuffer, 0, btRxPacket.packetSize-2)) {
            view.showCommMessage(PACKET_CRC_CHECK_FAILED)
        } else {
            if(btRxPacket.executeBytes.header != PKT_HEADER) {
                view.showCommMessage(PACKET_MISS_HEADER)
            } else {
                if(btRxPacket.executeBytes.tail != PKT_TAIL) {
                    view.showCommMessage(PACKET_MISS_TAIL)
                } else {
                    when(btRxPacket.executeBytes.cmd) {
                        CMD_AUDIT_TRAILS_BT -> {
                            decodeAuditTrail(btRxPacket)
                            view.updateReportToEnd()
                        }
                        CMD_AUDIT_TRAIL_DONE_BT -> {
                            view.showCommMessage(AUDIT_TRAIL_DONE)
                            mDoingAudit = false
                        }
                        CMD_AUDIT_TRAIL_EMPTY_BT -> {
                            view.showCommMessage(AUDIT_TRAIL_EMPTY)
                            mDoingAudit = false
                        }
                        CMD_CLEAR_AUDIT_BT -> {
                            view.showCommMessage(REMOVE_AUDIT_TRAILS_DONE)
                        }
                        CMD_LOCK_USAGE_INFO_BT -> {
                            decodeLockUsageInfo(btRxPacket)
                            view.updateReportToEnd()
                        }
                        CMD_LOCK_USAGE_INFO_EMPTY_BT -> {
                            view.showCommMessage(LOCK_USAGE_INFO_EMPTY)
                            mDoingLockUsageInfo = false
                        }
                        CMD_LOCK_USAGE_INFO_DONE_BT -> {
                            view.showCommMessage(LOCK_USAGE_INFO_DONE)
                            mDoingLockUsageInfo = false
                        }
                        CMD_LOCK_STATUS_BT -> {
                            decodeLockStatus(btRxPacket)
                        }
                        CMD_LOCK_STATUS_DONE_BT -> {
                            view.showCommMessage(LOCK_STATUS_DONE)
                        }
                        CMD_AUTO_LOCK_STATUS_BT -> {
                            decodeAutoLockStatus(btRxPacket)
                        }
                        CMD_TOGGLE_LOCK_STATUS_BT -> {
                            if(btRxPacket.executeBytes.result == BT_RESULT_SERVER_STATE_ERROR) {
                                view.showCommMessage(DOGGLE_LOCK_STATUS_REJECTED)
                            } else {
                                val lockSN = String(byteArrayOf(btRxPacket.executeBytes.para[2], btRxPacket.executeBytes.para[3],btRxPacket.executeBytes.para[4],btRxPacket.executeBytes.para[5] ))
                                view.sendWsPacket(dataSource.getLockStatusPacket(dataSource.getLockNameBylockSN(lockSN)))
                            }
                        }
                        else -> {
                            view.showCommMessage("Mobile Command: Unsupported command-${btRxPacket.executeBytes.cmd}")
                        }
                    }
                }
            }
        }
    }

    private fun decodeLockStatus(packet: CommPacket) {
        val lockSN = String(byteArrayOf(packet.executeBytes.para[2], packet.executeBytes.para[3],packet.executeBytes.para[4],packet.executeBytes.para[5] ))
        val lockStatus = packet.executeBytes.para[8]

        Log.i(LOG_TAG, "Update lock: ${lockSN} status: ${lockStatus}")

        val disposable = dataSource.updateLockStatus(lockSN, lockStatus)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {locks ->
                            view.showLocksScreen()

                            Log.i(LOG_TAG, "Update lock status success .")
                        },

                        {
                            Log.e(LOG_TAG, "Update lock status failed:  ${it.localizedMessage}")
                            view.showErrorMessage("Error: Update lock status failed:  ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(disposable)
    }

    private fun decodeAutoLockStatus(packet: CommPacket) {
        var offset = 0
        offset++
        offset++
        val lockSN = String(byteArrayOf(packet.executeBytes.para[offset++], packet.executeBytes.para[offset++],packet.executeBytes.para[offset++],packet.executeBytes.para[offset++] ))

        offset++
        offset++
        val lockStatus = packet.executeBytes.para[offset++]

        Log.i(LOG_TAG, "Update lock: ${lockSN} status: ${lockStatus}")

        val disposable = dataSource.updateLockStatus(lockSN, lockStatus)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {locks ->
                            view.showLocksScreen()

                            val btTxPacket = CommPacket()
                            btTxPacket.executeBytes.cmd = CMD_RESP_AUTO_LOCK_STATUS_BT

                            for(index in 0..5) {
                                btTxPacket.executeBytes.para[index] = packet.executeBytes.para[index]
                            }
                            btTxPacket.fromExecuteBytesToBuffer()
                            btTxPacket.packetBuffer[btTxPacket.packetSize-1] = crc8ofByteArrayRange(btTxPacket.packetBuffer, 0, btTxPacket.packetSize-2)
                            view.sendWsPacket(btTxPacket)

                            Log.i(LOG_TAG, "Update lock status success .")
                        },

                        {
                            Log.e(LOG_TAG, "Update lock status failed:  ${it.localizedMessage}")
                            view.showErrorMessage("Error: Update lock status failed:  ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(disposable)
    }

    private fun decodeAuditTrail(packet: CommPacket) {
        var offset = 0
        offset++        // Parameter type: Lock serial number
        offset++        // length of lock serial number: fix 4 bytes
        val lockSNBuf = ByteArray(4)
        for(index in 0 until 4) {
            lockSNBuf[index] = packet.executeBytes.para[offset++]
        }

        offset++        // Parameter type: Credential SN
        val credSNBuf = ByteArray(packet.executeBytes.para[offset++].toInt())
        for(index in 0 until packet.executeBytes.para[7]) {
            credSNBuf[index] = packet.executeBytes.para[offset++]
        }

        offset++        // Parameter type: Lock action
        offset++        // length: fixed 1 byte
        val action = packet.executeBytes.para[offset++]

        offset++        // Parameter type: date information
        val dtLen = packet.executeBytes.para[offset++]
        val dtBuf = ByteArray(dtLen.toInt())
        for(index in 0 until dtLen) {
            dtBuf[index] = packet.executeBytes.para[offset++]
        }

        offset++        // Parameter type: lock function
        offset++;       // length: fixed 1 byte
        val lockFunc = packet.executeBytes.para[offset++]

        offset++        // Parameter type: Credential type
        offset++;       // length: fixed 1 byte
        val credType = packet.executeBytes.para[offset++]

        val sDateTime = String(dtBuf)
        val lockName = dataSource.getLockName(String(lockSNBuf))
        var userName = UNKNOWN_USER_NAME

        userName = dataSource.getUserName(makeCredSN(credSNBuf), sDateTime)

        var credTypeTag = UNKNOWN_TAG
        if(credType == RFID_TYPE_TAG)   credTypeTag = RFID_TAG
        else if(credType == MOBILEID_TYPE_TAG)   credTypeTag = MOBILEID_TAG
        else if(credType == PINCODE_TYPE_TAG)   credTypeTag = PINCODE_TAG
        else if(credType == ADMIN_TYPE_TAG)   credTypeTag = ADMIN_TAG

        val sAction = lockActionMaps()[action]

        dataSource.addAuditTrail(Report(lockName, lockFuncs[lockFunc.toInt()], userName, sAction!!, sDateTime, credTypeTag))
    }

    private fun decodeLockUsageInfo(packet: CommPacket) {
        var offset = 0
        offset++        // Parameter type: Lock serial number
        offset++        // length of lock serial number: fix 4 bytes
        val lockSNBuf = ByteArray(4)
        for(index in 0 until 4) {
            lockSNBuf[index] = packet.executeBytes.para[offset++]
        }

        var test = 0
        if(lockSNBuf[3] == 0x35.toByte()) {
            test = offset
            val gg = test
        }

        offset++        // Parameter type: Credential SN
        val credSNBuf = ByteArray(packet.executeBytes.para[offset++].toInt())
        for(index in 0 until packet.executeBytes.para[7]) {
            credSNBuf[index] = packet.executeBytes.para[offset++]
        }

        offset++        // Parameter type: Lock action
        offset++        // length: fixed 1 byte
        val action = packet.executeBytes.para[offset++]

        offset++        // Parameter type: date information
        val dtLen = packet.executeBytes.para[offset++]
        val dtBuf = ByteArray(dtLen.toInt())
        for(index in 0 until dtLen) {
            dtBuf[index] = packet.executeBytes.para[offset++]
        }

        offset++        // Parameter type: Credential type
        offset++;       // length: fixed 1 byte
        val credType = packet.executeBytes.para[offset++]

        val sDateTime = String(dtBuf)
        val lockName = dataSource.getLockName(String(lockSNBuf))
        var userName = UNKNOWN_USER_NAME

        userName = dataSource.getUserName(makeCredSN(credSNBuf), sDateTime)

        var credTypeTag = UNKNOWN_TAG
        if(credType == RFID_TYPE_TAG)   credTypeTag = RFID_TAG
        else if(credType == MOBILEID_TYPE_TAG)   credTypeTag = MOBILEID_TAG
        else if(credType == PINCODE_TYPE_TAG)   credTypeTag = PINCODE_TAG
        else if(credType == ADMIN_TYPE_TAG)   credTypeTag = ADMIN_TAG

        val sAction = lockActionMaps()[action]

        dataSource.addLockUsageInfo(Report(lockName, dataSource.getLockFunc(lockName), userName, sAction!!, sDateTime, credTypeTag))
        dataSource.filterLockUsageInfos(mLockUsageInfoFilterType)
        view.updateReports()
    }


    override fun exportReport(context: Context, useInternalMem: Boolean) {

        if(!useInternalMem && !checkSDCard()) {
            view.showErrorMessage(NO_SD_CARD)
            return
        } else {
            val disposable = dataSource.exportReport(context, mReportType, Environment.getExternalStorageDirectory().getPath())
                    .subscribeOn(scheduler.io())
                    .observeOn(scheduler.ui())
                    .subscribe(
                            {
                                Log.i(LOG_TAG, "Export report success .")
                                view.showErrorMessage("Export report success .")
                            },
                            {
                                Log.i(LOG_TAG, "Export report failed: ${it.localizedMessage}")
                                view.showErrorMessage("Export report failed: ${it.localizedMessage}")
                            }
                    )

            compositeDisposable.add(disposable)
        }
    }

    override fun sortByLockName() {
        if(mSortByLockName == AUDIT_SORT_NONE || mSortByLockName == AUDIT_SORT_DSC)
            mSortByLockName = AUDIT_SORT_ASC
        else
            mSortByLockName = AUDIT_SORT_DSC

        dataSource.sortByLockName(mSortByLockName)
        view.updateReports()
    }

    override fun sortByLockFunc() {
        if(mSortByLockFunc == AUDIT_SORT_NONE || mSortByLockFunc == AUDIT_SORT_DSC)
            mSortByLockFunc = AUDIT_SORT_ASC
        else
            mSortByLockFunc = AUDIT_SORT_DSC

        dataSource.sortByLockFunc(mSortByLockFunc)
        view.updateReports()
    }

    override fun sortByUserName() {
        if(mSortByUserName == AUDIT_SORT_NONE || mSortByUserName == AUDIT_SORT_DSC)
            mSortByUserName = AUDIT_SORT_ASC
        else
            mSortByUserName = AUDIT_SORT_DSC

        dataSource.sortByUserName(mSortByUserName)
        view.updateReports()
    }

    override fun sortByLockAction() {
        if(mSortByLockAction == AUDIT_SORT_NONE || mSortByLockAction == AUDIT_SORT_DSC)
            mSortByLockAction = AUDIT_SORT_ASC
        else
            mSortByLockAction = AUDIT_SORT_DSC

        dataSource.sortByLockAction(mSortByLockAction)
        view.updateReports()
    }

    override fun sortByAuditDT() {
        if(mSortByAuditDT == AUDIT_SORT_NONE || mSortByAuditDT == AUDIT_SORT_DSC)
            mSortByAuditDT = AUDIT_SORT_ASC
        else
            mSortByAuditDT = AUDIT_SORT_DSC

        dataSource.sortByAuditDT(mSortByAuditDT)
        view.updateReports()
    }

    override fun sortByCredType() {
        if(mSortByCredType == AUDIT_SORT_NONE || mSortByCredType == AUDIT_SORT_DSC)
            mSortByCredType = AUDIT_SORT_ASC
        else
            mSortByCredType = AUDIT_SORT_DSC

        dataSource.sortByCredType(mSortByCredType)
        view.updateReports()
    }

    private fun checkSDCard(): Boolean {
        return (getRemovableDeviceDirs().size > 0)
    }

    override fun updateLockStatus() {
        view.sendWsPacket(dataSource.getSendPacket(CMD_UPDATE_LOCK_STATUS_BT))
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    companion object {
        private val LOG_TAG = MainPresenter::class.java.simpleName
    }

}
