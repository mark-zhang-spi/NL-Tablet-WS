package com.digilock.nl.tablet.util

import java.util.HashMap

const val CONNECT_CONTROLLER_TIMEOUT = "Connect to network lock controller timeout."


/**
 * Defines several constants used between [BluetoothChatService] and the UI.
 */
const val REQUEST_ENABLE_BT = 3


const val WS_PACKET_SIZE = 64
const val PARAMETER_BYTES_SIZE = 59

const val PKT_HEADER: Byte = 0x7E
const val PKT_TAIL: Byte = -0x56

const val CMD_FIND_BT: Byte = 0x3E
const val CMD_ECHO_BT: Byte = 0x3F

const val CMD_GET_LOCK_STATUS_BT: Byte = 0x48
const val CMD_GET_ALL_LOCKS_STATUS_BT: Byte = 0x49
const val CMD_LOCK_STATUS_BT: Byte = 0x4A
const val CMD_LOCK_STATUS_DONE_BT: Byte = 0x4B

const val CMD_TOGGLE_LOCK_STATUS_BT: Byte = 0x4C

const val CMD_LOCK_DO_AUDIT_BT: Byte = 0x50
const val CMD_ALL_DO_AUDIT_BT: Byte = 0x51
const val CMD_CLEAR_AUDIT_BT: Byte = 0x52
const val CMD_AUDIT_TRAILS_BT: Byte = 0x53
const val CMD_AUDIT_TRAIL_DONE_BT: Byte = 0x54
const val CMD_DO_AUDIT_SHARED_BT: Byte = 0x55
const val CMD_DO_AUDIT_ASSIGNED_BT: Byte = 0x56
const val CMD_DO_AUDIT_LOCKED_BT: Byte = 0x57
const val CMD_DO_AUDIT_UNLOCKED_BT: Byte = 0x58
const val CMD_DO_AUDIT_RFID_BT: Byte = 0x59
const val CMD_DO_AUDIT_PINCODE_BT: Byte = 0x5A
const val CMD_DO_AUDIT_MOBILEID_BT: Byte = 0x5B
const val CMD_AUDIT_TRAIL_EMPTY_BT: Byte = 0x5C

const val CMD_ALL_LOCK_USAGE_INFO_BT: Byte = 0x78
const val CMD_LOCK_USAGE_INFO_EMPTY_BT: Byte = 0x79
const val CMD_LOCK_USAGE_INFO_DONE_BT: Byte = 0x7A
const val CMD_LOCK_USAGE_INFO_BT: Byte = 0x7B


const val CMD_DEASSIGN_ALL_LOCK_CREDENTIAL_BT: Byte = 0x60
const val CMD_DEASSIGN_LOCK_CREDENTIAL_BY_LOCK_ID_BT: Byte = 0x61

const val CMD_INSERT_LOCK_CREDENTIAL_ASSIGN_BT: Byte = 0x62
const val CMD_ADD_LOCK_BT: Byte = 0x63

const val CMD_AUTO_LOCK_STATUS_BT: Byte = 0x70
const val CMD_RESP_AUTO_LOCK_STATUS_BT: Byte = 0x71
const val CMD_UPDATE_LOCK_STATUS_BT: Byte = 0x72


const val PT_LOCK_SN: Byte = 1
const val PT_LENGTH_LOCK_SN: Byte = 4

const val PT_CREDENTIAL_SN: Byte = 2

const val PT_LOCK_ACTION: Byte = 3      // Audit Trail

const val PT_LOCK_DATETIME: Byte = 4    // Audit Trail

const val PT_CREDENTIAL_TYPE: Byte = 5  // Audit Trail

const val PT_LOCK_STATUS: Byte = 6      // Audit Trail

const val PT_LOCK_FUNC: Byte = 7        // SYNC Server

const val BT_RESULT_PASS: Byte = 0x00
const val BT_RESULT_FAILED: Byte = 0x01
const val BT_RESULT_SERVER_STATE_ERROR: Byte = 0x02



const val ACTION_UNKNOWN: Byte = 0
const val ACTION_LOCK: Byte = 1
const val ACTION_UNLOCK: Byte = 2
const val ACTION_ACCESS: Byte = 3

const val STATE_UNKNOWN: Byte = 0
const val STATE_LOCKED: Byte = 1
const val STATE_UNLOCKED: Byte = 2

fun lockActionMaps(): HashMap<Byte, String> {
    val lockActionMap = HashMap<Byte, String>()
    lockActionMap[ACTION_UNKNOWN] = "UNKNOWN"
    lockActionMap[ACTION_LOCK] = "LOCKED"
    lockActionMap[ACTION_UNLOCK] = "UNLOCKED"
    lockActionMap[ACTION_ACCESS] = "ACCESS"
    return lockActionMap
}

fun btCmdMaps(): HashMap<Byte, String> {
    val btCmdMap = HashMap<Byte, String>()
    btCmdMap[CMD_ECHO_BT] = "BT: Echo"
    btCmdMap[CMD_FIND_BT] = "BT: Find"
    btCmdMap[CMD_GET_ALL_LOCKS_STATUS_BT] = "BT: Get all locks status"
    btCmdMap[CMD_GET_LOCK_STATUS_BT] = "BT: Get lock status"
    btCmdMap[CMD_LOCK_STATUS_BT] = "BT: Lock status"
    btCmdMap[CMD_LOCK_STATUS_DONE_BT] = "BT: Receive lock status DONE !"
    btCmdMap[CMD_TOGGLE_LOCK_STATUS_BT] = "BT: Toggle Lock"
    btCmdMap[CMD_CLEAR_AUDIT_BT] = "BT: Delete all audit trails on server"
    btCmdMap[CMD_ALL_DO_AUDIT_BT] = "BT: Do Audit on all locks"
    btCmdMap[CMD_LOCK_DO_AUDIT_BT] = "BT: Do Audit on selected Lock"
    btCmdMap[CMD_AUDIT_TRAILS_BT] = "BT: Receive audit trails"
    btCmdMap[CMD_AUDIT_TRAIL_DONE_BT] = "BT: Receive audit trail DONE !"
    btCmdMap[CMD_DEASSIGN_ALL_LOCK_CREDENTIAL_BT] = "BT: Deassign all lock credential"
    btCmdMap[CMD_DEASSIGN_LOCK_CREDENTIAL_BY_LOCK_ID_BT] = "BT: Deassign lock credential by lock ID"
    btCmdMap[CMD_INSERT_LOCK_CREDENTIAL_ASSIGN_BT] = "BT: Assign lock credential"
    btCmdMap[CMD_AUTO_LOCK_STATUS_BT] = "BT: Auto send lock status"
    btCmdMap[CMD_RESP_AUTO_LOCK_STATUS_BT] = "BT: Response for Auto send lock status"
    btCmdMap[CMD_UPDATE_LOCK_STATUS_BT] = "BT: Update lock status"
    btCmdMap[CMD_DO_AUDIT_SHARED_BT] = "BT: Do Audit on shared use locks"
    btCmdMap[CMD_DO_AUDIT_ASSIGNED_BT] = "BT: Do Audit on assigned use locks"
    btCmdMap[CMD_DO_AUDIT_LOCKED_BT] = "BT: Do Audit on locked locks"
    btCmdMap[CMD_DO_AUDIT_UNLOCKED_BT] = "BT: Do Audit on unlocked locks"
    btCmdMap[CMD_DO_AUDIT_RFID_BT] = "BT: Do Audit on RFID credentials"
    btCmdMap[CMD_DO_AUDIT_PINCODE_BT] = "BT: Do Audit on PinCode credentials"
    btCmdMap[CMD_DO_AUDIT_MOBILEID_BT] = "BT: Do Audit on MobileID credentials"
    btCmdMap[CMD_AUDIT_TRAIL_EMPTY_BT] = "BT: No required audit trail ion server"
    btCmdMap[CMD_ALL_LOCK_USAGE_INFO_BT] = "BT: Get all lock usage information"
    btCmdMap[CMD_LOCK_USAGE_INFO_EMPTY_BT] = "BT: No lock usage information"
    btCmdMap[CMD_LOCK_USAGE_INFO_DONE_BT] = "BT: Read lock usage information was done !"
    btCmdMap[CMD_LOCK_USAGE_INFO_BT] = "BT: Lock Usage Information."
    btCmdMap[CMD_ADD_LOCK_BT] = "BT: Add a lock."


    return btCmdMap
}

const val DIGI_NL_CONTROLLER_NAME = "digi_nl_controller1"
const val CONTROLLER_NOT_CONNECTED = "Controller not connected"
const val BT_NOT_PAIRED = "not paired"
const val BT_NOT_CONNECTED = "not connected"
const val BT_CONNECTED = "connected"
const val BT_CONNECTING = "connecting ..."

const val LOCATION_ACCESS_IS_GRANTED_AUTO = "This version Android LOCATION ACCESS is granted automatically !"
const val LOCATION_ACCESS_IS_GRANTED = "LOCATION ACCESS is granted already !"

const val ALL_LOCKS = "All Locks"

const val AUDIT_TRAIL_DONE = "DO audit trail DONE !"
const val AUDIT_TRAIL_EMPTY = "No required audit trail on server !"
const val LOCK_USAGE_INFO_EMPTY = "Lock usage info is empty !"
const val LOCK_USAGE_INFO_DONE = "Read lock usage info Done !"
const val LOCK_STATUS_DONE = "Read lock status DONE !"
const val DOGGLE_LOCK_STATUS_REJECTED = "Donggle lock status was rejected !"

const val UNKNOWN_USER: Byte = 0
const val SHARED_USER: Byte = 1
const val ASSIGNED_USER: Byte = 2

const val UNKNOWN_USER_NAME = "Unknown User"
const val SHARED_USER_NAME = "Shared User"
const val ADMIN_USER_NAME = "Admin Remote"

const val GET_GATT_SERVER_FAILED = "Get GATT server failed, please try again."

const val BLE_COMM_BUFFER_SIZE = 64

const val PAIRED_CONTROLLER_NAME = "WS controller name"
const val PAIRED_CONTROLLER_IP_ADDRESS = "WS controller IP address"

