package com.digilock.nl.tablet.main

interface LockRowView {
    fun setLockName(name: String)
    fun setLockState(state: Byte)
}