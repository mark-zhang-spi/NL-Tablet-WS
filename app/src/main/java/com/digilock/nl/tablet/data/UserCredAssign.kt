package com.digilock.nl.tablet.data

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.digilock.nl.tablet.util.DB_NAME_USERCREDASSIGNS
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = DB_NAME_USERCREDASSIGNS)
data class UserCredAssign(
        @SerializedName("_id")
        @ColumnInfo(name = "assign_id")
        @PrimaryKey(autoGenerate = true)
        var assignId: Int = 0,

        @SerializedName("_user_id")
        @ColumnInfo(name = "user_id")
        var userId: Int = 0,

        @SerializedName("_cred_sn")
        @ColumnInfo(name = "cred_sn")
        var credSN: String = "",

        @SerializedName("_state")
        @ColumnInfo(name = "is_active")
        var isActive: Boolean = false,

        @SerializedName("_start")
        @ColumnInfo(name = "state_date")
        var startDate: String = "",

        @SerializedName("_end")
        @ColumnInfo(name = "end_date")
        var endDate: String = ""
): Parcelable
