package com.digilock.nl.tablet.util.constants

const val USER_ID_PARAMETER = "User ID"
const val USER_STATE_PARAMETER = "User state"
const val CONFRIM_CODE_PARAMETER = "Confirm Dialog Code"

const val USER_FNAME_IS_EMPTY = "First name can not be empty."
const val USER_LNAME_IS_EMPTY = "Last name can not be empty."
const val USER_DEPARTMENT_IS_EMPTY = "Department can not be empty."
const val USER_PHONE_IS_EMPTY = "Phone number can not be empty."
const val USER_EMAIL_IS_EMPTY = "Email can not be empty."
const val USER_NOTES_IS_EMPTY = "User description can not be empty."
const val USER_NAME_EXISTS = "User name exists already, please choose another one."
const val ENABLED_USER_NAME_EXISTS = "Enabled user name is same as another active user."

const val USER_IS_ACTIVE = "ACTIVE"
const val USER_IS_DISABLED = "DISABLED"

const val TRANSFER_CREDENTIAL = "TRANSFER"
const val TRYAGAIN_WAIT_CREDENTIAL = "TRY AGAIN"

val ARRAY_USER_FILTER: ArrayList<String> = arrayListOf(
        "All Users", "Active Users", "Disabled Users", "RFID Users", "Pincode Users", "MobileID Users"
)

val USER_FILTER_ALL = 0
val USER_FILTER_ACTIVE = 1
val USER_FILTER_DISABLED = 2
val USER_FILTER_RFID = 3
val USER_FILTER_PINCODE = 4
val USER_FILTER_MOBILEID = 5

val USER_IS_DISABLED_TAG = "User had been disabled."



