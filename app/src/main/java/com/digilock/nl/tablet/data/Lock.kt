package com.digilock.nl.tablet.data

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.digilock.nl.tablet.util.DB_NAME_LOCKS
import com.digilock.nl.tablet.util.STATE_UNKNOWN
import com.digilock.nl.tablet.util.constants.LOCK_FUNC_SHARED_USE
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = DB_NAME_LOCKS)
data class Lock(
        @SerializedName("_id")
        @ColumnInfo(name = "lock_id")
        @PrimaryKey(autoGenerate = true)
        var lockId: Int = 0,

        @SerializedName("_state")
        @ColumnInfo(name = "lock_state")
        var lockState: Byte = STATE_UNKNOWN,                 // true: locked, false: Unlocked

        @SerializedName("_name")
        @ColumnInfo(name = "lock_name")
        var lockName: String = "",

        @SerializedName("_serial")
        @ColumnInfo(name = "lock_serial")
        var lockSN: String = "",

        @SerializedName("_location")
        @ColumnInfo(name = "lock_location")
        var lockLocation: String = "",

        @SerializedName("_notes")
        @ColumnInfo(name = "lock_notes")
        var lockNotes: String = "",

        @SerializedName("_type")
        @ColumnInfo(name = "lock_type")
        var lockType: String = "",

        @SerializedName("_func")
        @ColumnInfo(name = "lock_func")
        var lockFunc: String = LOCK_FUNC_SHARED_USE,

        @SerializedName("_cred_permit")
        @ColumnInfo(name = "credential_permit")
        var credPermit: Byte = 0
) : Parcelable