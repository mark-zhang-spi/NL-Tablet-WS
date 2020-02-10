package com.digilock.nl.tablet.util

import java.text.SimpleDateFormat
import java.util.HashMap



const val BASE_URL_SERVER_DEV: String = "https://pl-server-dev.herokuapp.com/tablet/"
const val SYSTEM_CONFIG: String = "system config"

val sdtf = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

const val WAIT_CREDENTIAL_PERIOD: Long = 20000      // 20 seconds
const val WAIT_MOBILE_ID_CREDENTIAL_PERIOD: Long = 60000      // 60 seconds

const val CREDENTIAL_RFID: Byte = 0x01
const val CREDENTIAL_MOBILEID: Byte = 0x02
const val CREDENTIAL_PINCODE: Byte = 0x04

const val PINCODE_TYPE_TAG: Byte = 0x02
const val RFID_TYPE_TAG: Byte = 0x03
const val MOBILEID_TYPE_TAG: Byte = 0x04
const val ADMIN_TYPE_TAG: Byte = 0x05

const val UNKNOWN_TAG = "Unknown"
const val PINCODE_TAG = "PinCode"
const val RFID_TAG = "RFID"
const val MOBILEID_TAG = "MobileID"
const val ADMIN_TAG = "Admin"

const val ASSIGNED_TAG = "Assigned"
const val SHARED_TAG = "Shared"


const val WAIT_RFID_TIMEOUT_MSG = "Failed to read RFID card, would you like try again ?"
const val WAIT_MOBILEID_TIMEOUT_MSG = "Failed to read Mobile ID, would you like try again ?"
const val WAIT_PINCODE_TIMEOUT_MSG = "Failed to get PIN Code, would you like try again ?"

const val QUERY_TRANSFER_EXIST_CARD_MSG = "Presented card has been assigned to another user, would you like to transfer it or try another one?"
const val QUERY_TRANSFER_EXIST_MOBILEID_MSG = "Presented Mobile ID has been assigned to another user, would you like to transfer it or try another one?"

fun timeoutMsgs(): HashMap<Byte, String> {
    val timeoutMsgMap = HashMap<Byte, String>()
    timeoutMsgMap[CREDENTIAL_RFID] = WAIT_RFID_TIMEOUT_MSG
    timeoutMsgMap[CREDENTIAL_MOBILEID] = WAIT_MOBILEID_TIMEOUT_MSG
    timeoutMsgMap[CREDENTIAL_PINCODE] = WAIT_PINCODE_TIMEOUT_MSG
    return timeoutMsgMap
}

fun credExistsMsgs(): HashMap<Byte, String> {
    val credExistsMsgMap = HashMap<Byte, String>()
    credExistsMsgMap[CREDENTIAL_RFID] = QUERY_TRANSFER_EXIST_CARD_MSG
    credExistsMsgMap[CREDENTIAL_MOBILEID] = QUERY_TRANSFER_EXIST_MOBILEID_MSG
    credExistsMsgMap[CREDENTIAL_PINCODE] = ""
    return credExistsMsgMap
}

const val PIN_CODE_COUNT_MAX = 7
const val PIN_CODE_COUNT_MIN = 4

const val MOBILE_ID_COUNT = 8

const val MSG_PIN_CODE_IS_SHORT = "Pin Code is less than $PIN_CODE_COUNT_MIN digits, would you like try again ?"

const val MSG_MOBILE_ID_IS_ILLEGAL = "Mobile ID was illegal, would you like try again ?"

const val CODE_DELETE: Int = -1
const val CODE_DELETE_ALL: Int = -88
const val CODE_FIND: Int = 100
const val CODE_CANCEL: Int = 30
const val CODE_OK: Int = 31
const val CODE_BLANK: Int = -100

const val DOWNLOAD_DIR_NAME = "/Digilock/NL_Tablet/Download"

const val NOT_CONNECT_CONTROLLER = "Not connect with controller."
const val WRONG_CONTROLLER_DEVICE_NAME = "Wrong controller device name."

const val RED_COLOR = "#AD1255"
const val GREEN_COLOR = "#12AD55"





val ARRAY_SHARED_STATUS_FILTER: ArrayList<String> = arrayListOf(
        "Locked Locks", "Unlocked Locks"
)

val SHARED_STATUS_FILTER_LOCKED = 0
val SHARED_STATUS_FILTER_UNLOCKED = 1

val ARRAY_ASSIGNED_STATUS_FILTER: ArrayList<String> = arrayListOf(
        "Has User", "No User"
)

val ASSIGNED_STATUS_FILTER_WITH_USER = 0
val ASSIGNED_STATUS_FILTER_NO_USER = 1

const val SHARED_CREDENTIAL = "FFFFFFFFFFFFFF14"
const val ADMIN_CREDENTIAL = "FFFFFFFFFFFF886F"

const val DEFAULT_PASSCODE = "digilock123"
const val EMPTY_PASSCODE = "Passcode can not be empty !"
const val NOT_MATCH_PASSCODE = "Passcode does not match !"

const val CONFIRM_CHANGE_USER_STATE_CODE = 200
const val CONFIRM_CLEAR_PAIRED_DEVICE_CODE = 201
const val CONFIRM_CLEAR_AUDIT_TRAIL_CODE = 202
const val CONFIRM_DELETE_LOCK_CODE = 203
const val CONFIRM_RESTORE_DB_CODE = 204
const val CONFIRM_SYNC_CONTROLLER_CODE = 205
const val CONFIRM_DISABLE_USER_CODE = 206
