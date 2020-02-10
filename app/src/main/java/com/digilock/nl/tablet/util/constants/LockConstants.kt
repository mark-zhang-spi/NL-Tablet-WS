package com.digilock.nl.tablet.util.constants

const val LOCK_ID_PARAMETER = "Old Lock ID"

const val LOCK_NAME_IS_EMPTY = "Lock name can not be empty."
const val LOCK_SN_IS_EMPTY = "Lock serial number can not be empty."
const val LOCK_LOCATION_IS_EMPTY = "Lock location can not be empty."
const val LOCK_DESC_IS_EMPTY = "Lock description can not be empty."
const val LOCK_CRED_IS_EMPTY = "Lock credential permit can not be empty."

const val LOCK_IS_LOCKED = "Locked"
const val LOCK_IS_UNKNOWN = "Unknown"
const val LOCK_IS_UNLOCKED = "Unlocked"

const val LOCK_NAME_EXISTS = "Lock name exists already, please choose another one."
const val LOCK_SN_EXISTS = "Lock serial number exists already, please choose another one."


val lockTypes = arrayOf("Aspire RFID/MobileID Standard Body", "Aspire KeyPad")
// Bit0: RFID, Bit1: MobileID, Bit2: User Pin
val credentialPermits  = byteArrayOf(0x07, 0x04)

val LOCK_FUNC_SHARED_USE = "Shared Use"
val LOCK_FUNC_ASSIGNED_USE = "Assigned Use"
val lockFuncs = arrayOf(LOCK_FUNC_SHARED_USE, LOCK_FUNC_ASSIGNED_USE)

val ARRAY_LOCK_FILTER: ArrayList<String> = arrayListOf(
        "All Locks", "Shared Use Locks", "Assigned Use Locks", "Locked Locks", "Unlocked Locks", "Unknown State Locks", "RFID Locks", "Keypad Locks", "Mobile ID Locks"
)

val LOCK_FILTER_ALL: Byte = 0
val LOCK_FILTER_SHARED: Byte = 1
val LOCK_FILTER_ASSIGNED: Byte = 2
val LOCK_FILTER_LOCKED: Byte = 3
val LOCK_FILTER_UNLOCKED: Byte = 4
val LOCK_FILTER_STATE_UNKNOWN: Byte = 5
val LOCK_FILTER_RFID: Byte = 6
val LOCK_FILTER_KEYPAD: Byte = 7
val LOCK_FILTER_MOBILEID: Byte = 8





