package com.digilock.nl.tablet.main

import android.content.Context
import android.text.Editable
import com.digilock.nl.tablet.BasePresenter
import com.digilock.nl.tablet.BaseView
import com.digilock.nl.tablet.comm.CommPacket


interface MainContract {
    interface View: BaseView<Presenter> {

        fun showErrorMessage(msg: String)
        fun showCommMessage(msg: String)

        fun showUsersScreen()
        fun showNoUserScreen()
        fun showUserDetail(userID: Int)
        fun showFilterUsersScreen()
        fun showFilterUsersScreen(filterCode: Int)
        fun confirmChangeUserState(userID: Int, disable: Boolean)
        fun updateChangeUserState()

        fun showLocksScreen()
        fun showNoLockScreen()
        fun showLockDetail(lockID: Int)
        fun showFilterLocksScreen()
        fun toggleLockState(lockSN: String)
        fun updateLockNames(locks: ArrayList<String>)

        fun sendWsPacket(packet: CommPacket)
        fun sendWsJSonPacket(sPacket: String)

        fun showLockNamesFilter(hide: Boolean)
        fun showAuditTrail(hide: Boolean)
        fun showSharedStatus(hide: Boolean, locked: Boolean, count: Int)
        fun showAssignedStatus(hide: Boolean, hasUser: Boolean, count: Int)

        fun updateSharedStatusReportCount(locked: Boolean, count: Int)

        fun updateReports()
        fun updateReportToEnd()

        fun pauseUpdateTimer()
        fun resumeUpdateTimer()

        fun startScanController()
        fun stopScanController()

        fun connectController(address: String)
        fun showPairedControllerName(name: String)


        fun updateControllerDevicesView(devices: List<DeviceData>)

        fun reStartApp()
    }

    interface Presenter: BasePresenter {
        fun getEchoPacket(): CommPacket
        fun updateLockStatus()

        /*
            ADMIN related
         */
        fun connectWSController(context: Context)

        /*
            LOCKS related
         */
        fun filterLocks(position: Int)

        fun lockCount(): Int
        fun onBindLockRowAtPosition(position: Int, rowView: LockRowView)

        fun showLockDetail(position: Int)
        fun toggleLockState(position: Int)

        fun showLocksScreenClicked(position: Int)

        /*
            USERS related
         */
        fun filterUsers(position: Int)

        fun userCount(): Int
        fun onBindUserRowAtPosition(position: Int, rowView: UserRowView)

        fun confirmChangeUserState(position: Int)
        fun changeUserState(userID: Int)
        fun showUserDetail(position: Int)

        fun showUsersScreenClicked(position: Int)

        fun updateUserCredPermit(userID: Int)

        /*
            Bluetooth related
         */
        fun getToggleLockStatePacket(lockSN: String): CommPacket
        fun getLockStatusPacket(lockSN: String): CommPacket

        fun processMobilePacket(data: ByteArray)

        fun connectControllerTimeout()

        /*
            REPORTS related
        */
        fun selectLockFuncType(position: Int)
        fun selectLockName(position: Int)


        fun doAudit(lockFuncTypePos: Int, lockNamePos: Int)
        fun doLockUsageInfo(lockStatusPos: Byte)
        fun sharedStatus(lockStatusPos: Int)
        fun assignedStatus(userAssigned: Int)

        fun exportReport(context: Context, useInternalMem: Boolean)

        fun sortByLockName()
        fun sortByLockFunc()
        fun sortByUserName()
        fun sortByLockAction()
        fun sortByAuditDT()
        fun sortByCredType()

        fun reportRowsCount(): Int
        fun onBindReportRowAtPosition(context: Context, position: Int, rowView: ReportRowView)

        /*
            SETTINGS related
         */
        fun showSettings()
        fun scanController(context: Context)
        fun savePairedController(name: String, address: String)

        fun addController()

        fun syncController()
        fun emptyAudit()
        fun getAllLockStatus()

        fun downloadDB(context: Context)
        fun restoreDB(context: Context)

        fun unsubscribe()
    }
}