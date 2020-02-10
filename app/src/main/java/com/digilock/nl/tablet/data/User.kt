package com.digilock.nl.tablet.data

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.digilock.nl.tablet.util.DB_NAME_USERS
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * - User
 */
@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = DB_NAME_USERS)
data class User(
        @SerializedName("_id")
        @ColumnInfo(name = "user_id")
        @PrimaryKey(autoGenerate = true)
        var userId: Int = 0,

        @SerializedName("_state")
        @ColumnInfo(name = "user_state")
        var userState: Boolean = true,                 // true: active, false: terminated

        @SerializedName("_cred_permit")
        @ColumnInfo(name = "cred_permit")
        var credpermit: Byte = 0,

        @SerializedName("_start")
        @ColumnInfo(name = "state_date")
        var startDate: String = "",

        @SerializedName("_end")
        @ColumnInfo(name = "end_date")
        var endDate: String = "",

        @SerializedName("_department")
        @ColumnInfo(name = "dept")
        var dept: String = "",

        @SerializedName("_notes")
        @ColumnInfo(name = "notes")
        var notes: String = "",

        @SerializedName("email")
        @ColumnInfo(name = "email")
        var email: String = "",

        @SerializedName("phone_num")
        @ColumnInfo(name = "phone_num")
        var phoneNum: String = "",

        @SerializedName("first_name")
        @ColumnInfo(name = "first_name")
        var fstName: String = "",

        @SerializedName("last_name")
        @ColumnInfo(name = "last_name")
        var lstName: String = ""
) : Parcelable