package com.digilock.nl.tablet.main

import android.content.Context

interface ReportRowView {
    fun setIndex(index: String)
    fun setLockName(lockName: String)
    fun setLockFunc(lockFunc: String)
    fun setUserName(userName: String)
    fun setAction(action: String)
    fun setDateTime(dt: String)
    fun setCredType(credType: String)
    fun setLockUsers(context: Context, lockUsers: Array<String?>)
    fun showReportDataItems(reportType: Int)
}