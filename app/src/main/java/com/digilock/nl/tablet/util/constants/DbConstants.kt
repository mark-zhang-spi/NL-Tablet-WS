package com.digilock.nl.tablet.util

const val DB_NAME_USERS: String = "users"
const val DB_USERS: String = "$DB_NAME_USERS.db"
const val DB_QUERY_ALL_USERS: String = "SELECT * FROM $DB_NAME_USERS"
const val DB_QUERY_ALL_ACTIVE_USERS: String = "SELECT * FROM $DB_NAME_USERS WHERE user_state = 1"

const val DB_NAME_LOCKS: String = "locks"
const val DB_LOCKS: String = "$DB_NAME_LOCKS.db"
const val DB_QUERY_ALL_LOCKS: String = "SELECT * FROM $DB_NAME_LOCKS"

const val DB_NAME_CREDS: String = "credentials"
const val DB_CREDS: String = "$DB_NAME_CREDS.db"
const val DB_QUERY_ALL_CREDS: String = "SELECT * FROM $DB_NAME_CREDS"

const val DB_NAME_USERCREDASSIGNS: String = "user_credential_assigns"
const val DB_USERCREDASSIGNS: String = "$DB_NAME_USERCREDASSIGNS.db"
const val DB_QUERY_ALL_USERCREDASSIGNS: String = "SELECT * FROM $DB_NAME_USERCREDASSIGNS"
const val DB_QUERY_USERCREDASSIGNS_BY_USERID: String = "SELECT * FROM $DB_NAME_USERCREDASSIGNS WHERE user_id = :userId"
const val DB_REMOVE_ALL_USERCREDASSIGNS: String = "DELETE FROM $DB_NAME_USERCREDASSIGNS"


const val DB_NAME_USERLOCKASSIGNS: String = "user_lock_assigns"
const val DB_USERLOCKASSIGNS: String = "$DB_NAME_USERLOCKASSIGNS.db"
const val DB_QUERY_ALL_USERLOCKASSIGNS: String = "SELECT * FROM $DB_NAME_USERLOCKASSIGNS"
const val DB_QUERY_USERLOCKASSIGNS_BY_USERID: String = "SELECT * FROM $DB_NAME_USERLOCKASSIGNS WHERE user_id = :userId"

const val DB_NAME_AUDITTRAILS: String = "audit_trails"
const val DB_AUDITTRAILS: String = "$DB_NAME_AUDITTRAILS.db"
const val DB_QUERY_ALL_AUDITTRAILS: String = "SELECT * FROM $DB_NAME_AUDITTRAILS"
const val DB_QUERY_AUDITTRAILS_BY_LOCKSN: String = "SELECT * FROM $DB_NAME_AUDITTRAILS WHERE lock_sn = :lockSN"
const val DB_REMOVE_ALL_AUDIT_TRAILS: String = "DELETE FROM $DB_NAME_AUDITTRAILS"
