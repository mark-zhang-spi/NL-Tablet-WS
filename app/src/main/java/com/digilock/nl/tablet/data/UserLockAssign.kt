package com.digilock.nl.tablet.data

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.digilock.nl.tablet.util.DB_NAME_USERLOCKASSIGNS
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = DB_NAME_USERLOCKASSIGNS)
data class UserLockAssign(
        @SerializedName("_id")
        @ColumnInfo(name = "assign_id")
        @PrimaryKey(autoGenerate = true)
        var assignId: Int = 0,

        @SerializedName("_user_id")
        @ColumnInfo(name = "user_id")
        var userId: Int = 0,

        @SerializedName("_lock_id")
        @ColumnInfo(name = "lock_id")
        var lockId: Int = 0,

        @SerializedName("_time_stamp")
        @ColumnInfo(name = "time_stamp")
        var timeStamp: String = ""
): Parcelable
