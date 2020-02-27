package com.digilock.nl.tablet.websocket

import com.digilock.nl.tablet.util.CMD_GET_ALL_LOCKS_STATUS_BT

const val JSON_CMD_TYPE = "cmdType"
const val CMD_KEEP_CONNECTION = "Keep Connection Alive"
const val JSON_BODY = "Body"
const val CONTENT_KEEP_CONNECTION = "-Ping-Pong-"

const val CMD_AUDIT_TRAIL = "Audit Trail"
const val LOCK_STATUS = "lockStatus"

const val LOCK_UUID = "lockUUID"
const val LOCK_UID = "lockUID"
const val LOCK_FUNC = "lockFunc"

const val CREDENTIAL_UUID = "credentialUUID"
const val CREDENTIAL_TYPE = "credentialType"
const val CREDENTIAL_VALUE = "credentialValue"

const val CMD_LOCK_CRED_ASSIGNMENT = "Lock Credential Assignment"

const val CRED_RFID = "RFID"
const val CRED_MOBILEID = "MObileID"
const val CRED_PINCODE = "PinCode"
const val CRED_TEMP = "Temp"

const val CRED_RFID_VAL = 1
const val CRED_MOBILEID_VAL = 2
const val CRED_PINCODE_VAL = 4
const val CRED_TEMP_VAL = 8

const val PACKET_SUCCESS = "Success"
const val PACKET_FAILED = "Failed"

const val CMD_LOCK_CRED_DEASSIGNMENT = "Lock Credential Deassignment"

const val CMD_PACKET = "Packet command"

const val CMD_NL_DISCOVERY = "NL Device Discovery"
const  val CMD_NL_STOP_RESPONSE = "NL Stop Response"
const  val BODY_MAC_ADDRESS = "MAC Address"
const  val BODY_IP_ADDRESS = "IP address"

const val CMD_INITIALIZE_CONTROLLER = "Initialize Controller"
const val CMD_PROGRAM_CONTROLLER = "Program Controller"
const val SYS_JWT = "System Token"
const val SITE_UUID = "Site UUID"
const val ADMIN_UUID = "Admin UUID"
const val CTRL_UUID = "Controller UUID"
const val CTRL_NAME = "Controller Name"
const val CTRL_LOCATION = "Controller Location"
const val SYS_NOTES = "System Notes"



fun convertTypeToInt(type: String): Int {
    return when(type) {
        CRED_RFID -> CRED_RFID_VAL
        CRED_MOBILEID -> CRED_MOBILEID_VAL
        CRED_PINCODE -> CRED_PINCODE_VAL
        CRED_TEMP -> CRED_TEMP_VAL
        else -> -1
    }
}


fun intToIp(i: Int): String? {
    return (i and 0xFF).toString() + "." +
            (i shr 8 and 0xFF) + "." +
            (i shr 16 and 0xFF) + "." +
            (i shr 24 and 0xFF)
}




const val CMD_GET_LOCK_STATUS_WS: Byte = 0x48
