package com.digilock.nl.tablet.main

import com.digilock.nl.tablet.data.Lock

interface UserRowView {
    fun setUserName(name: String)
    fun setUserDepartment(dept: String)
    fun setUserState(state: Boolean)
    fun setUserDTInfo(dtInfo: String)
    fun setAssignedLocks(lockNames: List<String>)
}