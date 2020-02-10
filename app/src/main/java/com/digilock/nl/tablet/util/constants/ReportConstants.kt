package com.digilock.nl.tablet.util.constants

const val REPORT_NONE: Byte = 0
const val REPORT_AUDIT_TRAIL: Byte = 1
const val REPORT_SHARED_USE_LOCKED: Byte = 2
const val REPORT_SHARED_USE_UNLOCKED: Byte = 3
const val REPORT_ASSIGNED_USE_HAS_USER: Byte = 4
const val REPORT_ASSIGNED_USE_NO_USER: Byte = 5

const val NO_SD_CARD = "Not fund SD Card !"

const val AUDIT_TRAIL_FOLDER = "/Digilock/NL_Tablet/Export/Audit Trail"
const val SHARED_USE_FOLDER = "/Digilock/NL_Tablet/Export/Shared Use"
const val ASSIGNED_USE_FOLDER = "/Digilock/NL_Tablet/Export/Assigned Use"
const val AUDIT_TRAIL_FILE = "/AuditReail.txt"
const val SHARED_USE_LOCKED_FILE = "/SharedUse_Locked.txt"
const val SHARED_USE_UNLOCKED_FILE = "/SharedUse_Unlocked.txt"
const val ASSIGNED_USE_NO_USER_FILE = "/AssignedUse_No_User.txt"
const val ASSIGNED_USE_HAS_USER_FILE = "/AssignedUse_Has_User.txt"

const val TITLE_AUDIT_TRAIL = "Audit Trail"
const val TITLE_SHARED_USE_LOCKED = "Shared Use - Unavailable"
const val TITLE_SHARED_USE_UNLOCKED = "Shared Use - Available"
const val TITLE_ASSIGNED_USE_NO_USER = "Assigned Use - No User"
const val TITLE_ASSIGNED_USE_HAS_USER = "Assigned Use - Has User"

const val DEF_LOCK_NAME_SPACE = 16
const val DEF_LOCK_FUNC_SPACE = 16
const val DEF_ASSIGNED_USER_SPACE = 32
const val DEF_USER_NAME_SPACE = 16
const val DEF_ACTION_SPACE = 12
const val DEF_DATE_SPACE = 16
const val DEF_CRED_TYPE_SPACE = 16

const val AUDIT_SORT_NONE: Byte = 0
const val AUDIT_SORT_ASC: Byte = 1
const val AUDIT_SORT_DSC: Byte = 2

val AUDIT_LOCK_FUNC_FILTER_ALL: Byte = 0
val AUDIT_LOCK_FUNC_FILTER_LOCKID: Byte = 1
val AUDIT_LOCK_FUNC_FILTER_SHARED: Byte = 2
val AUDIT_LOCK_FUNC_FILTER_ASSIGNED: Byte = 3
val AUDIT_LOCK_FUNC_FILTER_LOCKED: Byte = 4
val AUDIT_LOCK_FUNC_FILTER_UNLOCKED: Byte = 5
val AUDIT_LOCK_FUNC_FILTER_RFID: Byte = 6
val AUDIT_LOCK_FUNC_FILTER_KEYPAD: Byte = 7
val AUDIT_LOCK_FUNC_FILTER_MOBILEID: Byte = 8
val AUDIT_LOCK_FUNC_FILTER_ADMIN: Byte = 9

val LOCK_USAGE_INFO_FILTER_LOCKED: Byte = 0
val LOCK_USAGE_INFO_FILTER_UNLOCKED: Byte = 1

val ARRAY_AUDIT_FILTER: ArrayList<String> = arrayListOf(
        "All Locks", "By Lock ID", "Shared Use", "Assigned Use", "Locked", "Unlocked", "By RFID Card", "By PinCode", "By MobileID", "By Admin"
)

const val ACTION_IS_LOCKED = "LOCKED"
const val ACTION_IS_UNKNOWN = "UNKNOWN"
const val ACTION_IS_UNLOCKED = "UNLOCKED"

const val LOCK_USAGE_INFO_IS_EMPTY = "Lock usage information is empty, please fetch from controller !"